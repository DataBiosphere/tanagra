package bio.terra.tanagra.service.search;

import bio.terra.common.exception.BadRequestException;
import bio.terra.tanagra.service.search.Expression.AttributeExpression;
import bio.terra.tanagra.service.search.Filter.ArrayFunction;
import bio.terra.tanagra.service.search.Filter.BinaryFunction;
import bio.terra.tanagra.service.search.Filter.NullFilter;
import bio.terra.tanagra.service.search.Filter.RelationshipFilter;
import bio.terra.tanagra.service.search.Filter.TextSearchFilter;
import bio.terra.tanagra.service.search.Filter.UnaryFunction;
import bio.terra.tanagra.service.search.Selection.PrimaryKey;
import bio.terra.tanagra.service.search.utils.RandomNumberGenerator;
import bio.terra.tanagra.service.underlay.ArrayColumnFilter;
import bio.terra.tanagra.service.underlay.AttributeMapping;
import bio.terra.tanagra.service.underlay.AttributeMapping.LookupColumn;
import bio.terra.tanagra.service.underlay.AttributeMapping.SimpleColumn;
import bio.terra.tanagra.service.underlay.BinaryColumnFilter;
import bio.terra.tanagra.service.underlay.Column;
import bio.terra.tanagra.service.underlay.ForeignKey;
import bio.terra.tanagra.service.underlay.Hierarchy;
import bio.terra.tanagra.service.underlay.Hierarchy.ChildrenTable;
import bio.terra.tanagra.service.underlay.Hierarchy.DescendantsTable;
import bio.terra.tanagra.service.underlay.IntermediateTable;
import bio.terra.tanagra.service.underlay.Table;
import bio.terra.tanagra.service.underlay.TableFilter;
import bio.terra.tanagra.service.underlay.TextSearchInformation;
import bio.terra.tanagra.service.underlay.TextSearchInformation.TextTable;
import bio.terra.tanagra.service.underlay.Underlay;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Streams;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.apache.commons.text.StringSubstitutor;

/** Visitors for walking query constructs to create SQL. */
// TODO consider how this may need to be split for different SQL backends.
// TODO consider the jOOQ DSL.
public class SqlVisitor {
  private final SearchContext searchContext;
  private final UnderlayResolver underlayResolver;

  public SqlVisitor(SearchContext searchContext) {
    this.searchContext = searchContext;
    this.underlayResolver =
        new UnderlayResolver(searchContext.underlay(), searchContext.randomNumberGenerator());
  }

  public String createSql(Query query) {
    String selections =
        query.selections().stream()
            .map(selectField -> selectField.accept(new SelectionVisitor(searchContext)))
            .collect(Collectors.joining(", "));
    Optional<String> filterSql =
        query.filter().map(predicate -> predicate.accept(new FilterVisitor(searchContext)));

    String template = "SELECT ${selections} FROM ${table} WHERE ${filter}";
    Map<String, String> params =
        ImmutableMap.<String, String>builder()
            .put("selections", selections)
            .put("table", underlayResolver.resolveTable(query.primaryEntity()))
            .put("filter", filterSql.orElse("TRUE"))
            .build();
    String sqlWithoutOrderBy = StringSubstitutor.replace(template, params);

    if (query.orderBy() != null) {
      String orderByExpression = query.orderBy().accept(new SelectionVisitor(searchContext));

      String orderByTemplate = "${sqlWithoutOrderBy} ORDER BY ${orderBy} ${orderByDirection}";
      Map<String, String> orderByParams =
          ImmutableMap.<String, String>builder()
              .put("sqlWithoutOrderBy", sqlWithoutOrderBy)
              .put("orderBy", orderByExpression)
              .put("orderByDirection", query.orderByDirection().name())
              .build();
      return StringSubstitutor.replace(orderByTemplate, orderByParams);
    } else {
      return sqlWithoutOrderBy;
    }
  }

  /** A {@link Selection.Visitor} for creating SQL for selections. */
  static class SelectionVisitor implements Selection.Visitor<String> {
    private final SearchContext searchContext;

    SelectionVisitor(SearchContext searchContext) {
      this.searchContext = searchContext;
    }

    @Override
    public String selectExpression(Selection.SelectExpression selectExpression) {
      String expression =
          selectExpression.expression().accept(new ExpressionVisitor(searchContext));
      if (selectExpression.name().isEmpty()) {
        return expression;
      } else {
        return String.format("%s AS %s", expression, selectExpression.name());
      }
    }

    @Override
    public String count(Selection.Count count) {
      return String.format(
          "COUNT(%s) AS %s", count.entityVariable().variable().name(), count.name());
    }

    @Override
    public String primaryKey(PrimaryKey primaryKey) {
      Column primaryKeyColumn =
          searchContext.underlay().primaryKeys().get(primaryKey.entityVariable().entity());
      Preconditions.checkArgument(
          primaryKeyColumn != null,
          "Unable to find a primary key for entity '%s'",
          primaryKey.entityVariable().entity());
      return String.format(
          "%s.%s AS %s",
          primaryKey.entityVariable().variable().name(),
          primaryKeyColumn.name(),
          primaryKey.name());
    }
  }

  /** A {@link Filter.Visitor} for creating SQL for filters. */
  static class FilterVisitor implements Filter.Visitor<String> {
    private final SearchContext searchContext;
    private final UnderlayResolver underlayResolver;

    FilterVisitor(SearchContext searchContext) {
      this.searchContext = searchContext;
      this.underlayResolver =
          new UnderlayResolver(searchContext.underlay(), searchContext.randomNumberGenerator());
    }

    @Override
    public String visitArrayFunction(ArrayFunction arrayFunction) {
      String operatorDelimiter = String.format(" %s ", convert(arrayFunction.operator()));
      // e.g. (operand0) OR (operand1)
      return arrayFunction.operands().stream()
          // Recursively evaluate each operand.
          .map(f -> f.accept(this))
          // Join with the operator delimiter.
          .collect(Collectors.joining(operatorDelimiter));
    }

    private static String convert(ArrayFunction.Operator operator) {
      switch (operator) {
        case AND:
          return "AND";
        case OR:
          return "OR";
        default:
          throw new UnsupportedOperationException(
              String.format("Unable to convert ArrayFunction.Operator %s to SQL string", operator));
      }
    }

    @Override
    public String visitUnaryFunction(UnaryFunction unaryFunction) {
      String operatorDelimiter = String.format(" %s ", convert(unaryFunction.operator()));
      // e.g. NOT ((operator1) OR operator2))

      return operatorDelimiter
          .concat("(")
          .concat(
              unaryFunction.operands().stream()
                  // Recursively evaluate each operand.
                  .map(f -> f.accept(this))
                  // Join with the operator delimiter.
                  .collect(Collectors.joining("")))
          .concat(")");
    }

    private static String convert(UnaryFunction.Operator operator) {
      if (operator == UnaryFunction.Operator.NOT) return "NOT";
      throw new UnsupportedOperationException(
          String.format("Unable to convert UnaryFunction.Operator %s to SQL string", operator));
    }

    @Override
    public String visitBinaryComparision(BinaryFunction binaryFunction) {
      ExpressionVisitor expressionVisitor = new ExpressionVisitor(searchContext);

      String leftSql = binaryFunction.left().accept(expressionVisitor);
      String rightSql = binaryFunction.right().accept(expressionVisitor);
      boolean valueIsNull = "NULL".equals(rightSql);
      switch (binaryFunction.operator()) {
        case LESS_THAN:
          return String.format("%s < %s", leftSql, rightSql);
        case GREATER_THAN:
          return String.format("%s > %s", leftSql, rightSql);
        case EQUALS:
          return valueIsNull
              ? String.format("%s IS %s", leftSql, rightSql)
              : String.format("%s = %s", leftSql, rightSql);
        case NOT_EQUALS:
          return valueIsNull
              ? String.format("%s IS NOT %s", leftSql, rightSql)
              : String.format("%s != %s", leftSql, rightSql);
        case DESCENDANT_OF_INCLUSIVE:
          return resolveDescendantOfInclusive(binaryFunction);
        case CHILD_OF:
          return resolveChildOf(binaryFunction);
        default:
          throw new UnsupportedOperationException(
              String.format("Unsupported BinaryFunction.Operator %s", binaryFunction.operator()));
      }
    }

    /**
     * Returns an SQL string for a {@link BinaryFunction.Operator#DESCENDANT_OF_INCLUSIVE} filter
     * function.
     */
    private String resolveDescendantOfInclusive(BinaryFunction binaryFunction) {
      Preconditions.checkArgument(
          binaryFunction.operator().equals(BinaryFunction.Operator.DESCENDANT_OF_INCLUSIVE));
      if (!(binaryFunction.left() instanceof Expression.AttributeExpression)) {
        throw new BadRequestException("DESCENDANT_OF only supported for attribute left operand.");
      }
      Expression.AttributeExpression attributeExpression =
          (Expression.AttributeExpression) binaryFunction.left();
      Hierarchy hierarchy =
          searchContext
              .underlay()
              .hierarchies()
              .get(attributeExpression.attributeVariable().attribute());
      if (hierarchy == null) {
        throw new BadRequestException(
            String.format(
                "DESCENDANT_OF only supported for hierarchical attributes, but [%s] has no known hierarchy.",
                attributeExpression.attributeVariable().attribute()));
      }
      DescendantsTable descendantsTable = hierarchy.descendantsTable();

      ExpressionVisitor expressionVisitor = new ExpressionVisitor(searchContext);
      String rightSql = binaryFunction.right().accept(expressionVisitor);
      String template =
          "(${attribute} = ${right} OR ${attribute} IN "
              + "(SELECT ${descendant} FROM ${hierarchy_table} WHERE ${ancestor} = ${right}))";

      Map<String, String> params =
          ImmutableMap.<String, String>builder()
              .put("attribute", binaryFunction.left().accept(expressionVisitor))
              .put("hierarchy_table", underlayResolver.resolveTable(descendantsTable.table()))
              .put("ancestor", descendantsTable.ancestor().name())
              .put("descendant", descendantsTable.descendant().name())
              .put("right", rightSql)
              .build();
      return StringSubstitutor.replace(template, params);
    }

    /** Returns an SQL string for a {@link BinaryFunction.Operator#CHILD_OF} filter function. */
    private String resolveChildOf(BinaryFunction binaryFunction) {
      Preconditions.checkArgument(
          binaryFunction.operator().equals(BinaryFunction.Operator.CHILD_OF));
      if (!(binaryFunction.left() instanceof Expression.AttributeExpression)) {
        throw new BadRequestException("CHILD_OF only supported for attribute left operand.");
      }
      Expression.AttributeExpression attributeExpression =
          (Expression.AttributeExpression) binaryFunction.left();
      Hierarchy hierarchy =
          searchContext
              .underlay()
              .hierarchies()
              .get(attributeExpression.attributeVariable().attribute());
      if (hierarchy == null) {
        throw new BadRequestException(
            String.format(
                "CHILD_OF only supported for hierarchical attributes, but [%s] has no known hierarchy.",
                attributeExpression.attributeVariable().attribute()));
      }
      ChildrenTable childrenTable = hierarchy.childrenTable();

      ExpressionVisitor expressionVisitor = new ExpressionVisitor(searchContext);
      String rightSql = binaryFunction.right().accept(expressionVisitor);
      String template =
          "${attribute} IN "
              + "(SELECT ${child} FROM ${hierarchy_table} WHERE ${parent} = ${right})";

      Map<String, String> params =
          ImmutableMap.<String, String>builder()
              .put("attribute", binaryFunction.left().accept(expressionVisitor))
              .put(
                  "hierarchy_table",
                  underlayResolver.resolveTable(childrenTable.table(), childrenTable.tableFilter()))
              .put("child", childrenTable.child().name())
              .put("parent", childrenTable.parent().name())
              .put("right", rightSql)
              .build();
      return StringSubstitutor.replace(template, params);
    }

    @Override
    public String visitRelationship(RelationshipFilter relationshipFilter) {
      String innerFilterSql = relationshipFilter.filter().accept(this);
      return underlayResolver.resolveRelationship(
          relationshipFilter.outerVariable(), relationshipFilter.newVariable(), innerFilterSql);
    }

    @Override
    public String visitTextSearch(TextSearchFilter textSearchFilter) {
      TextSearchInformation textSearchInfo =
          searchContext
              .underlay()
              .textSearchInformation()
              .get(textSearchFilter.entityVariable().entity());
      if (textSearchInfo == null) {
        throw new BadRequestException(
            String.format(
                "Text search only supported for entities with related information defined, but [%s] has no known information.",
                textSearchFilter.entityVariable().entity().name()));
      }
      TextTable textTable = textSearchInfo.textTable();

      Column primaryKeyColumn =
          searchContext.underlay().primaryKeys().get(textSearchFilter.entityVariable().entity());
      Preconditions.checkArgument(
          primaryKeyColumn != null,
          "Unable to find a primary key for entity '%s'",
          textSearchFilter.entityVariable().entity());

      String template =
          "${entity_table_ref}.${entity_primary_key} IN "
              + "(SELECT ${lookup_table_key} FROM ${text_table} WHERE CONTAINS_SUBSTR(${full_text}, ${search_text}))";
      Map<String, String> params =
          ImmutableMap.<String, String>builder()
              .put("entity_table_ref", textSearchFilter.entityVariable().variable().name())
              .put("entity_primary_key", primaryKeyColumn.name())
              .put("text_table", underlayResolver.resolveTable(textTable.table()))
              .put("lookup_table_key", textTable.lookupTableKey().name())
              .put("full_text", textTable.fullText().name())
              .put(
                  "search_text",
                  textSearchFilter.term().accept(new ExpressionVisitor(searchContext)))
              .build();
      return StringSubstitutor.replace(template, params);
    }

    @Override
    public String visitNull(NullFilter nullFilter) {
      return "TRUE";
    }
  }

  /** A {@link Expression.Visitor} for creating SQL for expressions. */
  static class ExpressionVisitor implements Expression.Visitor<String> {
    private final UnderlayResolver underlayResolver;

    ExpressionVisitor(SearchContext searchContext) {
      this.underlayResolver =
          new UnderlayResolver(searchContext.underlay(), searchContext.randomNumberGenerator());
    }

    @Override
    public String visitLiteral(Expression.Literal literal) {
      // TODO parameterize output to avoid injection.
      if (literal.value() == null) {
        return "NULL";
      }
      switch (literal.dataType()) {
        case STRING:
          return String.format("'%s'", literal.value());
        case INT64:
          return literal.value();
        default:
          throw new UnsupportedOperationException(
              String.format("Unsupported DataType %s", literal.dataType()));
      }
    }

    @Override
    public String visitAttribute(AttributeExpression attributeExpression) {
      return underlayResolver.resolveAttribute(attributeExpression.attributeVariable());
    }
  }

  /** Resolves logical entity model expressions to backing SQL constructs for an underlay. */
  private static class UnderlayResolver {

    private final Underlay underlay;
    private final RandomNumberGenerator randomNumberGenerator;

    UnderlayResolver(Underlay underlay, RandomNumberGenerator randomNumberGenerator) {
      this.underlay = underlay;
      this.randomNumberGenerator = randomNumberGenerator;
    }

    /** Resolve an {@link EntityVariable} as an SQL table clause. */
    public String resolveTable(EntityVariable entityVariable) {
      Column primaryKey = underlay.primaryKeys().get(entityVariable.entity());
      if (primaryKey == null) {
        throw new IllegalArgumentException(
            String.format("Unable to find primary key for entity %s", entityVariable.entity()));
      }
      String resolvedTable =
          resolveTable(primaryKey.table(), underlay.tableFilters().get(entityVariable.entity()));
      return String.format("%s AS %s", resolvedTable, entityVariable.variable().name());
    }

    /** Resolve a {@link Table} into a SQL string. */
    private String resolveTable(Table table) {
      // `projectId.datasetId`.table
      return String.format(
          "`%s.%s`.%s", table.dataset().projectId(), table.dataset().datasetId(), table.name());
    }

    /** Resolve a {@link Table} with a {@link TableFilter} into a SQL string. */
    private String resolveTable(Table table, @Nullable TableFilter tableFilter) {
      if (tableFilter == null) {
        return resolveTable(table);
      } else if (tableFilter.arrayColumnFilter() != null) {
        // (SELECT * FROM `projectId.datasetId`.table WHERE whereclause)
        return String.format(
            "(SELECT * FROM `%s.%s`.%s WHERE %s)",
            table.dataset().projectId(),
            table.dataset().datasetId(),
            table.name(),
            resolveArrayColumnFilter(tableFilter.arrayColumnFilter()));
      } else if (tableFilter.binaryColumnFilter() != null) {
        // (SELECT * FROM `projectId.datasetId`.table WHERE whereclause)
        return String.format(
            "(SELECT * FROM `%s.%s`.%s WHERE %s)",
            table.dataset().projectId(),
            table.dataset().datasetId(),
            table.name(),
            resolveBinaryColumnFilter(tableFilter.binaryColumnFilter()));
      } else {
        throw new IllegalArgumentException(
            "Invalid table filter missing binary and array column filters.");
      }
    }

    /** Resolve an {@link ArrayColumnFilter} into a SQL string WHERE clause. */
    private String resolveArrayColumnFilter(ArrayColumnFilter arrayColumnFilter) {
      String joinOperatorInWhereClause;
      switch (arrayColumnFilter.operator()) {
        case AND:
          joinOperatorInWhereClause = "AND";
          break;
        case OR:
          joinOperatorInWhereClause = "OR";
          break;
        default:
          throw new IllegalArgumentException(
              "Unknown array column filter operator type: " + arrayColumnFilter.operator());
      }

      // %s AND %s AND (%s OR %s) ...
      return Streams.concat(
              arrayColumnFilter.binaryColumnFilters().stream()
                  .map(f -> resolveBinaryColumnFilter(f)),
              arrayColumnFilter.arrayColumnFilters().stream()
                  .map(f -> "(" + resolveArrayColumnFilter(f) + ")"))
          .collect(Collectors.joining(" " + joinOperatorInWhereClause + " "));
    }

    /** Resolve a {@link BinaryColumnFilter} into a SQL string WHERE clause. */
    private String resolveBinaryColumnFilter(BinaryColumnFilter binaryColumnFilter) {
      String valueInWhereClause;
      boolean valueIsNull = binaryColumnFilter.value() == null;
      if (valueIsNull) {
        valueInWhereClause = "NULL";
      } else {
        switch (binaryColumnFilter.column().dataType()) {
          case STRING:
            valueIsNull = binaryColumnFilter.value().stringVal() == null;
            valueInWhereClause = String.format("'%s'", binaryColumnFilter.value().stringVal());
            break;
          case INT64:
            valueIsNull = binaryColumnFilter.value().int64Val() == null;
            valueInWhereClause = String.valueOf(binaryColumnFilter.value().int64Val());
            break;
          default:
            throw new IllegalArgumentException(
                "Unknown column data type: " + binaryColumnFilter.column().dataType());
        }
      }

      String operatorInWhereClause;
      switch (binaryColumnFilter.operator()) {
        case EQUALS:
          operatorInWhereClause = valueIsNull ? "IS" : "=";
          break;
        case NOT_EQUALS:
          operatorInWhereClause = valueIsNull ? "IS NOT" : "!=";
          break;
        case LESS_THAN:
          operatorInWhereClause = "<";
          break;
        case GREATER_THAN:
          operatorInWhereClause = ">";
          break;
        default:
          throw new IllegalArgumentException(
              "Unknown binary column filter operator type: " + binaryColumnFilter.operator());
      }

      // columnFilter=value
      return String.format(
          "%s %s %s",
          binaryColumnFilter.column().name(), operatorInWhereClause, valueInWhereClause);
    }

    /** Resolve an {@link AttributeExpression} as an SQL expression. */
    public String resolveAttribute(AttributeVariable attributeVariable) {
      AttributeMapping mapping = underlay.attributeMappings().get(attributeVariable.attribute());
      if (mapping == null) {
        throw new IllegalArgumentException(
            String.format(
                "Unable to find attribute mapping for attribute %s",
                attributeVariable.attribute()));
      }
      return mapping.accept(new AttributeResolver(attributeVariable));
    }

    /**
     * A {@link AttributeMapping.Visitor} for resolving an {@link AttributeVariable} as an SQL
     * expression.
     */
    private class AttributeResolver implements AttributeMapping.Visitor<String> {
      private final AttributeVariable attributeVariable;

      private AttributeResolver(AttributeVariable attributeVariable) {
        this.attributeVariable = attributeVariable;
      }

      @Override
      public String visitSimpleColumn(SimpleColumn simpleColumn) {
        // variableName.column
        return String.format(
            "%s.%s", attributeVariable.variable().name(), simpleColumn.column().name());
      }

      @Override
      public String visitLookupColumn(LookupColumn lookupColumn) {
        String template =
            "(SELECT ${lookup_table_name}.${lookup_column} FROM ${lookup_table} WHERE ${lookup_table_name}.${lookup_key} = ${var}.${primary_key})";
        Map<String, String> params =
            ImmutableMap.<String, String>builder()
                .put("lookup_table_name", lookupColumn.lookupColumn().table().name())
                .put("lookup_column", lookupColumn.lookupColumn().name())
                .put("lookup_table", resolveTable(lookupColumn.lookupColumn().table()))
                .put("lookup_key", lookupColumn.lookupTableKey().name())
                .put("var", attributeVariable.variable().name())
                .put("primary_key", lookupColumn.primaryTableLookupKey().name())
                .build();
        return StringSubstitutor.replace(template, params);
      }

      @Override
      public String visitHierarchyPathColumn(
          AttributeMapping.HierarchyPathColumn hierarchyPathColumn) {
        String template =
            "(SELECT ${hierarchy_path_table_name}.${path_column} "
                + "FROM ${hierarchy_path_table} "
                + "WHERE ${hierarchy_path_table_name}.${node_column} = ${hierarchy_attribute_value})";
        Map<String, String> params =
            ImmutableMap.<String, String>builder()
                .put(
                    "hierarchy_path_table_name",
                    hierarchyPathColumn.hierarchy().pathsTable().table().name())
                .put(
                    "hierarchy_path_table",
                    resolveTable(hierarchyPathColumn.hierarchy().pathsTable().table()))
                .put("path_column", hierarchyPathColumn.hierarchy().pathsTable().path().name())
                .put("node_column", hierarchyPathColumn.hierarchy().pathsTable().node().name())
                .put(
                    "hierarchy_attribute_value",
                    hierarchyPathColumn.hierarchyAttributeMapping().accept(this))
                .build();
        return StringSubstitutor.replace(template, params);
      }

      @Override
      public String visitHierarchyNumChildrenColumn(
          AttributeMapping.HierarchyNumChildrenColumn hierarchyNumChildrenColumn) {
        String template =
            "(SELECT ${hierarchy_path_table_name}.${numChildren_column} "
                + "FROM ${hierarchy_path_table} "
                + "WHERE ${hierarchy_path_table_name}.${node_column} = ${hierarchy_attribute_value})";
        Map<String, String> params =
            ImmutableMap.<String, String>builder()
                .put(
                    "hierarchy_path_table_name",
                    hierarchyNumChildrenColumn.hierarchy().pathsTable().table().name())
                .put(
                    "hierarchy_path_table",
                    resolveTable(hierarchyNumChildrenColumn.hierarchy().pathsTable().table()))
                .put(
                    "numChildren_column",
                    hierarchyNumChildrenColumn.hierarchy().pathsTable().numChildren().name())
                .put(
                    "node_column",
                    hierarchyNumChildrenColumn.hierarchy().pathsTable().node().name())
                .put(
                    "hierarchy_attribute_value",
                    hierarchyNumChildrenColumn.hierarchyAttributeMapping().accept(this))
                .build();
        return StringSubstitutor.replace(template, params);
      }
    }

    /**
     * Create an SQL filter clause linking the outer variable with a newly bound entity variable,
     * with the {@code innerFilterSql} included as a filter on the newly bound entity variable.
     */
    public String resolveRelationship(
        EntityVariable outerVariable, EntityVariable innerVariable, String innerFilterSql) {
      Optional<Relationship> relationship =
          underlay.getRelationship(outerVariable.entity(), innerVariable.entity());
      if (relationship.isEmpty()) {
        throw new IllegalArgumentException(
            String.format(
                "Unable to resolve RelationshipFilter for unknown relationship. Outer entity {%s}, inner entity {%s}",
                outerVariable.entity(), innerVariable.entity()));
      }
      Object relationshipMapping = underlay.relationshipMappings().get(relationship.get());
      if (relationshipMapping == null) {
        throw new IllegalArgumentException(
            String.format(
                "Unable to find relationship mapping for relationship %s", relationship.get()));
      }

      if (relationshipMapping instanceof ForeignKey) {
        ForeignKey foreignKey = (ForeignKey) relationshipMapping;
        Table outerPrimaryTable = underlay.primaryKeys().get(outerVariable.entity()).table();
        Table innerPrimaryTable = underlay.primaryKeys().get(innerVariable.entity()).table();

        Column outerColumn = getKeyForTable(foreignKey, outerPrimaryTable);
        Column innerColumn = getKeyForTable(foreignKey, innerPrimaryTable);

        String template =
            "${outer_var}.${outer_column} IN (SELECT ${inner_var}.${inner_column} FROM ${inner_table} WHERE ${inner_filter})";
        Map<String, String> params =
            ImmutableMap.<String, String>builder()
                .put("outer_var", outerVariable.variable().name())
                .put("outer_column", outerColumn.name())
                .put("inner_var", innerVariable.variable().name())
                .put("inner_column", innerColumn.name())
                .put("inner_table", resolveTable(innerVariable))
                .put("inner_filter", innerFilterSql)
                .build();
        return StringSubstitutor.replace(template, params);
      } else if (relationshipMapping instanceof IntermediateTable) {
        IntermediateTable intermediateTable = (IntermediateTable) relationshipMapping;

        Column outerColumn;
        Column innerColumn;
        Column outerIntermediateColumn;
        Column innerIntermediateColumn;
        if (outerVariable.entity().equals(relationship.get().entity1())) {
          // outer variable = entity 1, inner variable = entity 2
          outerColumn = intermediateTable.entity1EntityTableKey();
          innerColumn = intermediateTable.entity2EntityTableKey();
          outerIntermediateColumn = intermediateTable.entity1IntermediateTableKey();
          innerIntermediateColumn = intermediateTable.entity2IntermediateTableKey();
        } else {
          // outer variable = entity 2, inner variable = entity 1
          outerColumn = intermediateTable.entity2EntityTableKey();
          innerColumn = intermediateTable.entity1EntityTableKey();
          outerIntermediateColumn = intermediateTable.entity2IntermediateTableKey();
          innerIntermediateColumn = intermediateTable.entity1IntermediateTableKey();
        }

        String template =
            "${outer_var}.${outer_column} IN "
                + "(SELECT ${intermediate_var}.${outer_intermediate_column} FROM ${intermediate_table} AS ${intermediate_var} WHERE ${intermediate_var}.${inner_intermediate_column} IN "
                + "(SELECT ${inner_var}.${inner_column} FROM ${inner_table} WHERE ${inner_filter}))";
        Map<String, String> params =
            ImmutableMap.<String, String>builder()
                .put("outer_var", outerVariable.variable().name())
                .put("outer_column", outerColumn.name())
                .put("inner_var", innerVariable.variable().name())
                .put("inner_column", innerColumn.name())
                .put("inner_table", resolveTable(innerVariable))
                .put("inner_filter", innerFilterSql)
                .put("intermediate_var", generateIntermediateTableAlias(relationship.get().name()))
                .put(
                    "intermediate_table",
                    resolveTable(intermediateTable.entity1IntermediateTableKey().table()))
                .put("outer_intermediate_column", outerIntermediateColumn.name())
                .put("inner_intermediate_column", innerIntermediateColumn.name())
                .build();
        return StringSubstitutor.replace(template, params);
      } else {
        throw new IllegalArgumentException(
            String.format(
                "Unknown relationship mapping type for relationship %s", relationship.get()));
      }
    }

    /** Generate an alias for an intermediate table prefixed with the given relationship name. */
    private String generateIntermediateTableAlias(String relationshipName) {
      return relationshipName + randomNumberGenerator.getNext();
    }

    /** Returns the primary key or the foreign key that matches the table, or else throw. */
    private static Column getKeyForTable(ForeignKey foreignKey, Table table) {
      if (foreignKey.primaryKey().table().equals(table)) {
        return foreignKey.primaryKey();
      } else if (foreignKey.foreignKey().table().equals(table)) {
        return foreignKey.foreignKey();
      } else {
        throw new IllegalArgumentException(
            String.format(
                "Table {%s} matches neither the primary key {%s} nor the foreign key {%s}.",
                table, foreignKey.primaryKey(), foreignKey.foreignKey()));
      }
    }
  }
}

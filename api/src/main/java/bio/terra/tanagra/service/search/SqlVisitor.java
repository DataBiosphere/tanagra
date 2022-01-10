package bio.terra.tanagra.service.search;

import bio.terra.common.exception.BadRequestException;
import bio.terra.tanagra.service.search.Expression.AttributeExpression;
import bio.terra.tanagra.service.search.Filter.ArrayFunction;
import bio.terra.tanagra.service.search.Filter.BinaryFunction;
import bio.terra.tanagra.service.search.Filter.NullFilter;
import bio.terra.tanagra.service.search.Filter.RelationshipFilter;
import bio.terra.tanagra.service.search.Selection.PrimaryKey;
import bio.terra.tanagra.service.underlay.AttributeMapping;
import bio.terra.tanagra.service.underlay.AttributeMapping.LookupColumn;
import bio.terra.tanagra.service.underlay.AttributeMapping.SimpleColumn;
import bio.terra.tanagra.service.underlay.Column;
import bio.terra.tanagra.service.underlay.ForeignKey;
import bio.terra.tanagra.service.underlay.Hierarchy;
import bio.terra.tanagra.service.underlay.Hierarchy.DescendantsTable;
import bio.terra.tanagra.service.underlay.IntermediateTable;
import bio.terra.tanagra.service.underlay.Table;
import bio.terra.tanagra.service.underlay.TableFilter;
import bio.terra.tanagra.service.underlay.Underlay;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.text.StringSubstitutor;

/** Visitors for walking query constructs to create SQL. */
// TODO consider how this may need to be split for different SQL backends.
// TODO consider the jOOQ DSL.
public class SqlVisitor {
  private final SearchContext searchContext;
  private final UnderlayResolver underlayResolver;

  public SqlVisitor(SearchContext searchContext) {
    this.searchContext = searchContext;
    this.underlayResolver = new UnderlayResolver(searchContext.underlay());
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
    return StringSubstitutor.replace(template, params);
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
      return String.format("%s AS %s", expression, selectExpression.name());
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
      this.underlayResolver = new UnderlayResolver(searchContext.underlay());
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
    public String visitBinaryComparision(BinaryFunction binaryFunction) {
      ExpressionVisitor expressionVisitor = new ExpressionVisitor(searchContext);

      String leftSql = binaryFunction.left().accept(expressionVisitor);
      String rightSql = binaryFunction.right().accept(expressionVisitor);
      switch (binaryFunction.operator()) {
        case LESS_THAN:
          return String.format("%s < %s", leftSql, rightSql);
        case EQUALS:
          return String.format("%s = %s", leftSql, rightSql);
        case DESCENDANT_OF_INCLUSIVE:
          return resolveDescendantOfInclusive(binaryFunction);
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

    @Override
    public String visitRelationship(RelationshipFilter relationshipFilter) {
      String innerFilterSql = relationshipFilter.filter().accept(this);
      return underlayResolver.resolveRelationship(
          relationshipFilter.outerVariable(), relationshipFilter.newVariable(), innerFilterSql);
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
      this.underlayResolver = new UnderlayResolver(searchContext.underlay());
    }

    @Override
    public String visitLiteral(Expression.Literal literal) {
      // TODO parameterize output to avoid injection.
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

    UnderlayResolver(Underlay underlay) {
      this.underlay = underlay;
    }

    /** Resolve an {@link EntityVariable} as an SQL table clause. */
    public String resolveTable(EntityVariable entityVariable) {
      Column primaryKey = underlay.primaryKeys().get(entityVariable.entity());
      if (primaryKey == null) {
        throw new IllegalArgumentException(
            String.format("Unable to find primary key for entity %s", entityVariable.entity()));
      }
      TableFilter tableFilter = underlay.tableFilters().get(entityVariable.entity());
      String resolvedTable;
      if (tableFilter == null) {
        resolvedTable = resolveTable(primaryKey.table());
      } else {
        resolvedTable = resolveTable(primaryKey.table(), tableFilter);
      }
      return String.format("%s AS %s", resolvedTable, entityVariable.variable().name());
    }

    private String resolveTable(Table table) {
      // `projectId.datasetId`.table
      return String.format(
          "`%s.%s`.%s", table.dataset().projectId(), table.dataset().datasetId(), table.name());
    }

    private String resolveTable(Table table, TableFilter tableFilter) {
      String operatorInWhereClause;
      switch (tableFilter.binaryColumnFilter().operator()) {
        case EQUALS:
          operatorInWhereClause = "=";
          break;
        case LESS_THAN:
          operatorInWhereClause = "<";
          break;
        case GREATER_THAN:
          operatorInWhereClause = ">";
          break;
        default:
          throw new IllegalArgumentException(
              "Unknown column filter operator type: "
                  + tableFilter.binaryColumnFilter().operator());
      }

      String valueInWhereClause;
      switch (tableFilter.binaryColumnFilter().column().dataType()) {
        case STRING:
          valueInWhereClause =
              String.format("'%s'", tableFilter.binaryColumnFilter().value().stringVal());
          break;
        case INT64:
          valueInWhereClause = String.valueOf(tableFilter.binaryColumnFilter().value().int64Val());
          break;
        default:
          throw new IllegalArgumentException(
              "Unknown column data type: " + tableFilter.binaryColumnFilter().column().dataType());
      }

      // (SELECT * FROM `projectId.datasetId`.table WHERE columnFilter=value)
      return String.format(
          "(SELECT * FROM `%s.%s`.%s WHERE %s %s %s)",
          table.dataset().projectId(),
          table.dataset().datasetId(),
          table.name(),
          tableFilter.binaryColumnFilter().column().name(),
          operatorInWhereClause,
          valueInWhereClause);
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
        return "mariko"; // !!!
      } else {
        throw new IllegalArgumentException(
            String.format(
                "Unknown relationship mapping type for relationship %s", relationship.get()));
      }
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

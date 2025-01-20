package bio.terra.tanagra.query.sql.translator;

import bio.terra.tanagra.api.field.AttributeField;
import bio.terra.tanagra.api.field.CountDistinctField;
import bio.terra.tanagra.api.field.HierarchyIsMemberField;
import bio.terra.tanagra.api.field.HierarchyIsRootField;
import bio.terra.tanagra.api.field.HierarchyNumChildrenField;
import bio.terra.tanagra.api.field.HierarchyPathField;
import bio.terra.tanagra.api.field.RelatedEntityIdCountField;
import bio.terra.tanagra.api.field.ValueDisplayField;
import bio.terra.tanagra.api.filter.AttributeFilter;
import bio.terra.tanagra.api.filter.BooleanAndOrFilter;
import bio.terra.tanagra.api.filter.BooleanAndOrFilter.LogicalOperator;
import bio.terra.tanagra.api.filter.BooleanNotFilter;
import bio.terra.tanagra.api.filter.EntityFilter;
import bio.terra.tanagra.api.filter.GroupHasItemsFilter;
import bio.terra.tanagra.api.filter.HierarchyHasAncestorFilter;
import bio.terra.tanagra.api.filter.HierarchyHasParentFilter;
import bio.terra.tanagra.api.filter.HierarchyIsLeafFilter;
import bio.terra.tanagra.api.filter.HierarchyIsMemberFilter;
import bio.terra.tanagra.api.filter.HierarchyIsRootFilter;
import bio.terra.tanagra.api.filter.ItemInGroupFilter;
import bio.terra.tanagra.api.filter.OccurrenceForPrimaryFilter;
import bio.terra.tanagra.api.filter.PrimaryWithCriteriaFilter;
import bio.terra.tanagra.api.filter.RelationshipFilter;
import bio.terra.tanagra.api.filter.TemporalPrimaryFilter;
import bio.terra.tanagra.api.filter.TextSearchFilter;
import bio.terra.tanagra.api.shared.BinaryOperator;
import bio.terra.tanagra.api.shared.Literal;
import bio.terra.tanagra.api.shared.NaryOperator;
import bio.terra.tanagra.api.shared.OrderByDirection;
import bio.terra.tanagra.api.shared.UnaryOperator;
import bio.terra.tanagra.exception.InvalidQueryException;
import bio.terra.tanagra.exception.SystemException;
import bio.terra.tanagra.query.sql.SqlField;
import bio.terra.tanagra.query.sql.SqlParams;
import bio.terra.tanagra.query.sql.SqlQueryField;
import bio.terra.tanagra.query.sql.SqlTable;
import bio.terra.tanagra.query.sql.translator.filter.BooleanAndOrFilterTranslator;
import bio.terra.tanagra.query.sql.translator.filter.BooleanNotFilterTranslator;
import bio.terra.tanagra.query.sql.translator.filter.GroupHasItemsFilterTranslator;
import bio.terra.tanagra.query.sql.translator.filter.ItemInGroupFilterTranslator;
import bio.terra.tanagra.query.sql.translator.filter.OccurrenceForPrimaryFilterTranslator;
import bio.terra.tanagra.underlay.entitymodel.Attribute;
import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.text.StringSubstitutor;

public interface ApiTranslator {
  String FUNCTION_TEMPLATE_FIELD_VAR = "fieldSql";
  String FUNCTION_TEMPLATE_FIELD_VAR_BRACES = "${" + FUNCTION_TEMPLATE_FIELD_VAR + "}";

  String FUNCTION_TEMPLATE_VALUES_VAR = "values";
  String FUNCTION_TEMPLATE_VALUES_VAR_BRACES = "${" + FUNCTION_TEMPLATE_VALUES_VAR + "}";

  default String orderByRandSql() {
    return "RAND()";
  }

  default String orderByDirectionSql(OrderByDirection orderByDirection) {
    return switch (orderByDirection) {
      case ASCENDING -> "ASC";
      case DESCENDING -> "DESC";
      default -> throw new SystemException("Unknown order by direction: " + orderByDirection);
    };
  }

  default String unaryFilterSql(
      SqlField field, UnaryOperator operator, @Nullable String tableAlias, SqlParams sqlParams) {
    return functionWithCommaSeparatedArgsFilterSql(
        field, unaryOperatorTemplateSql(operator), List.of(), tableAlias, sqlParams);
  }

  default String binaryFilterSql(
      SqlField field,
      BinaryOperator operator,
      Literal value,
      @Nullable String tableAlias,
      SqlParams sqlParams) {
    if (value.isNull()) {
      return unaryFilterSql(
          field,
          BinaryOperator.EQUALS.equals(operator)
              ? UnaryOperator.IS_NULL
              : UnaryOperator.IS_NOT_NULL,
          tableAlias,
          sqlParams);
    }

    String operatorTemplateSql =
        FUNCTION_TEMPLATE_FIELD_VAR_BRACES
            + ' '
            + binaryOperatorSql(operator)
            + ' '
            + FUNCTION_TEMPLATE_VALUES_VAR_BRACES;
    return functionWithCommaSeparatedArgsFilterSql(
        field, operatorTemplateSql, List.of(value), tableAlias, sqlParams);
  }

  default String textSearchFilterSql(
      SqlField field,
      TextSearchFilter.TextSearchOperator operator,
      Literal value,
      @Nullable String tableAlias,
      SqlParams sqlParams) {
    return functionWithCommaSeparatedArgsFilterSql(
        field, textSearchOperatorTemplateSql(operator), List.of(value), tableAlias, sqlParams);
  }

  @SuppressWarnings("PMD.AvoidLiteralsInIfCondition")
  default String naryFilterSql(
      SqlField field,
      NaryOperator naryOperator,
      List<Literal> values,
      @Nullable String tableAlias,
      SqlParams sqlParams) {
    String operatorSql;
    switch (naryOperator) {
      case IN:
        operatorSql =
            FUNCTION_TEMPLATE_FIELD_VAR_BRACES
                + " IN ("
                + FUNCTION_TEMPLATE_VALUES_VAR_BRACES
                + ")";
        return functionWithCommaSeparatedArgsFilterSql(
            field, operatorSql, values, tableAlias, sqlParams);
      case NOT_IN:
        operatorSql =
            FUNCTION_TEMPLATE_FIELD_VAR_BRACES
                + " NOT IN ("
                + FUNCTION_TEMPLATE_VALUES_VAR_BRACES
                + ")";
        return functionWithCommaSeparatedArgsFilterSql(
            field, operatorSql, values, tableAlias, sqlParams);
      case BETWEEN:
        if (values.size() != 2) {
          throw new InvalidQueryException("BETWEEN operator expects exactly two values");
        }
        String paramName1 = sqlParams.addParam("val", values.get(0));
        String paramName2 = sqlParams.addParam("val", values.get(1));
        return SqlQueryField.of(field).renderForWhere(tableAlias)
            + " BETWEEN @"
            + paramName1
            + " AND @"
            + paramName2;
      default:
        throw new SystemException("Unknown function template: " + naryOperator);
    }
  }

  String naryFilterOnRepeatedFieldSql(
      SqlField field,
      NaryOperator naryOperator,
      List<Literal> values,
      @Nullable String tableAlias,
      SqlParams sqlParams);

  default String functionWithCommaSeparatedArgsFilterSql(
      SqlField field,
      String functionTemplate,
      List<Literal> values,
      @Nullable String tableAlias,
      SqlParams sqlParams) {
    List<String> valueParamNames = new ArrayList<>();
    values.forEach(value -> valueParamNames.add(sqlParams.addParam("val", value)));
    Map<String, String> substitutorParams =
        Map.of(
            FUNCTION_TEMPLATE_FIELD_VAR, SqlQueryField.of(field).renderForWhere(tableAlias),
            FUNCTION_TEMPLATE_VALUES_VAR,
                valueParamNames.stream()
                    .map(valueParamName -> '@' + valueParamName)
                    .collect(Collectors.joining(",")));
    return StringSubstitutor.replace(functionTemplate, substitutorParams);
  }

  default String binaryOperatorSql(BinaryOperator operator) {
    return switch (operator) {
      case EQUALS -> "=";
      case NOT_EQUALS -> "!=";
      case GREATER_THAN -> ">";
      case LESS_THAN -> "<";
      case GREATER_THAN_OR_EQUAL -> ">=";
      case LESS_THAN_OR_EQUAL -> "<=";
      default -> throw new SystemException("Unknown binary operator: " + operator);
    };
  }

  default String unaryOperatorTemplateSql(UnaryOperator operator) {
    return switch (operator) {
      case IS_NULL -> FUNCTION_TEMPLATE_FIELD_VAR_BRACES + " IS NULL";
      case IS_NOT_NULL -> FUNCTION_TEMPLATE_FIELD_VAR_BRACES + " IS NOT NULL";
      case IS_EMPTY_STRING -> FUNCTION_TEMPLATE_FIELD_VAR_BRACES + " = ''";
      default -> throw new SystemException("Unknown unary operator: " + operator);
    };
  }

  default String textSearchOperatorTemplateSql(TextSearchFilter.TextSearchOperator operator) {
    return switch (operator) {
      case EXACT_MATCH ->
          "REGEXP_CONTAINS(UPPER("
              + FUNCTION_TEMPLATE_FIELD_VAR_BRACES
              + "), UPPER("
              + FUNCTION_TEMPLATE_VALUES_VAR_BRACES
              + "))";
      case FUZZY_MATCH -> throw new InvalidQueryException("Fuzzy text match not supported");
      default -> throw new SystemException("Unknown text search operator: " + operator);
    };
  }

  @SuppressWarnings("checkstyle:ParameterNumber")
  default String inSelectFilterSql(
      SqlField whereField,
      @Nullable String tableAlias,
      SqlField selectField,
      SqlTable table,
      @Nullable String filterSql,
      @Nullable String havingSql,
      boolean appendSqlToTable,
      SqlParams sqlParams,
      Literal... unionAllLiterals) {
    List<String> selectSqls = new ArrayList<>();
    String sqlClause =
        (filterSql != null ? " WHERE " + filterSql : "")
            + (havingSql != null ? ' ' + havingSql : "");
    selectSqls.add(
        "SELECT "
            + SqlQueryField.of(selectField).renderForSelect()
            + " FROM "
            + (appendSqlToTable ? table.render(sqlClause) : table.render() + sqlClause));
    Arrays.stream(unionAllLiterals)
        .forEach(literal -> selectSqls.add("SELECT @" + sqlParams.addParam("val", literal)));
    return SqlQueryField.of(whereField).renderForWhere(tableAlias)
        + " IN ("
        + String.join(" UNION ALL ", selectSqls)
        + ')';
  }

  default String booleanAndOrFilterSql(
      BooleanAndOrFilter.LogicalOperator operator, String... subFilterSqls) {
    return Arrays.stream(subFilterSqls)
        .map(subFilterSql -> '(' + subFilterSql + ')')
        .collect(Collectors.joining(' ' + logicalOperatorSql(operator) + ' '));
  }

  default String booleanNotFilterSql(String subFilterSql) {
    return "NOT (" + subFilterSql + ")";
  }

  default String logicalOperatorSql(BooleanAndOrFilter.LogicalOperator operator) {
    return switch (operator) {
      case AND -> "AND";
      case OR -> "OR";
      default -> throw new SystemException("Unknown logical operator: " + operator);
    };
  }

  default String havingSql(
      SqlField groupByField,
      BinaryOperator havingOperator,
      Integer havingCount,
      @Nullable String tableAlias,
      SqlParams sqlParams) {
    String groupByCountParam =
        sqlParams.addParam("groupByCount", Literal.forInt64(Long.valueOf(havingCount)));
    return "GROUP BY "
        + SqlQueryField.of(groupByField).renderForGroupBy(tableAlias, false)
        + " HAVING COUNT(*) "
        + binaryOperatorSql(havingOperator)
        + " @"
        + groupByCountParam;
  }

  ApiFieldTranslator translator(AttributeField attributeField);

  ApiFieldTranslator translator(CountDistinctField countDistinctField);

  ApiFieldTranslator translator(HierarchyIsMemberField hierarchyIsMemberField);

  ApiFieldTranslator translator(HierarchyIsRootField hierarchyIsRootField);

  ApiFieldTranslator translator(HierarchyNumChildrenField hierarchyNumChildrenField);

  ApiFieldTranslator translator(HierarchyPathField hierarchyPathField);

  ApiFieldTranslator translator(RelatedEntityIdCountField relatedEntityIdCountField);

  default ApiFieldTranslator translator(ValueDisplayField valueDisplayField) {
    if (valueDisplayField instanceof AttributeField) {
      return translator((AttributeField) valueDisplayField);
    } else if (valueDisplayField instanceof CountDistinctField) {
      return translator((CountDistinctField) valueDisplayField);
    } else if (valueDisplayField instanceof HierarchyIsMemberField) {
      return translator((HierarchyIsMemberField) valueDisplayField);
    } else if (valueDisplayField instanceof HierarchyIsRootField) {
      return translator((HierarchyIsRootField) valueDisplayField);
    } else if (valueDisplayField instanceof HierarchyNumChildrenField) {
      return translator((HierarchyNumChildrenField) valueDisplayField);
    } else if (valueDisplayField instanceof HierarchyPathField) {
      return translator((HierarchyPathField) valueDisplayField);
    } else if (valueDisplayField instanceof RelatedEntityIdCountField) {
      return translator((RelatedEntityIdCountField) valueDisplayField);
    } else {
      throw new InvalidQueryException("No SQL translator defined for field: " + valueDisplayField);
    }
  }

  ApiFilterTranslator translator(
      AttributeFilter attributeFilter, Map<Attribute, SqlField> attributeSwapFields);

  Optional<ApiFilterTranslator> mergedTranslator(
      List<AttributeFilter> attributeFilter,
      LogicalOperator logicalOperator,
      Map<Attribute, SqlField> attributeSwapFields);

  default ApiFilterTranslator translator(
      BooleanAndOrFilter booleanAndOrFilter, Map<Attribute, SqlField> attributeSwapFields) {
    return new BooleanAndOrFilterTranslator(this, booleanAndOrFilter, attributeSwapFields);
  }

  default ApiFilterTranslator translator(
      BooleanNotFilter booleanNotFilter, Map<Attribute, SqlField> attributeSwapFields) {
    return new BooleanNotFilterTranslator(this, booleanNotFilter, attributeSwapFields);
  }

  ApiFilterTranslator translator(
      HierarchyHasAncestorFilter hierarchyHasAncestorFilter,
      Map<Attribute, SqlField> attributeSwapFields);

  ApiFilterTranslator translator(
      HierarchyHasParentFilter hierarchyHasParentFilter,
      Map<Attribute, SqlField> attributeSwapFields);

  ApiFilterTranslator translator(
      HierarchyIsLeafFilter hierarchyIsLeafFilter, Map<Attribute, SqlField> attributeSwapFields);

  ApiFilterTranslator translator(
      HierarchyIsMemberFilter hierarchyIsMemberFilter,
      Map<Attribute, SqlField> attributeSwapFields);

  ApiFilterTranslator translator(
      HierarchyIsRootFilter hierarchyIsRootFilter, Map<Attribute, SqlField> attributeSwapFields);

  default ApiFilterTranslator translator(
      GroupHasItemsFilter groupHasItemsFilter, Map<Attribute, SqlField> attributeSwapFields) {
    return new GroupHasItemsFilterTranslator(this, groupHasItemsFilter, attributeSwapFields);
  }

  default ApiFilterTranslator translator(
      ItemInGroupFilter itemInGroupFilter, Map<Attribute, SqlField> attributeSwapFields) {
    return new ItemInGroupFilterTranslator(this, itemInGroupFilter, attributeSwapFields);
  }

  default ApiFilterTranslator translator(
      OccurrenceForPrimaryFilter occurrenceForPrimaryFilter,
      Map<Attribute, SqlField> attributeSwapFields) {
    return new OccurrenceForPrimaryFilterTranslator(
        this, occurrenceForPrimaryFilter, attributeSwapFields);
  }

  ApiFilterTranslator translator(
      PrimaryWithCriteriaFilter primaryWithCriteriaFilter,
      Map<Attribute, SqlField> attributeSwapFields);

  ApiFilterTranslator translator(
      RelationshipFilter relationshipFilter, Map<Attribute, SqlField> attributeSwapFields);

  ApiFilterTranslator translator(
      TextSearchFilter textSearchFilter, Map<Attribute, SqlField> attributeSwapFields);

  ApiFilterTranslator translator(
      TemporalPrimaryFilter temporalPrimaryFilter, Map<Attribute, SqlField> attributeSwapFields);

  default ApiFilterTranslator translator(EntityFilter entityFilter) {
    return translator(entityFilter, null);
  }

  default ApiFilterTranslator translator(
      EntityFilter entityFilter, Map<Attribute, SqlField> attributeSwapFields) {
    if (entityFilter instanceof AttributeFilter) {
      return translator((AttributeFilter) entityFilter, attributeSwapFields);
    } else if (entityFilter instanceof BooleanAndOrFilter) {
      return translator((BooleanAndOrFilter) entityFilter, attributeSwapFields);
    } else if (entityFilter instanceof BooleanNotFilter) {
      return translator((BooleanNotFilter) entityFilter, attributeSwapFields);
    } else if (entityFilter instanceof HierarchyHasAncestorFilter) {
      return translator((HierarchyHasAncestorFilter) entityFilter, attributeSwapFields);
    } else if (entityFilter instanceof HierarchyHasParentFilter) {
      return translator((HierarchyHasParentFilter) entityFilter, attributeSwapFields);
    } else if (entityFilter instanceof HierarchyIsLeafFilter) {
      return translator((HierarchyIsLeafFilter) entityFilter, attributeSwapFields);
    } else if (entityFilter instanceof HierarchyIsMemberFilter) {
      return translator((HierarchyIsMemberFilter) entityFilter, attributeSwapFields);
    } else if (entityFilter instanceof HierarchyIsRootFilter) {
      return translator((HierarchyIsRootFilter) entityFilter, attributeSwapFields);
    } else if (entityFilter instanceof GroupHasItemsFilter) {
      return translator((GroupHasItemsFilter) entityFilter, attributeSwapFields);
    } else if (entityFilter instanceof ItemInGroupFilter) {
      return translator((ItemInGroupFilter) entityFilter, attributeSwapFields);
    } else if (entityFilter instanceof OccurrenceForPrimaryFilter) {
      return translator((OccurrenceForPrimaryFilter) entityFilter, attributeSwapFields);
    } else if (entityFilter instanceof PrimaryWithCriteriaFilter) {
      return translator((PrimaryWithCriteriaFilter) entityFilter, attributeSwapFields);
    } else if (entityFilter instanceof RelationshipFilter) {
      return translator((RelationshipFilter) entityFilter, attributeSwapFields);
    } else if (entityFilter instanceof TextSearchFilter) {
      return translator((TextSearchFilter) entityFilter, attributeSwapFields);
    } else if (entityFilter instanceof TemporalPrimaryFilter) {
      return translator((TemporalPrimaryFilter) entityFilter, attributeSwapFields);
    } else {
      throw new InvalidQueryException("No SQL translator defined for filter");
    }
  }

  default Optional<ApiFilterTranslator> optionalMergedTranslator(
      List<EntityFilter> entityFilters,
      LogicalOperator logicalOperator,
      Map<Attribute, SqlField> attributeSwapFields) {
    // A list of sub-filters can be merged (optimized) if they are of the same type
    // Additional checks may be needed for individual sub-filter types
    EntityFilter firstFilter = entityFilters.get(0);

    // At this time only optimize attribute filters are supported
    if (!(firstFilter instanceof AttributeFilter)
        || !EntityFilter.areSameFilterType(entityFilters)) {
      return Optional.empty();
    }
    return mergedTranslator(
        entityFilters.stream().map(filter -> (AttributeFilter) filter).toList(),
        logicalOperator,
        attributeSwapFields);
  }
}

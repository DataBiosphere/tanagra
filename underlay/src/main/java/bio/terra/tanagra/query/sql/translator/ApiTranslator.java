package bio.terra.tanagra.query.sql.translator;

import bio.terra.tanagra.api.field.AttributeField;
import bio.terra.tanagra.api.field.EntityIdCountField;
import bio.terra.tanagra.api.field.HierarchyIsMemberField;
import bio.terra.tanagra.api.field.HierarchyIsRootField;
import bio.terra.tanagra.api.field.HierarchyNumChildrenField;
import bio.terra.tanagra.api.field.HierarchyPathField;
import bio.terra.tanagra.api.field.RelatedEntityIdCountField;
import bio.terra.tanagra.api.field.ValueDisplayField;
import bio.terra.tanagra.api.filter.AttributeFilter;
import bio.terra.tanagra.api.filter.BooleanAndOrFilter;
import bio.terra.tanagra.api.filter.BooleanNotFilter;
import bio.terra.tanagra.api.filter.EntityFilter;
import bio.terra.tanagra.api.filter.HierarchyHasAncestorFilter;
import bio.terra.tanagra.api.filter.HierarchyHasParentFilter;
import bio.terra.tanagra.api.filter.HierarchyIsMemberFilter;
import bio.terra.tanagra.api.filter.HierarchyIsRootFilter;
import bio.terra.tanagra.api.filter.RelationshipFilter;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
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
    switch (orderByDirection) {
      case ASCENDING:
        return "ASC";
      case DESCENDING:
        return "DESC";
      default:
        throw new SystemException("Unknown order by direction: " + orderByDirection);
    }
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
    return functionWithCommaSeparatedArgsFilterSql(
        field, binaryOperatorTemplateSql(operator), List.of(value), tableAlias, sqlParams);
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
      default:
        throw new SystemException("Unknown function template: " + naryOperator);
    }
  }

  default String functionWithCommaSeparatedArgsFilterSql(
      SqlField field,
      String functionTemplate,
      List<Literal> values,
      @Nullable String tableAlias,
      SqlParams sqlParams) {
    List<String> valueParamNames = new ArrayList<>();
    values.stream().forEach(value -> valueParamNames.add(sqlParams.addParam("val", value)));
    Map<String, String> substitutorParams =
        Map.of(
            FUNCTION_TEMPLATE_FIELD_VAR, SqlQueryField.of(field).renderForWhere(tableAlias),
            FUNCTION_TEMPLATE_VALUES_VAR,
                valueParamNames.stream()
                    .map(valueParamName -> '@' + valueParamName)
                    .collect(Collectors.joining(",")));
    return StringSubstitutor.replace(functionTemplate, substitutorParams);
  }

  default String binaryOperatorTemplateSql(BinaryOperator operator) {
    return FUNCTION_TEMPLATE_FIELD_VAR_BRACES
        + ' '
        + binaryOperatorSql(operator)
        + ' '
        + FUNCTION_TEMPLATE_VALUES_VAR_BRACES;
  }

  default String binaryOperatorSql(BinaryOperator operator) {
    switch (operator) {
      case EQUALS:
        return "=";
      case NOT_EQUALS:
        return "!=";
      case GREATER_THAN:
        return ">";
      case LESS_THAN:
        return "<";
      case GREATER_THAN_OR_EQUAL:
        return ">=";
      case LESS_THAN_OR_EQUAL:
        return "<=";
      default:
        throw new SystemException("Unknown binary operator: " + operator);
    }
  }

  default String unaryOperatorTemplateSql(UnaryOperator operator) {
    switch (operator) {
      case IS_NULL:
        return FUNCTION_TEMPLATE_FIELD_VAR_BRACES + " IS NULL";
      case IS_NOT_NULL:
        return FUNCTION_TEMPLATE_FIELD_VAR_BRACES + " IS NOT NULL";
      case IS_EMPTY_STRING:
        return FUNCTION_TEMPLATE_FIELD_VAR_BRACES + " = ''";
      default:
        throw new SystemException("Unknown unary operator: " + operator);
    }
  }

  default String functionTemplateSql(NaryOperator naryOperator) {
    switch (naryOperator) {
      case IN:
        return FUNCTION_TEMPLATE_FIELD_VAR_BRACES
            + " IN ("
            + FUNCTION_TEMPLATE_VALUES_VAR_BRACES
            + ")";
      case NOT_IN:
        return FUNCTION_TEMPLATE_FIELD_VAR_BRACES
            + " NOT IN ("
            + FUNCTION_TEMPLATE_VALUES_VAR_BRACES
            + ")";
      default:
        throw new SystemException("Unknown function template: " + naryOperator);
    }
  }

  default String textSearchOperatorTemplateSql(TextSearchFilter.TextSearchOperator operator) {
    switch (operator) {
      case EXACT_MATCH:
        return "REGEXP_CONTAINS(UPPER("
            + FUNCTION_TEMPLATE_FIELD_VAR_BRACES
            + "), UPPER("
            + FUNCTION_TEMPLATE_VALUES_VAR_BRACES
            + "))";
      case FUZZY_MATCH:
        throw new InvalidQueryException("Fuzzy text match not supported");
      default:
        throw new SystemException("Unknown text search operator: " + operator);
    }
  }

  @SuppressWarnings("checkstyle:ParameterNumber")
  default String inSelectFilterSql(
      SqlField whereField,
      @Nullable String tableAlias,
      SqlField selectField,
      SqlTable table,
      @Nullable String filterSql,
      @Nullable String havingSql,
      SqlParams sqlParams,
      Literal... unionAllLiterals) {
    List<String> selectSqls = new ArrayList<>();
    selectSqls.add(
        "SELECT "
            + SqlQueryField.of(selectField).renderForSelect()
            + " FROM "
            + table.render()
            + (filterSql != null ? " WHERE " + filterSql : "")
            + (havingSql != null ? ' ' + havingSql : ""));
    Arrays.stream(unionAllLiterals)
        .forEach(literal -> selectSqls.add("SELECT @" + sqlParams.addParam("val", literal)));
    return SqlQueryField.of(whereField).renderForWhere(tableAlias)
        + " IN ("
        + selectSqls.stream().collect(Collectors.joining(" UNION ALL "))
        + ')';
  }

  default String booleanAndOrFilterSql(
      BooleanAndOrFilter.LogicalOperator operator, String... subFilterSqls) {
    return Arrays.stream(subFilterSqls)
        .map(subFilterSql -> '(' + subFilterSql + ')')
        .collect(Collectors.joining(' ' + logicalOperatorSql(operator) + ' '));
  }

  default String booleanNotFilterSql(String subFilterSql) {
    return "NOT " + subFilterSql;
  }

  default String logicalOperatorSql(BooleanAndOrFilter.LogicalOperator operator) {
    switch (operator) {
      case AND:
        return "AND";
      case OR:
        return "OR";
      default:
        throw new SystemException("Unknown logical operator: " + operator);
    }
  }

  default String havingSql(
      BinaryOperator groupByOperator,
      Integer groupByCount,
      List<SqlField> groupByFields,
      @Nullable String tableAlias,
      SqlParams sqlParams) {
    sqlParams.addParam("groupByCount", Literal.forInt64(Long.valueOf(groupByCount)));
    return "GROUP BY "
        + groupByFields.stream()
            .map(groupByField -> SqlQueryField.of(groupByField).renderForWhere(tableAlias))
            .collect(Collectors.joining(","))
        + " HAVING COUNT(*) "
        + binaryOperatorSql(groupByOperator)
        + " @groupByCount";
  }

  ApiFieldTranslator translator(AttributeField attributeField);

  ApiFieldTranslator translator(EntityIdCountField entityIdCountField);

  ApiFieldTranslator translator(HierarchyIsMemberField hierarchyIsMemberField);

  ApiFieldTranslator translator(HierarchyIsRootField hierarchyIsRootField);

  ApiFieldTranslator translator(HierarchyNumChildrenField hierarchyNumChildrenField);

  ApiFieldTranslator translator(HierarchyPathField hierarchyPathField);

  ApiFieldTranslator translator(RelatedEntityIdCountField relatedEntityIdCountField);

  default ApiFieldTranslator translator(ValueDisplayField valueDisplayField) {
    if (valueDisplayField instanceof AttributeField) {
      return translator((AttributeField) valueDisplayField);
    } else if (valueDisplayField instanceof EntityIdCountField) {
      return translator((EntityIdCountField) valueDisplayField);
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

  ApiFilterTranslator translator(AttributeFilter attributeFilter);

  default ApiFilterTranslator translator(BooleanAndOrFilter booleanAndOrFilter) {
    return new BooleanAndOrFilterTranslator(this, booleanAndOrFilter);
  }

  default ApiFilterTranslator translator(BooleanNotFilter booleanNotFilter) {
    return new BooleanNotFilterTranslator(this, booleanNotFilter);
  }

  ApiFilterTranslator translator(HierarchyHasAncestorFilter hierarchyHasAncestorFilter);

  ApiFilterTranslator translator(HierarchyHasParentFilter hierarchyHasParentFilter);

  ApiFilterTranslator translator(HierarchyIsMemberFilter hierarchyIsMemberFilter);

  ApiFilterTranslator translator(HierarchyIsRootFilter hierarchyIsRootFilter);

  ApiFilterTranslator translator(RelationshipFilter relationshipFilter);

  ApiFilterTranslator translator(TextSearchFilter textSearchFilter);

  default ApiFilterTranslator translator(EntityFilter entityFilter) {
    if (entityFilter instanceof AttributeFilter) {
      return translator((AttributeFilter) entityFilter);
    } else if (entityFilter instanceof BooleanAndOrFilter) {
      return translator((BooleanAndOrFilter) entityFilter);
    } else if (entityFilter instanceof BooleanNotFilter) {
      return translator((BooleanNotFilter) entityFilter);
    } else if (entityFilter instanceof HierarchyHasAncestorFilter) {
      return translator((HierarchyHasAncestorFilter) entityFilter);
    } else if (entityFilter instanceof HierarchyHasParentFilter) {
      return translator((HierarchyHasParentFilter) entityFilter);
    } else if (entityFilter instanceof HierarchyIsMemberFilter) {
      return translator((HierarchyIsMemberFilter) entityFilter);
    } else if (entityFilter instanceof HierarchyIsRootFilter) {
      return translator((HierarchyIsRootFilter) entityFilter);
    } else if (entityFilter instanceof RelationshipFilter) {
      return translator((RelationshipFilter) entityFilter);
    } else if (entityFilter instanceof TextSearchFilter) {
      return translator((TextSearchFilter) entityFilter);
    } else {
      throw new InvalidQueryException("No SQL translator defined for filter");
    }
  }
}

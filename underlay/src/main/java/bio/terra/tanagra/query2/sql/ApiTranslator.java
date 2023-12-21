package bio.terra.tanagra.query2.sql;

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
import bio.terra.tanagra.api.shared.FunctionTemplate;
import bio.terra.tanagra.api.shared.Literal;
import bio.terra.tanagra.api.shared.LogicalOperator;
import bio.terra.tanagra.api.shared.OrderByDirection;
import bio.terra.tanagra.exception.InvalidQueryException;
import bio.terra.tanagra.exception.SystemException;
import bio.terra.tanagra.query2.sql.filtertranslator.BooleanAndOrFilterTranslator;
import bio.terra.tanagra.query2.sql.filtertranslator.BooleanNotFilterTranslator;
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

  default String selectSql(SqlQueryField sqlQueryField) {
    return fieldSql(sqlQueryField, null, false);
  }

  default String selectSql(SqlQueryField sqlQueryField, @Nullable String tableAlias) {
    return fieldSql(sqlQueryField, tableAlias, false);
  }

  default String whereSql(SqlField field, @Nullable String tableAlias) {
    return fieldSql(SqlQueryField.of(field), tableAlias, false);
  }

  default String orderBySql(
      SqlQueryField sqlQueryField, @Nullable String tableAlias, boolean fieldIsSelected) {
    if (fieldIsSelected) {
      String alias = sqlQueryField.getAlias();
      return alias == null || alias.isEmpty() ? sqlQueryField.getField().getColumnName() : alias;
    } else {
      return fieldSql(sqlQueryField, tableAlias, true);
    }
  }

  default String orderByRandSql() {
    return "RAND()";
  }

  default String groupBySql(
      SqlQueryField sqlQueryField, @Nullable String tableAlias, boolean fieldIsSelected) {
    if (fieldIsSelected) {
      String alias = sqlQueryField.getAlias();
      return alias == null || alias.isEmpty() ? sqlQueryField.getField().getColumnName() : alias;
    } else {
      return fieldSql(sqlQueryField, tableAlias, true);
    }
  }

  default String fieldSql(
      SqlQueryField sqlQueryField, @Nullable String tableAlias, boolean isForOrderOrGroupBy) {
    SqlField field = sqlQueryField.getField();
    String alias = sqlQueryField.getAlias();
    String baseFieldSql =
        tableAlias == null ? field.getColumnName() : (tableAlias + '.' + field.getColumnName());
    if (!field.hasFunctionWrapper()) {
      if (field.getColumnName().equals(alias)) {
        alias = null;
      }
    } else if (field.getFunctionWrapper().contains(FUNCTION_TEMPLATE_FIELD_VAR_BRACES)) {
      baseFieldSql =
          StringSubstitutor.replace(
              field.getFunctionWrapper(), Map.of(FUNCTION_TEMPLATE_FIELD_VAR, baseFieldSql));
    } else {
      baseFieldSql = field.getFunctionWrapper() + '(' + baseFieldSql + ')';
    }
    return isForOrderOrGroupBy || alias == null || alias.isEmpty()
        ? baseFieldSql
        : (baseFieldSql + " AS " + alias);
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

  default String binaryFilterSql(
      SqlField field,
      BinaryOperator operator,
      Literal value,
      @Nullable String tableAlias,
      SqlParams sqlParams) {
    return functionFilterSql(
        field,
        FUNCTION_TEMPLATE_FIELD_VAR_BRACES
            + ' '
            + binaryOperatorSql(operator)
            + ' '
            + FUNCTION_TEMPLATE_VALUES_VAR_BRACES,
        List.of(value),
        tableAlias,
        sqlParams);
  }

  default String functionFilterSql(
      SqlField field,
      String functionTemplate,
      List<Literal> values,
      @Nullable String tableAlias,
      SqlParams sqlParams) {
    List<String> valueParamNames = new ArrayList<>();
    values.stream().forEach(value -> valueParamNames.add(sqlParams.addParam("val", value)));
    Map<String, String> substitutorParams =
        Map.of(
            FUNCTION_TEMPLATE_FIELD_VAR, whereSql(field, tableAlias),
            FUNCTION_TEMPLATE_VALUES_VAR,
                valueParamNames.stream()
                    .map(valueParamName -> '@' + valueParamName)
                    .collect(Collectors.joining(",")));
    return StringSubstitutor.replace(functionTemplate, substitutorParams);
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
      case IS:
        return "IS";
      case IS_NOT:
        return "IS NOT";
      default:
        throw new SystemException("Unknown binary operator: " + operator);
    }
  }

  default String inSelectFilterSql(
      SqlField whereField,
      @Nullable String tableAlias,
      SqlField selectField,
      SqlTable table,
      String filterSql,
      SqlParams sqlParams,
      Literal... unionAllLiterals) {
    List<String> selectSqls = new ArrayList<>();
    selectSqls.add(
        "SELECT "
            + selectSql(SqlQueryField.of(selectField))
            + " FROM "
            + table.renderSQL()
            + " WHERE "
            + filterSql);
    Arrays.stream(unionAllLiterals)
        .forEach(literal -> selectSqls.add("SELECT @" + sqlParams.addParam("val", literal)));
    return whereSql(whereField, tableAlias)
        + " IN ("
        + selectSqls.stream().collect(Collectors.joining(" UNION ALL "))
        + ')';
  }

  default String booleanAndOrFilterSql(LogicalOperator operator, String... subFilterSqls) {
    return Arrays.stream(subFilterSqls)
        .collect(Collectors.joining(' ' + logicalOperatorSql(operator) + ' '));
  }

  default String booleanNotFilterSql(String subFilterSql) {
    return "NOT " + subFilterSql;
  }

  default String logicalOperatorSql(LogicalOperator operator) {
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
    sqlParams.addParam("groupByCount", new Literal(groupByCount));
    return "GROUP BY "
        + groupByFields.stream()
            .map(groupByField -> whereSql(groupByField, tableAlias))
            .collect(Collectors.joining(","))
        + " HAVING COUNT(*) "
        + binaryOperatorSql(groupByOperator)
        + " @groupByCount";
  }

  default String functionTemplateSql(FunctionTemplate functionTemplate) {
    switch (functionTemplate) {
      case TEXT_EXACT_MATCH:
        return "REGEXP_CONTAINS(UPPER("
            + FUNCTION_TEMPLATE_FIELD_VAR_BRACES
            + "), UPPER("
            + FUNCTION_TEMPLATE_VALUES_VAR_BRACES
            + "))";
      case TEXT_FUZZY_MATCH:
        throw new InvalidQueryException("Fuzzy text match not supported");
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
      case IS_NULL:
        return FUNCTION_TEMPLATE_FIELD_VAR_BRACES + " IS NULL";
      case IS_NOT_NULL:
        return FUNCTION_TEMPLATE_FIELD_VAR_BRACES + " IS NOT NULL";
      case IS_EMPTY_STRING:
        return FUNCTION_TEMPLATE_FIELD_VAR_BRACES + " = ''";
      default:
        throw new SystemException("Unknown function template: " + functionTemplate);
    }
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

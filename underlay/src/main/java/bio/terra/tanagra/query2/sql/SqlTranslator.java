package bio.terra.tanagra.query2.sql;

import bio.terra.tanagra.api.field.valuedisplay.AttributeField;
import bio.terra.tanagra.api.field.valuedisplay.EntityIdCountField;
import bio.terra.tanagra.api.field.valuedisplay.HierarchyIsMemberField;
import bio.terra.tanagra.api.field.valuedisplay.HierarchyIsRootField;
import bio.terra.tanagra.api.field.valuedisplay.HierarchyNumChildrenField;
import bio.terra.tanagra.api.field.valuedisplay.HierarchyPathField;
import bio.terra.tanagra.api.field.valuedisplay.RelatedEntityIdCountField;
import bio.terra.tanagra.api.field.valuedisplay.ValueDisplayField;
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
import bio.terra.tanagra.exception.InvalidQueryException;
import bio.terra.tanagra.exception.SystemException;
import bio.terra.tanagra.query.FieldPointer;
import bio.terra.tanagra.query.FunctionTemplate;
import bio.terra.tanagra.query.Literal;
import bio.terra.tanagra.query.OrderByDirection;
import bio.terra.tanagra.query.TablePointer;
import bio.terra.tanagra.query.filtervariable.BinaryFilterVariable;
import bio.terra.tanagra.query.filtervariable.BooleanAndOrFilterVariable;
import bio.terra.tanagra.query2.sql.filtertranslator.BooleanAndOrFilterTranslator;
import bio.terra.tanagra.query2.sql.filtertranslator.BooleanNotFilterTranslator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.apache.commons.text.StringSubstitutor;

public interface SqlTranslator {
  String FUNCTION_TEMPLATE_FIELD_VAR = "fieldSql";
  String FUNCTION_TEMPLATE_FIELD_VAR_BRACES = "${" + FUNCTION_TEMPLATE_FIELD_VAR + "}";

  String FUNCTION_TEMPLATE_VALUES_VAR = "values";
  String FUNCTION_TEMPLATE_VALUES_VAR_BRACES = "${" + FUNCTION_TEMPLATE_VALUES_VAR + "}";

  default String selectSql(SqlField sqlField, @Nullable String tableAlias) {
    return fieldSql(sqlField, tableAlias, false);
  }

  default String whereSql(FieldPointer field, @Nullable String tableAlias) {
    return fieldSql(SqlField.of(field, null), tableAlias, false);
  }

  default String orderBySql(
      SqlField sqlField, @Nullable String tableAlias, boolean fieldIsSelected) {
    if (fieldIsSelected) {
      String alias = sqlField.getAlias();
      return alias == null || alias.isEmpty() ? sqlField.getField().getColumnName() : alias;
    } else {
      return fieldSql(sqlField, tableAlias, true);
    }
  }

  default String orderByRandSql() {
    return "RAND()";
  }

  default String groupBySql(
      SqlField sqlField, @Nullable String tableAlias, boolean fieldIsSelected) {
    if (fieldIsSelected) {
      String alias = sqlField.getAlias();
      return alias == null || alias.isEmpty() ? sqlField.getField().getColumnName() : alias;
    } else {
      return fieldSql(sqlField, tableAlias, true);
    }
  }

  default String fieldSql(
      SqlField sqlField, @Nullable String tableAlias, boolean isForOrderOrGroupBy) {
    FieldPointer field = sqlField.getField();
    String alias = sqlField.getAlias();
    String baseFieldSql =
        tableAlias == null ? field.getColumnName() : (tableAlias + '.' + field.getColumnName());
    if (!field.hasSqlFunctionWrapper()) {
      if (field.getColumnName().equals(alias)) {
        alias = null;
      }
    } else if (field.getSqlFunctionWrapper().contains(FUNCTION_TEMPLATE_FIELD_VAR_BRACES)) {
      baseFieldSql =
          StringSubstitutor.replace(
              field.getSqlFunctionWrapper(), Map.of(FUNCTION_TEMPLATE_FIELD_VAR, baseFieldSql));
    } else {
      baseFieldSql = field.getSqlFunctionWrapper() + '(' + baseFieldSql + ')';
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
      FieldPointer field,
      BinaryFilterVariable.BinaryOperator operator,
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
      FieldPointer field,
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

  default String binaryOperatorSql(BinaryFilterVariable.BinaryOperator operator) {
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
      FieldPointer whereField,
      @Nullable String tableAlias,
      FieldPointer selectField,
      TablePointer table,
      String filterSql,
      SqlParams sqlParams,
      Literal... unionAllLiterals) {
    List<String> selectSqls = new ArrayList<>();
    selectSqls.add(
        "SELECT "
            + selectSql(SqlField.of(selectField, null), null)
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

  default String booleanAndOrFilterSql(
      BooleanAndOrFilterVariable.LogicalOperator operator, String... subFilterSqls) {
    return Arrays.stream(subFilterSqls)
        .collect(Collectors.joining(' ' + logicalOperatorSql(operator) + ' '));
  }

  default String booleanNotFilterSql(String subFilterSql) {
    return "NOT " + subFilterSql;
  }

  default String logicalOperatorSql(BooleanAndOrFilterVariable.LogicalOperator operator) {
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
      BinaryFilterVariable.BinaryOperator groupByOperator,
      Integer groupByCount,
      List<FieldPointer> groupByFields,
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

  SqlFieldTranslator translator(AttributeField attributeField);

  SqlFieldTranslator translator(EntityIdCountField entityIdCountField);

  SqlFieldTranslator translator(HierarchyIsMemberField hierarchyIsMemberField);

  SqlFieldTranslator translator(HierarchyIsRootField hierarchyIsRootField);

  SqlFieldTranslator translator(HierarchyNumChildrenField hierarchyNumChildrenField);

  SqlFieldTranslator translator(HierarchyPathField hierarchyPathField);

  SqlFieldTranslator translator(RelatedEntityIdCountField relatedEntityIdCountField);

  default SqlFieldTranslator translator(ValueDisplayField valueDisplayField) {
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

  SqlFilterTranslator translator(AttributeFilter attributeFilter);

  default SqlFilterTranslator translator(BooleanAndOrFilter booleanAndOrFilter) {
    return new BooleanAndOrFilterTranslator(this, booleanAndOrFilter);
  }

  default SqlFilterTranslator translator(BooleanNotFilter booleanNotFilter) {
    return new BooleanNotFilterTranslator(this, booleanNotFilter);
  }

  SqlFilterTranslator translator(HierarchyHasAncestorFilter hierarchyHasAncestorFilter);

  SqlFilterTranslator translator(HierarchyHasParentFilter hierarchyHasParentFilter);

  SqlFilterTranslator translator(HierarchyIsMemberFilter hierarchyIsMemberFilter);

  SqlFilterTranslator translator(HierarchyIsRootFilter hierarchyIsRootFilter);

  SqlFilterTranslator translator(RelationshipFilter relationshipFilter);

  SqlFilterTranslator translator(TextSearchFilter textSearchFilter);

  default SqlFilterTranslator translator(EntityFilter entityFilter) {
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

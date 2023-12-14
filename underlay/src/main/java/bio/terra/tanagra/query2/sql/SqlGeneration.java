package bio.terra.tanagra.query2.sql;

import bio.terra.tanagra.exception.SystemException;
import bio.terra.tanagra.query.FieldPointer;
import bio.terra.tanagra.query.Literal;
import bio.terra.tanagra.query.OrderByDirection;
import bio.terra.tanagra.query.TablePointer;
import bio.terra.tanagra.query.filtervariable.BinaryFilterVariable;
import bio.terra.tanagra.query.filtervariable.BooleanAndOrFilterVariable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.text.StringSubstitutor;

public final class SqlGeneration {
  public static final String FUNCTION_TEMPLATE_FIELD_VAR = "fieldSql";
  public static final String FUNCTION_TEMPLATE_FIELD_VAR_BRACES =
      "${" + FUNCTION_TEMPLATE_FIELD_VAR + "}";

  public static final String FUNCTION_TEMPLATE_VALUES_VAR = "values";
  public static final String FUNCTION_TEMPLATE_VALUES_VAR_BRACES =
      "${" + FUNCTION_TEMPLATE_VALUES_VAR + "}";

  private SqlGeneration() {}

  public static String selectSql(
      Pair<FieldPointer, String> fieldAndAlias, @Nullable String tableAlias) {
    return fieldSql(fieldAndAlias, tableAlias, false);
  }

  public static String whereSql(FieldPointer field, @Nullable String tableAlias) {
    return fieldSql(Pair.of(field, null), tableAlias, false);
  }

  public static String orderBySql(
      Pair<FieldPointer, String> fieldAndAlias,
      @Nullable String tableAlias,
      boolean fieldIsSelected) {
    if (fieldIsSelected) {
      String alias = fieldAndAlias.getRight();
      return alias == null || alias.isEmpty() ? fieldAndAlias.getLeft().getColumnName() : alias;
    } else {
      return fieldSql(fieldAndAlias, tableAlias, true);
    }
  }

  public static String groupBySql(
      Pair<FieldPointer, String> fieldAndAlias,
      @Nullable String tableAlias,
      boolean fieldIsSelected) {
    if (fieldIsSelected) {
      String alias = fieldAndAlias.getRight();
      return alias == null || alias.isEmpty() ? fieldAndAlias.getLeft().getColumnName() : alias;
    } else {
      return fieldSql(fieldAndAlias, tableAlias, true);
    }
  }

  private static String fieldSql(
      Pair<FieldPointer, String> fieldAndAlias,
      @Nullable String tableAlias,
      boolean isForOrderOrGroupBy) {
    FieldPointer field = fieldAndAlias.getLeft();
    String alias = fieldAndAlias.getRight();
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

  public static String orderByDirectionSql(OrderByDirection orderByDirection) {
    switch (orderByDirection) {
      case ASCENDING:
        return "ASC";
      case DESCENDING:
        return "DESC";
      default:
        throw new SystemException("Unknown order by direction: " + orderByDirection);
    }
  }

  public static String binaryFilterSql(
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

  public static String functionFilterSql(
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

  private static String binaryOperatorSql(BinaryFilterVariable.BinaryOperator operator) {
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

  public static String inSelectFilterSql(
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
            + selectSql(Pair.of(selectField, null), null)
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

  public static String booleanAndOrFilterSql(
      BooleanAndOrFilterVariable.LogicalOperator operator, String... subFilterSqls) {
    return Arrays.stream(subFilterSqls)
        .collect(Collectors.joining(' ' + logicalOperatorSql(operator) + ' '));
  }

  public static String booleanNotFilterSql(String subFilterSql) {
    return "NOT " + subFilterSql;
  }

  private static String logicalOperatorSql(BooleanAndOrFilterVariable.LogicalOperator operator) {
    switch (operator) {
      case AND:
        return "AND";
      case OR:
        return "OR";
      default:
        throw new SystemException("Unknown logical operator: " + operator);
    }
  }

  public static String havingSql(
      BinaryFilterVariable.BinaryOperator groupByOperator,
      Integer groupByCount,
      FieldPointer groupByField,
      @Nullable String tableAlias,
      SqlParams sqlParams) {
    sqlParams.addParam("groupByCount", new Literal(groupByCount));
    return "GROUP BY "
        + whereSql(groupByField, tableAlias)
        + " HAVING COUNT(*) "
        + binaryOperatorSql(groupByOperator)
        + " @groupByCount";
  }
}

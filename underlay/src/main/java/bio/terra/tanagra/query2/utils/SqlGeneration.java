package bio.terra.tanagra.query2.utils;

import bio.terra.tanagra.exception.SystemException;
import bio.terra.tanagra.query.FieldPointer;
import bio.terra.tanagra.query.OrderByDirection;
import java.util.Map;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.text.StringSubstitutor;

public final class SqlGeneration {
  // TODO: Move these constants somewhere in the underlay package, since they are part of the
  // underlay config input.
  public static final String FUNCTION_WRAPPER_SUBSTITUTION_VAR = "fieldSql";
  public static final String FUNCTION_WRAPPER_SUBSTITUTION_VAR_BRACES =
      "${" + FUNCTION_WRAPPER_SUBSTITUTION_VAR + "}";

  private SqlGeneration() {}

  public static String selectSql(Pair<FieldPointer, String> fieldAndAlias, String tableAlias) {
    return sql(fieldAndAlias, tableAlias, false);
  }

  public static String orderBySql(
      Pair<FieldPointer, String> fieldAndAlias, String tableAlias, boolean fieldIsSelected) {
    if (fieldIsSelected) {
      String alias = fieldAndAlias.getRight();
      return alias == null || alias.isEmpty() ? fieldAndAlias.getLeft().getColumnName() : alias;
    } else {
      return sql(fieldAndAlias, tableAlias, true);
    }
  }

  private static String sql(
      Pair<FieldPointer, String> fieldAndAlias, String tableAlias, boolean isForOrderBy) {
    FieldPointer field = fieldAndAlias.getLeft();
    String alias = fieldAndAlias.getRight();
    String baseFieldSql = tableAlias + '.' + field.getColumnName();
    if (!field.hasSqlFunctionWrapper()) {
      if (field.getColumnName().equals(alias)) {
        alias = null;
      }
    } else if (field.getSqlFunctionWrapper().contains(FUNCTION_WRAPPER_SUBSTITUTION_VAR_BRACES)) {
      baseFieldSql =
          StringSubstitutor.replace(
              field.getSqlFunctionWrapper(),
              Map.of(FUNCTION_WRAPPER_SUBSTITUTION_VAR, baseFieldSql));
    } else {
      baseFieldSql = field.getSqlFunctionWrapper() + '(' + baseFieldSql + ')';
    }
    return isForOrderBy || alias == null || alias.isEmpty()
        ? baseFieldSql
        : (baseFieldSql + " AS " + alias);
  }

  public static String sql(OrderByDirection orderByDirection) {
    switch (orderByDirection) {
      case ASCENDING:
        return "ASC";
      case DESCENDING:
        return "DESC";
      default:
        throw new SystemException("Unknown order by direction");
    }
  }
}

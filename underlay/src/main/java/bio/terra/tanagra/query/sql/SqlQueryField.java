package bio.terra.tanagra.query.sql;

import bio.terra.tanagra.query.sql.translator.ApiTranslator;
import java.util.Map;
import javax.annotation.Nullable;
import org.apache.commons.text.StringSubstitutor;

public final class SqlQueryField {
  private final SqlField field;
  private final String alias;

  private SqlQueryField(SqlField field, String alias) {
    this.field = field;
    this.alias = alias;
  }

  public static SqlQueryField of(SqlField field, String alias) {
    return new SqlQueryField(field, alias);
  }

  public static SqlQueryField of(SqlField field) {
    return of(field, null);
  }

  public SqlField getField() {
    return field;
  }

  public String getAlias() {
    return alias;
  }

  public String render(@Nullable String tableAlias, boolean isForOrderOrGroupBy) {
    SqlField field = getField();
    String alias = getAlias();
    String baseFieldSql =
        tableAlias == null ? field.getColumnName() : (tableAlias + '.' + field.getColumnName());
    if (!field.hasFunctionWrapper()) {
      if (field.getColumnName().equals(alias)) {
        alias = null;
      }
    } else if (field
        .getFunctionWrapper()
        .contains(ApiTranslator.FUNCTION_TEMPLATE_FIELD_VAR_BRACES)) {
      baseFieldSql =
          StringSubstitutor.replace(
              field.getFunctionWrapper(),
              Map.of(ApiTranslator.FUNCTION_TEMPLATE_FIELD_VAR, baseFieldSql));
    } else {
      baseFieldSql = field.getFunctionWrapper() + '(' + baseFieldSql + ')';
    }
    return isForOrderOrGroupBy || alias == null || alias.isEmpty()
        ? baseFieldSql
        : (baseFieldSql + " AS " + alias);
  }

  public String renderForSelect(@Nullable String tableAlias) {
    return render(tableAlias, false);
  }

  public String renderForSelect() {
    return renderForSelect(null);
  }

  public String renderForWhere(@Nullable String tableAlias) {
    return render(tableAlias, false);
  }

  public String renderForOrderBy(@Nullable String tableAlias, boolean fieldIsSelected) {
    if (fieldIsSelected) {
      String alias = getAlias();
      return alias == null || alias.isEmpty() ? getField().getColumnName() : alias;
    } else {
      return render(tableAlias, true);
    }
  }

  public String renderForGroupBy(@Nullable String tableAlias, boolean fieldIsSelected) {
    if (fieldIsSelected) {
      String alias = getAlias();
      return alias == null || alias.isEmpty() ? getField().getColumnName() : alias;
    } else {
      return render(tableAlias, true);
    }
  }
}

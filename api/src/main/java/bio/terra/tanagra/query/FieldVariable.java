package bio.terra.tanagra.query;

import bio.terra.tanagra.underlay.FieldPointer;
import com.google.common.collect.ImmutableMap;
import java.util.Map;
import org.apache.commons.text.StringSubstitutor;

public class FieldVariable implements SQLExpression {
  private final FieldPointer fieldPointer;
  private final TableVariable tableVariable;
  private String alias;

  public FieldVariable(FieldPointer fieldPointer, TableVariable tableVariable) {
    this.fieldPointer = fieldPointer;
    this.tableVariable = tableVariable;
  }

  public FieldVariable(FieldPointer fieldPointer, TableVariable tableVariable, String alias) {
    this.fieldPointer = fieldPointer;
    this.tableVariable = tableVariable;
    this.alias = alias;
  }

  @Override
  public String renderSQL() {
    return renderSQL(true);
  }

  public String renderSqlForOrderBy() {
    return renderSQL(false);
  }

  private String renderSQL(boolean useAliasAndFunctionWrapper) {
    String template = "${tableAlias}.${columnName}";
    Map<String, String> params =
        ImmutableMap.<String, String>builder()
            .put("tableAlias", tableVariable.getAlias())
            .put("columnName", fieldPointer.getColumnName())
            .build();
    String sql = StringSubstitutor.replace(template, params);

    if (fieldPointer.isForeignKey()) {
      throw new UnsupportedOperationException("TODO: implement embedded selects " + sql);
    }

    if (fieldPointer.hasSqlFunctionWrapper() && useAliasAndFunctionWrapper) {
      final String substitutionVar = "${fieldSql}";
      if (fieldPointer.getSqlFunctionWrapper().contains(substitutionVar)) {
        template = fieldPointer.getSqlFunctionWrapper();
        params = ImmutableMap.<String, String>builder().put("fieldSql", sql).build();
      } else {
        template = "${functionName}(${fieldSql})";
        params =
            ImmutableMap.<String, String>builder()
                .put("functionName", fieldPointer.getSqlFunctionWrapper())
                .put("fieldSql", sql)
                .build();
      }
      sql = StringSubstitutor.replace(template, params);
    }

    if (alias != null && useAliasAndFunctionWrapper) {
      template = "${fieldSql} AS ${fieldAlias}";
      params =
          ImmutableMap.<String, String>builder()
              .put("fieldSql", sql)
              .put("fieldAlias", alias)
              .build();
      sql = StringSubstitutor.replace(template, params);
    }

    return sql;
  }

  public String getAlias() {
    return alias == null ? "" : alias;
  }

  public FieldPointer getFieldPointer() {
    return fieldPointer;
  }
}

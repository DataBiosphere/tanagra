package bio.terra.tanagra.query;

public enum FunctionTemplate {
  TEXT_EXACT_MATCH("REGEXP_CONTAINS(UPPER(${fieldVariable}), UPPER(${value}))"),
  TEXT_FUZZY_MATCH("bqutil.fn.levenshtein(UPPER(${fieldVariable}), UPPER(${value}))<5"),
  IN("${fieldVariable} IN (${value})"),
  NOT_IN("${fieldVariable} NOT IN (${value})"),
  IS_NULL("${fieldVariable} IS NULL"),
  IS_NOT_NULL("${fieldVariable} IS NOT NULL"),
  IS_EMPTY_STRING("${fieldVariable} = ''");

  private String sqlTemplate;

  FunctionTemplate(String sqlTemplate) {
    this.sqlTemplate = sqlTemplate;
  }

  public String getSqlTemplate() {
    return sqlTemplate;
  }
}

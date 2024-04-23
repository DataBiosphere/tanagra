package bio.terra.tanagra.utils;

import org.hibernate.engine.jdbc.internal.BasicFormatterImpl;

public final class SqlFormatter {
  private SqlFormatter() {}

  public static String format(String sqlNoLineBreaks) {
    return sqlNoLineBreaks != null ? new BasicFormatterImpl().format(sqlNoLineBreaks) : null;
  }
}

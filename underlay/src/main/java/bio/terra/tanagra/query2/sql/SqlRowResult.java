package bio.terra.tanagra.query2.sql;

import bio.terra.tanagra.query.Literal;

public interface SqlRowResult {
  /** Get literal value for the column in this row. */
  Literal get(String columnName, Literal.DataType expectedDataType);

  /** Return the number of {@link bio.terra.tanagra.query.Literal}s in this row. */
  int size();
}

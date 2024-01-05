package bio.terra.tanagra.query.sql;

import bio.terra.tanagra.api.shared.DataType;
import bio.terra.tanagra.api.shared.Literal;

public interface SqlRowResult {
  /** Get literal value for the column in this row. */
  Literal get(String columnName, DataType expectedDataType);

  /** Return the number of {@link bio.terra.tanagra.api.shared.Literal}s in this row. */
  int size();
}

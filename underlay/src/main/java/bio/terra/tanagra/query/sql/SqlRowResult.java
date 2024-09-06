package bio.terra.tanagra.query.sql;

import bio.terra.tanagra.api.shared.DataType;
import bio.terra.tanagra.api.shared.Literal;
import java.util.*;

public interface SqlRowResult {
  /** Get literal value for the column in this row. */
  Literal get(String columnName, DataType expectedDataType);

  /** Get literal values for the repeated column in this row. */
  List<Literal> getRepeated(String columnName, DataType expectedDataType);

  /** Return the number of {@link bio.terra.tanagra.api.shared.Literal}s in this row. */
  int size();
}

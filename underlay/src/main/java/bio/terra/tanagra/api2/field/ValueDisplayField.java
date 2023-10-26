package bio.terra.tanagra.api2.field;

import bio.terra.tanagra.api2.query.ValueDisplay;
import bio.terra.tanagra.query.*;
import java.util.List;

public abstract class ValueDisplayField {
  public abstract List<FieldVariable> buildFieldVariables(
      TableVariable entityTableVar, List<TableVariable> tableVars);

  public abstract List<ColumnSchema> getColumnSchemas();

  public abstract ValueDisplay parseFromRowResult(RowResult rowResult);
}

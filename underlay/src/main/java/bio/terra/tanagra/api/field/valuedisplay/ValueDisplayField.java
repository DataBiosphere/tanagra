package bio.terra.tanagra.api.field.valuedisplay;

import bio.terra.tanagra.api.query.ValueDisplay;
import bio.terra.tanagra.query.ColumnSchema;
import bio.terra.tanagra.query.FieldVariable;
import bio.terra.tanagra.query.RowResult;
import bio.terra.tanagra.query.TableVariable;
import java.util.List;

public abstract class ValueDisplayField {
  public abstract List<FieldVariable> buildFieldVariables(
      TableVariable entityTableVar, List<TableVariable> tableVars);

  public abstract List<ColumnSchema> getColumnSchemas();

  public abstract ValueDisplay parseFromRowResult(RowResult rowResult);
}

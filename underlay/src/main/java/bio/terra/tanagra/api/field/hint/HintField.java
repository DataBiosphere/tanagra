package bio.terra.tanagra.api.field.hint;

import bio.terra.tanagra.api.query.hint.Hint;
import bio.terra.tanagra.exception.SystemException;
import bio.terra.tanagra.query.CellValue;
import bio.terra.tanagra.query.ColumnSchema;
import bio.terra.tanagra.query.FieldVariable;
import bio.terra.tanagra.query.Literal;
import bio.terra.tanagra.query.RowResult;
import bio.terra.tanagra.query.TableVariable;
import java.util.List;
import java.util.Optional;

public abstract class HintField {
  public abstract List<FieldVariable> buildFieldVariables(
      TableVariable entityTableVar, List<TableVariable> tableVars);

  public abstract List<ColumnSchema> getColumnSchemas();

  public abstract Hint parseFromRowResult(RowResult rowResult);

  protected Optional<Literal> getCellValueOrThrow(RowResult rowResult, String columnName) {
    CellValue cellValue = rowResult.get(columnName);
    if (cellValue == null) {
      throw new SystemException("Column not found: " + columnName);
    }
    return cellValue.getLiteral();
  }
}

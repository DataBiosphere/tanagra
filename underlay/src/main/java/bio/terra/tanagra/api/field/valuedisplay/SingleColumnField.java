package bio.terra.tanagra.api.field.valuedisplay;

import bio.terra.tanagra.api.query.ValueDisplay;
import bio.terra.tanagra.exception.SystemException;
import bio.terra.tanagra.query.CellValue;
import bio.terra.tanagra.query.ColumnSchema;
import bio.terra.tanagra.query.FieldPointer;
import bio.terra.tanagra.query.FieldVariable;
import bio.terra.tanagra.query.RowResult;
import bio.terra.tanagra.query.TableVariable;
import com.google.common.collect.ImmutableList;
import java.util.List;

public abstract class SingleColumnField extends ValueDisplayField {

  @Override
  public List<FieldVariable> buildFieldVariables(
      TableVariable entityTableVar, List<TableVariable> tableVars) {
    return List.of(new FieldVariable(getField(), entityTableVar, getFieldAlias()));
  }

  @Override
  public ImmutableList<ColumnSchema> getColumnSchemas() {
    return ImmutableList.of(getColumnSchema());
  }

  @Override
  public ValueDisplay parseFromRowResult(RowResult rowResult) {
    CellValue cellValue = rowResult.get(getFieldAlias());
    if (cellValue == null) {
      throw new SystemException("Column not found: " + getFieldAlias());
    }
    return cellValue.getLiteral().isEmpty() ? null : new ValueDisplay(cellValue.getLiteral().get());
  }

  protected abstract FieldPointer getField();

  public String getFieldAlias() {
    return getField().getColumnName();
  }

  protected abstract CellValue.SQLDataType getFieldDataType();

  public ColumnSchema getColumnSchema() {
    return new ColumnSchema(getFieldAlias(), getFieldDataType());
  }
}

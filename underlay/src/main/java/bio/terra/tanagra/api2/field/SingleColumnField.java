package bio.terra.tanagra.api2.field;

import bio.terra.tanagra.api2.query.ValueDisplay;
import bio.terra.tanagra.exception.SystemException;
import bio.terra.tanagra.query.*;
import bio.terra.tanagra.underlay2.Entity;
import com.google.common.collect.ImmutableList;
import java.util.List;

public abstract class SingleColumnField extends EntityField {
  protected SingleColumnField(Entity entity) {
    super(entity);
  }

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

  protected abstract String getFieldAlias();

  protected abstract CellValue.SQLDataType getFieldDataType();

  protected ColumnSchema getColumnSchema() {
    return new ColumnSchema(getFieldAlias(), getFieldDataType());
  }
}

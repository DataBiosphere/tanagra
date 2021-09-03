package bio.terra.tanagra.service.databaseaccess.bigquery;

import bio.terra.tanagra.service.databaseaccess.CellValue;
import bio.terra.tanagra.service.databaseaccess.ColumnSchema;
import bio.terra.tanagra.service.search.DataType;
import com.google.cloud.bigquery.FieldValue;

/** A {@link CellValue} for BigQuery's {@link FieldValue}. */
class BigQueryCellValue implements CellValue {
  private final FieldValue fieldValue;
  private final ColumnSchema columnSchema;

  BigQueryCellValue(FieldValue fieldValue, ColumnSchema columnSchema) {
    this.fieldValue = fieldValue;
    this.columnSchema = columnSchema;
  }

  @Override
  public DataType dataType() {
    return columnSchema.dataType();
  }

  @Override
  public boolean isNull() {
    return fieldValue.isNull();
  }

  @Override
  public long getLong() {
    return fieldValue.getLongValue();
  }

  @Override
  public String getString() {
    return fieldValue.getStringValue();
  }

  @Override
  public boolean getBoolean() {
    return fieldValue.getBooleanValue();
  }
}

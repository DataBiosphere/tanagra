package bio.terra.tanagra.service.databaseaccess.bigquery;

import bio.terra.tanagra.service.databaseaccess.CellValue;
import bio.terra.tanagra.service.databaseaccess.ColumnSchema;
import bio.terra.tanagra.service.search.DataType;
import com.google.cloud.bigquery.FieldValue;

/** A {@link CellValue} for BigQuery's {@link FieldValue}. */
@SuppressWarnings("PMD.PreserveStackTrace")
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
    try {
      return fieldValue.getLongValue();
    } catch (NumberFormatException e) {
      throw new ClassCastException("Unable to format as number");
    }
  }

  @Override
  public String getString() {
    // Don't allow the FieldValue to treat any primitive as a string value.
    if (!dataType().equals(DataType.STRING)) {
      throw new ClassCastException(String.format("DataType is %s, not a string.", dataType()));
    }
    return fieldValue.getStringValue();
  }
}

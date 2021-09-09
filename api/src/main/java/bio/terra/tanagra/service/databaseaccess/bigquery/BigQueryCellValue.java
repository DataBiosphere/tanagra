package bio.terra.tanagra.service.databaseaccess.bigquery;

import bio.terra.tanagra.service.databaseaccess.CellValue;
import bio.terra.tanagra.service.databaseaccess.ColumnSchema;
import bio.terra.tanagra.service.search.DataType;
import com.google.cloud.bigquery.FieldValue;
import java.util.Optional;
import java.util.OptionalLong;

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
  @SuppressWarnings("PMD.PreserveStackTrace")
  public OptionalLong getLong() {
    assertDataTypeIs(DataType.INT64);
    try {
      return fieldValue.isNull()
          ? OptionalLong.empty()
          : OptionalLong.of(fieldValue.getLongValue());
    } catch (NumberFormatException e) {
      throw new ClassCastException("Unable to format as number");
    }
  }

  @Override
  public Optional<String> getString() {
    assertDataTypeIs(DataType.STRING);
    return fieldValue.isNull() ? Optional.empty() : Optional.of(fieldValue.getStringValue());
  }

  /**
   * Checks that the {@link #dataType()} is what's expected, or else throws a {@link
   * ClassCastException}.
   */
  private void assertDataTypeIs(DataType expected) {
    if (!dataType().equals(expected)) {
      throw new ClassCastException(
          String.format("DataType is %s, not the expected %s", dataType(), expected));
    }
  }
}

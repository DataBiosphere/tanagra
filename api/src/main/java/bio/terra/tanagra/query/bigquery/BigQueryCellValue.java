package bio.terra.tanagra.query.bigquery;

import bio.terra.tanagra.exception.SystemException;
import bio.terra.tanagra.query.CellValue;
import bio.terra.tanagra.query.ColumnSchema;
import bio.terra.tanagra.query.Literal;
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
  public SQLDataType dataType() {
    return columnSchema.getSqlDataType();
  }

  @Override
  @SuppressWarnings("PMD.PreserveStackTrace")
  public OptionalLong getLong() {
    assertDataTypeIs(SQLDataType.INT64);
    try {
      return fieldValue.isNull()
          ? OptionalLong.empty()
          : OptionalLong.of(fieldValue.getLongValue());
    } catch (NumberFormatException nfEx) {
      throw new SystemException("Unable to format as number", nfEx);
    }
  }

  @Override
  public Optional<String> getString() {
    assertDataTypeIs(SQLDataType.STRING);
    return fieldValue.isNull() ? Optional.empty() : Optional.of(fieldValue.getStringValue());
  }

  @Override
  public Literal getLiteral() {
    Literal.DataType dataType = dataType().toUnderlayDataType();
    switch (dataType) {
      case INT64:
        return new Literal(fieldValue.getLongValue());
      case STRING:
        return new Literal(fieldValue.isNull() ? null : fieldValue.getStringValue());
      case BOOLEAN:
        return new Literal(fieldValue.getBooleanValue());
      case DATE:
        return Literal.forDate(fieldValue.getStringValue());
      case DOUBLE:
        return new Literal(fieldValue.getDoubleValue());
      default:
        throw new SystemException("Unknown data type: " + dataType);
    }
  }

  /**
   * Checks that the {@link #dataType()} is what's expected, or else throws a {@link
   * SystemException}.
   */
  private void assertDataTypeIs(SQLDataType expected) {
    if (!dataType().equals(expected)) {
      throw new SystemException(
          String.format("SQLDataType is %s, not the expected %s", dataType(), expected));
    }
  }
}

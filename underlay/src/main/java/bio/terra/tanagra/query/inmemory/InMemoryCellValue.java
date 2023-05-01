package bio.terra.tanagra.query.inmemory;

import bio.terra.tanagra.exception.SystemException;
import bio.terra.tanagra.query.CellValue;
import bio.terra.tanagra.query.ColumnSchema;
import bio.terra.tanagra.query.Literal;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalLong;

/** A {@link CellValue} for an {@link Object}. */
class InMemoryCellValue implements CellValue {
  private final Object value;
  private final ColumnSchema columnSchema;

  InMemoryCellValue(Object value, ColumnSchema columnSchema) {
    this.value = value;
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
      return value == null ? OptionalLong.empty() : OptionalLong.of((Long) value);
    } catch (NumberFormatException nfEx) {
      throw new SystemException("Unable to format as number", nfEx);
    }
  }

  @Override
  public Optional<String> getString() {
    assertDataTypeIs(SQLDataType.STRING);
    return value == null ? Optional.empty() : Optional.of((String) value);
  }

  @Override
  @SuppressWarnings("PMD.PreserveStackTrace")
  public OptionalDouble getDouble() {
    try {
      return value == null ? OptionalDouble.empty() : OptionalDouble.of((Double) value);
    } catch (NumberFormatException nfEx) {
      throw new SystemException("Unable to format as number", nfEx);
    }
  }

  @Override
  public Optional<Literal> getLiteral() {
    if (value == null) {
      return Optional.empty();
    }

    Literal.DataType dataType = dataType().toUnderlayDataType();
    switch (dataType) {
      case INT64:
        return Optional.of(new Literal((Long) value));
      case STRING:
        return Optional.of(new Literal(value == null ? null : (String) value));
      case BOOLEAN:
        return Optional.of(new Literal((Boolean) value));
      case DATE:
        return Optional.of(Literal.forDate(value.toString()));
      case DOUBLE:
        return Optional.of(new Literal((Double) value));
      default:
        throw new SystemException("Unknown data type: " + dataType);
    }
  }
}

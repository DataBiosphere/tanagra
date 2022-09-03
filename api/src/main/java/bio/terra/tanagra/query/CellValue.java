package bio.terra.tanagra.query;

import bio.terra.tanagra.underlay.Literal;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * An interface for the value of a cell within a row within a result table.
 *
 * <p>This interface allows us to read data from different databases in a simple but uniform way.
 * Different database types should implement this for returning values.
 */
public interface CellValue {
  /** Enum for the SQL data types supported by Tanagra. */
  enum SQLDataType {
    INT64,
    STRING,
    BOOLEAN;

    public static SQLDataType fromUnderlayDataType(Literal.DataType underlayDataType) {
      switch (underlayDataType) {
        case INT64:
          return INT64;
        case STRING:
          return STRING;
        case BOOLEAN:
          return BOOLEAN;
        default:
          throw new IllegalArgumentException("Unknown underlay data type: " + underlayDataType);
      }
    }

    public Literal.DataType toUnderlayDataType() {
      switch (this) {
        case INT64:
          return Literal.DataType.INT64;
        case STRING:
          return Literal.DataType.STRING;
        case BOOLEAN:
          return Literal.DataType.BOOLEAN;
        default:
          throw new IllegalArgumentException("Unknown SQL data type: " + this);
      }
    }
  }

  /** The type of data in this cell. */
  SQLDataType dataType();

  /**
   * Returns this field's value as a long or empty if the value is null.
   *
   * @throws ClassCastException if the cell's value is not a long
   */
  OptionalLong getLong();

  /**
   * Returns this field's value as a string or empty if the value is null.
   *
   * @throws ClassCastException if the cell's value is not a string
   */
  Optional<String> getString();

  Literal getLiteral();
}

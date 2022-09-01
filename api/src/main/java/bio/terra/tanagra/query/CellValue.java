package bio.terra.tanagra.query;

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
    BOOLEAN
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
}

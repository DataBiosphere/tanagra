package bio.terra.tanagra.service.databaseaccess;

import bio.terra.tanagra.service.search.DataType;

/**
 * An interface for the value of a cell within a row within a result table.
 *
 * <p>This interface allows us to read data from different databases in a simple but uniform way.
 * Different database types should implement this for returning values.
 */
public interface CellValue {

  /** The type of data in this cell. */
  DataType dataType();

  /** Returns whether the cell's value is null. */
  boolean isNull();

  /**
   * Returns this field's value as a long.
   *
   * @throws ClassCastException if the cell's value is not a long
   * @throws NullPointerException if {@link #isNull()} returns {@code true}
   */
  long getLong();

  /**
   * Returns this field's value as a string.
   *
   * @throws ClassCastException if the cell's value is not a string
   * @throws NullPointerException if {@link #isNull()} returns {@code true}
   */
  String getString();
}

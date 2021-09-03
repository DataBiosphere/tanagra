package bio.terra.tanagra.service.databaseaccess;

import bio.terra.tanagra.service.search.DataType;

/**
 * An interface for the value of a cell within a row within a result table.
 *
 * <p>This interface allows us to read data from different databases in a simple but uniform way.
 * Different database types should implement this for returning values.
 */
@SuppressWarnings("PMD.BooleanGetMethodName") // getBoolean is a better name than isBoolean.
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

  /**
   * Returns this field's value as a string.
   *
   * @throws ClassCastException if the cell's value is not a string
   * @throws NullPointerException if {@link #isNull()} returns {@code true}
   */
  boolean getBoolean();
}

// DO NOT SUBMIT
// For BQ pagination, use job id + next page token to retrieve job -> table results.
// For Postgres, we need to use offset + limit. That means encoding it in the original query
// construction as pagination,
// or rewriting the query during execution.
// Thought: Add pagination info as input to the Query as optional. For postgres specialization, use
// that to append the ORDER BY %s OFFSET %s LIMIT %s

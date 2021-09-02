package bio.terra.tanagra.service.underlay.access;

/** An interface for a row within a result table.
 * <p>This interface allows us to read data from different databases in a simple but uniform way.
 * Each supported database must implement this for returning rows. */
public interface RowResult {

  /** Get {@link CellValue} by index of the column in the table query. */
  // TODO consider a get by column name.
  CellValue get(int index);

  /** Returns the number of {@link CellValue}s in this row. */
  int size();
}

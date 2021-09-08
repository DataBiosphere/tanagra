package bio.terra.tanagra.service.databaseaccess.testing;

import bio.terra.tanagra.service.databaseaccess.CellValue;
import bio.terra.tanagra.service.databaseaccess.ColumnHeaderSchema;
import bio.terra.tanagra.service.databaseaccess.RowResult;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import java.util.List;

/** A {@link RowResult} for testing. */
public class TestRowResult implements RowResult {
  private final ColumnHeaderSchema columnHeaderSchema;
  private final ImmutableList<CellValue> cellValues;

  public TestRowResult(ColumnHeaderSchema columnHeaderSchema, List<CellValue> cellValues) {
    Preconditions.checkArgument(
        columnHeaderSchema.columnSchemas().size() == cellValues.size(),
        "Number of columns (%s) and number of cells (%d) must match.",
        columnHeaderSchema.columnSchemas().size(),
        cellValues.size());
    this.columnHeaderSchema = columnHeaderSchema;
    this.cellValues = ImmutableList.copyOf(cellValues);
  }

  @Override
  public CellValue get(int index) {
    return cellValues.get(index);
  }

  @Override
  public CellValue get(String columnName) {
    return get(columnHeaderSchema.getIndex(columnName));
  }

  @Override
  public int size() {
    return cellValues.size();
  }
}

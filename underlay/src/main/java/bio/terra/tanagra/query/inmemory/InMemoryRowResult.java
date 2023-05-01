package bio.terra.tanagra.query.inmemory;

import bio.terra.tanagra.query.CellValue;
import bio.terra.tanagra.query.ColumnHeaderSchema;
import bio.terra.tanagra.query.RowResult;
import com.google.api.client.util.Preconditions;
import java.util.List;

/** A {@link RowResult} for defining a row result with {@link Object}. */
public class InMemoryRowResult implements RowResult {
  private final List<Object> values;
  private final ColumnHeaderSchema columnHeaderSchema;

  public InMemoryRowResult(List<Object> values, ColumnHeaderSchema columnHeaderSchema) {
    Preconditions.checkArgument(
        values.size() == columnHeaderSchema.getColumnSchemas().size(),
        "Values size %d did not match column schemas size %d.",
        values.size(),
        columnHeaderSchema.getColumnSchemas().size());
    this.values = values;
    this.columnHeaderSchema = columnHeaderSchema;
  }

  @Override
  public CellValue get(int index) {
    return new InMemoryCellValue(
        values.get(index), columnHeaderSchema.getColumnSchemas().get(index));
  }

  @Override
  public CellValue get(String columnName) {
    int index = columnHeaderSchema.getIndex(columnName);
    return get(index);
  }

  @Override
  public int size() {
    return values.size();
  }
}

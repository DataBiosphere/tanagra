package bio.terra.tanagra.service.databaseaccess;

import com.google.auto.value.AutoValue;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import java.util.List;

/**
 * A {@link bio.terra.tanagra.service.databaseaccess.RowResult} where the {@link
 * bio.terra.tanagra.service.databaseaccess.CellValue}s are eagerly materialized.
 */
@AutoValue
public abstract class EagerRowResult implements RowResult {
  abstract ColumnHeaderSchema columnHeaderSchema();

  abstract ImmutableList<CellValue> cellValues();

  @Override
  public CellValue get(int index) {
    return cellValues().get(index);
  }

  @Override
  public CellValue get(String columnName) {
    return get(columnHeaderSchema().getIndex(columnName));
  }

  @Override
  public int size() {
    return cellValues().size();
  }

  public static Builder builder() {
    return new AutoValue_EagerRowResult.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder columnHeaderSchema(ColumnHeaderSchema columnHeaderSchema);

    public abstract ColumnHeaderSchema columnHeaderSchema();

    public abstract Builder cellValues(List<CellValue> cellValues);

    public abstract ImmutableList<CellValue> cellValues();

    abstract EagerRowResult autoBuild();

    public EagerRowResult build() {
      Preconditions.checkArgument(
          columnHeaderSchema().columnSchemas().size() == cellValues().size(),
          "Number of columns (%s) and number of cells (%d) must match.",
          columnHeaderSchema().columnSchemas().size(),
          cellValues().size());
      return autoBuild();
    }
  }
}

package bio.terra.tanagra.service.databaseaccess;

import com.google.auto.value.AutoValue;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import java.util.List;

/** The schema of the columns in {@link RowResult}s. */
@AutoValue
public abstract class ColumnHeaderSchema {

  /** The list of column schemas. Must match the order of the corresponding {@link RowResult}. */
  public abstract ImmutableList<ColumnSchema> columnSchemas();

  public int getIndex(String columnName) {
    for (int i = 0; i < columnSchemas().size(); ++i) {
      if (columnSchemas().get(i).name().equals(columnName)) {
        return i;
      }
    }
    throw new IllegalArgumentException(
        String.format("Column name '%s' not a part of the column schema.", columnName));
  }

  public static Builder builder() {
    return new AutoValue_ColumnHeaderSchema.Builder();
  }

  /** Builder for {@link ColumnHeaderSchema}. */
  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder columnSchemas(List<ColumnSchema> columnSchemas);

    public abstract ImmutableList<ColumnSchema> columnSchemas();

    public ColumnHeaderSchema build() {
      Preconditions.checkArgument(
          columnSchemas().stream().map(ColumnSchema::name).distinct().count()
              == columnSchemas().size(),
          "ColumnSchema names must all be distinct.");
      return autoBuild();
    }

    abstract ColumnHeaderSchema autoBuild();
  }
}

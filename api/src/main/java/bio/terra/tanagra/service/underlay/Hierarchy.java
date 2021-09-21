package bio.terra.tanagra.service.underlay;

import com.google.auto.value.AutoValue;
import com.google.common.base.Preconditions;

/**
 * The underlay configuration for attributes that are a part of a domain hierarchy.
 *
 * <p>This class represents the configuration of the mapping of an attribute hierarchy to tables.
 */
@AutoValue
public abstract class Hierarchy {
  public abstract DescendantsTable descendantsTable();
  // TODO add relationship table.

  public static Builder builder() {
    return new AutoValue_Hierarchy.Builder();
  }

  @AutoValue
  public abstract static class DescendantsTable {
    /** A column with an id of the ancestor in the hierarchy table. */
    public abstract Column ancestor();

    /**
     * A column with an array of the descendants of the ancestor column of the table.
     *
     * <p>Must have the same Table as {@link #ancestor()}.
     */
    // TODO support other descendants table layouts.
    public abstract Column descendants();

    /** The underlying {@link Table}. */
    public Table table() {
      return ancestor().table();
    }

    public static Builder builder() {
      return new AutoValue_Hierarchy_DescendantsTable.Builder();
    }

    /** Builder for {@link DescendantsTable}. */
    @AutoValue.Builder
    public abstract static class Builder {

      public abstract Builder ancestor(Column ancestor);

      public abstract Column ancestor();

      public abstract Builder descendants(Column descendants);

      public abstract Column descendants();

      public DescendantsTable build() {
        Preconditions.checkArgument(
            ancestor().table().equals(descendants().table()),
            "ancestor and descendants must share the same table, but found [%s] and [%s] tables",
            ancestor().table(),
            descendants().table());
        return autoBuild();
      }

      abstract DescendantsTable autoBuild();
    }
  }

  /** Builder for {@link Hierarchy}. */
  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder descendantsTable(DescendantsTable descendantsTable);

    public abstract Hierarchy build();
  }
}

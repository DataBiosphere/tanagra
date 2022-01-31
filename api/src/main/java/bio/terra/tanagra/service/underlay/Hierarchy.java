package bio.terra.tanagra.service.underlay;

import com.google.auto.value.AutoValue;
import com.google.common.base.Preconditions;
import javax.annotation.Nullable;

/**
 * The underlay configuration for attributes that are a part of a domain hierarchy.
 *
 * <p>This class represents the configuration of the mapping of an attribute hierarchy to tables.
 */
@AutoValue
public abstract class Hierarchy {
  public abstract DescendantsTable descendantsTable();
  // TODO add relationship table.

  public abstract ChildrenTable childrenTable();

  public abstract PathsTable pathsTable();

  public static Builder builder() {
    return new AutoValue_Hierarchy.Builder();
  }

  /**
   * The specification for a ancestor-descendants table for the hierarchy.
   *
   * <p>For each (ancestor, descendant) pair, there should be a row in the table referenced by the
   * {@link DescendantsTable}.
   */
  @AutoValue
  public abstract static class DescendantsTable {
    /** A column with an id of the ancestor in the hierarchy table. */
    public abstract Column ancestor();

    /**
     * A column with the descendant of the ancestor column of the table.
     *
     * <p>Must have the same Table as {@link #ancestor()}.
     */
    public abstract Column descendant();

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

      public abstract Builder descendant(Column descendants);

      public abstract Column descendant();

      public DescendantsTable build() {
        Preconditions.checkArgument(
            ancestor().table().equals(descendant().table()),
            "ancestor and descendants must share the same table, but found [%s] and [%s] tables",
            ancestor().table(),
            descendant().table());
        return autoBuild();
      }

      abstract DescendantsTable autoBuild();
    }
  }

  /**
   * The specification for a parent-child table for the hierarchy.
   *
   * <p>For each (parent, child) pair, there should be a row in the table referenced by the {@link
   * ChildrenTable}.
   */
  @AutoValue
  public abstract static class ChildrenTable {
    /** A column with an id of the parent in the hierarchy table. */
    public abstract Column parent();

    /**
     * A column with the immediate child of the parent column of the table.
     *
     * <p>Must have the same Table as {@link #parent()}.
     */
    public abstract Column child();

    @Nullable
    public abstract TableFilter tableFilter();

    /** The underlying {@link Table}. */
    public Table table() {
      return parent().table();
    }

    public static Builder builder() {
      return new AutoValue_Hierarchy_ChildrenTable.Builder();
    }

    /** Builder for {@link ChildrenTable}. */
    @AutoValue.Builder
    public abstract static class Builder {

      public abstract Builder parent(Column parent);

      public abstract Column parent();

      public abstract Builder child(Column child);

      public abstract Column child();

      public abstract Builder tableFilter(TableFilter tableFilter);

      public abstract TableFilter tableFilter();

      public ChildrenTable build() {
        Preconditions.checkArgument(
            parent().table().equals(child().table()),
            "parent and child must share the same table, but found [%s] and [%s] tables",
            parent().table(),
            child().table());
        if (tableFilter() != null) {
          Preconditions.checkArgument(
              parent().table().equals(child().table()),
              "parent column and table filter must share the same table, but found [%s] and [%s] tables",
              parent().table(),
              tableFilter().table());
        }
        return autoBuild();
      }

      abstract ChildrenTable autoBuild();
    }
  }

  /**
   * The specification for a node-path table for the hierarchy.
   *
   * <p>For each node in the hierarchy, there should be a (node, path) row in the table referenced
   * by the {@link PathsTable}.
   */
  @AutoValue
  public abstract static class PathsTable {
    /** A column with an id of the node in the hierarchy table. */
    public abstract Column node();

    /**
     * A column with the descendant of the path column of the table.
     *
     * <p>Must have the same Table as {@link #node()}.
     */
    public abstract Column path();

    /** The underlying {@link Table}. */
    public Table table() {
      return node().table();
    }

    public static Builder builder() {
      return new AutoValue_Hierarchy_PathsTable.Builder();
    }

    /** Builder for {@link PathsTable}. */
    @AutoValue.Builder
    public abstract static class Builder {

      public abstract Builder node(Column node);

      public abstract Column node();

      public abstract Builder path(Column path);

      public abstract Column path();

      public PathsTable build() {
        Preconditions.checkArgument(
            node().table().equals(path().table()),
            "node and path columns must share the same table, but found [%s] and [%s] tables",
            node().table(),
            path().table());
        return autoBuild();
      }

      abstract PathsTable autoBuild();
    }
  }

  /** Builder for {@link Hierarchy}. */
  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder descendantsTable(DescendantsTable descendantsTable);

    public abstract Builder childrenTable(ChildrenTable childrenTable);

    public abstract Builder pathsTable(PathsTable pathsTable);

    public abstract Hierarchy build();
  }
}

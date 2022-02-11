package bio.terra.tanagra.service.underlay;

import com.google.auto.value.AutoValue;
import com.google.common.base.Preconditions;

/**
 * The underlay configuration for information required to do a text search for an entity instance.
 *
 * <p>This class represents the configuration of the mapping of an entity text search to an index
 * table.
 */
@AutoValue
public abstract class TextSearchInformation {
  public abstract TextTable textTable();

  public static Builder builder() {
    return new AutoValue_TextSearchInformation.Builder();
  }

  /**
   * The specification for a table with information about doing the text search.
   *
   * <p>For each (lookupTableKey, fullText) pair, there should be a row in the table referenced by
   * the {@link TextTable}.
   */
  @AutoValue
  public abstract static class TextTable {
    /** A column that foreign keys to the entity primary key. */
    public abstract Column lookupTableKey();

    /**
     * A column that we can do substring matches against.
     *
     * <p>Must have the same Table as {@link #lookupTableKey()}.
     */
    public abstract Column fullText();

    /** The underlying {@link Table}. */
    public Table table() {
      return fullText().table();
    }

    public static Builder builder() {
      return new AutoValue_TextSearchInformation_TextTable.Builder();
    }

    /** Builder for {@link TextTable}. */
    @AutoValue.Builder
    public abstract static class Builder {

      public abstract Builder lookupTableKey(Column lookupTableKey);

      public abstract Column lookupTableKey();

      public abstract Builder fullText(Column fullText);

      public abstract Column fullText();

      public TextTable build() {
        Preconditions.checkArgument(
            lookupTableKey().table().equals(fullText().table()),
            "lookupTableKey and fullText must share the same table, but found [%s] and [%s] tables",
            lookupTableKey().table(),
            fullText().table());
        return autoBuild();
      }

      abstract TextTable autoBuild();
    }
  }

  /** Builder for {@link TextSearchInformation}. */
  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder textTable(TextTable textTable);

    public abstract TextSearchInformation build();
  }
}

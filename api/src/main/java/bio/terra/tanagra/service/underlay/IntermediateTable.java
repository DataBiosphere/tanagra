package bio.terra.tanagra.service.underlay;

import com.google.auto.value.AutoValue;

/** An intermediate table that holds relationships between two SQL columns in different tables. */
@AutoValue
public abstract class IntermediateTable {
  /** The column in the first entity's primary table that FKs to the intermediate table. */
  public abstract Column entity1EntityTableKey();
  /** The column in the intermediate table that FKs to the first entity's primary table. */
  public abstract Column entity1IntermediateTableKey();

  /** The column in the second entity's primary table that FKs to the intermediate table. */
  public abstract Column entity2EntityTableKey();
  /** The column in the intermediate table that FKs to the second entity's primary table. */
  public abstract Column entity2IntermediateTableKey();

  public static Builder builder() {
    return new AutoValue_IntermediateTable.Builder();
  }

  /** A builder for {@link IntermediateTable}. */
  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder entity1EntityTableKey(Column value);

    public abstract Builder entity1IntermediateTableKey(Column value);

    public abstract Builder entity2EntityTableKey(Column value);

    public abstract Builder entity2IntermediateTableKey(Column value);

    public abstract IntermediateTable build();
  }
}

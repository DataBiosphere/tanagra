package bio.terra.tanagra.service.underlay;

import com.google.auto.value.AutoValue;

/** A foreign key constraint between two SLQ columns. */
@AutoValue
public abstract class ForeignKey {
  /** The primary key column in the constraint. */
  public abstract Column primaryKey();
  /** The foreign key column that references the primary key. */
  public abstract Column foreignKey();

  public static Builder builder() {
    return new AutoValue_ForeignKey.Builder();
  }

  /** A builder for {@link ForeignKey}. */
  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder primaryKey(Column primaryKey);

    public abstract Builder foreignKey(Column foreignKey);

    public abstract ForeignKey build();
  }
}

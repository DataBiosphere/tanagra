package bio.terra.tanagra.service.search;

import com.google.auto.value.AutoValue;

/** A relational relationship between two entities. */
@AutoValue
public abstract class Relationship {
  /** The name for the relatinoship. */
  public abstract String name();

  /** The first entity in the relationship. */
  public abstract Entity entity1();

  /** The second entity in the relationship. */
  public abstract Entity entity2();

  public static Builder builder() {
    return new AutoValue_Relationship.Builder();
  }

  /** A builder for {@link Relationship}. */
  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder name(String name);

    public abstract Builder entity1(Entity entity1);

    public abstract Builder entity2(Entity entity2);

    public abstract Relationship build();
  }
}

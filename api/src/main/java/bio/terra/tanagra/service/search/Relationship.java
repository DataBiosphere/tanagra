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

  /** Returns whether the {@code x} and {@code y} entities are the two entities of the relationship, in any order. */
  public boolean unorderedEntitiesAre(Entity x, Entity y) {
    return (entity1().equals(x) && entity2().equals(y))
        || (entity1().equals(y) && entity2().equals(x));
  }

  public static Builder builder() {
    return new AutoValue_Relationship.Builder();
  }

  /** A builder for {@link Relationship}. */
  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder name(String name);

    public abstract String name();

    public abstract Builder entity1(Entity entity1);

    public abstract Builder entity2(Entity entity2);

    public Relationship build() {
      NameUtils.checkName(name(), "Relationship name");
      return autoBuild();
    }

    abstract Relationship autoBuild();
  }
}

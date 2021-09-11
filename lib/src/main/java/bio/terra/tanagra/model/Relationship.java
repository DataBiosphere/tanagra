package bio.terra.tanagra.model;

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

  /**
   * Returns whether the {@code x} and {@code y} entities are the two entities of the relationship,
   * in any order.
   */
  public boolean hasEntitiesUnordered(Entity x, Entity y) {
    return (entity1().equals(x) && entity2().equals(y))
        || (entity1().equals(y) && entity2().equals(x));
  }

  /** Returns whether the {@code x} is either of the two entities of the relationship. */
  public boolean hasEntity(Entity x) {
    return entity1().equals(x) || entity2().equals(x);
  }

  /**
   * Returns the other entity in the relationship. {@code x} must be either {@link #entity1()} or
   * {@link #entity2()}.
   */
  public Entity other(Entity x) {
    if (entity1().equals(x)) {
      return entity2();
    } else if (entity2().equals(x)) {
      return entity1();
    } else {
      throw new IllegalArgumentException(
          String.format(
              "Tried to get the other entity in a relationship for an entity not in the relationship: %s",
              x));
    }
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

package bio.terra.tanagra.service.search;

import com.google.auto.value.AutoValue;

/**
 * A relational logical entity supported by a dataset underlay.
 *
 * <p>In an OMOP schema, you might have a "person" entity, and a "procedure" entity.
 */
@AutoValue
public abstract class Entity {
  /** The name of the entity. */
  public abstract String name();

  /** The name of the dataset underlay that this entity is a part of. */
  public abstract String underlay();

  public static Builder builder() {
    return new AutoValue_Entity.Builder();
  }

  /** A builder for {@link Entity}. */
  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder name(String name);

    public abstract Builder underlay(String underlay);

    public abstract Entity build();
  }
}

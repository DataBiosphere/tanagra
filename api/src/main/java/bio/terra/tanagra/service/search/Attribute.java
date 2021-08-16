package bio.terra.tanagra.service.search;

import com.google.auto.value.AutoValue;

/**
 * An Attribute describes a characteristic of an {@link Entity}.
 *
 * <p>An attribute of a "person" entity might be their "date of birth."
 */
@AutoValue
public abstract class Attribute {
  public abstract String name();

  public abstract DataType dataType();

  public abstract Entity entity();

  public static Builder builder() {
    return new AutoValue_Attribute.Builder();
  }

  /** A builder for {@link Attribute}. */
  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder name(String name);

    public abstract String name();

    public abstract Builder dataType(DataType dataType);

    public abstract Builder entity(Entity entity);

    public Attribute build() {
      NameUtils.checkName(name(), "Attribute name");
      return autoBuild();
    }

    abstract Attribute autoBuild();
  }
}

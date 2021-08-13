package bio.terra.tanagra.service.underlay;

import bio.terra.tanagra.service.search.Attribute;
import com.google.auto.value.AutoValue;

/** A mapping from a logical attribute to the physical talbes and columns that make it up. */
public interface AttributeMapping {
  /** The attribute being mapped. */
  Attribute attribute();

  /**
   * A visitor pattern interface for an {@link AttributeMapping}.
   *
   * @param <R> the return value for the visitor.
   */
  interface Visitor<R> {
    R visitSimpleColumn(SimpleColumn simpleColumn);

    R visitNormalizedColumn(NormalizedColumn normalizedColumn);
  }

  /** Accept the {@link AttributeMapping.Visitor} pattern. */
  <R> R accept(AttributeMapping.Visitor<R> visitor);

  /**
   * An {@link AttributeMapping} where an attribute maps simply to a column on the primary table.
   */
  @AutoValue
  abstract class SimpleColumn implements AttributeMapping {
    @Override
    public abstract Attribute attribute();

    /** The column on the Entity's primary table represented by the attribute. */
    public abstract Column column();

    @Override
    public <R> R accept(Visitor<R> visitor) {
      return visitor.visitSimpleColumn(this);
    }

    public static SimpleColumn create(Attribute attribute, Column column) {
      return new AutoValue_AttributeMapping_SimpleColumn(attribute, column);
    }
  }

  @AutoValue
  abstract class NormalizedColumn implements AttributeMapping {
    @Override
    public abstract Attribute attribute();

    /** The column on Entity's primary table that references {@link #factTableKey}. */
    public abstract Column primaryTableKey();

    /** The column on the fact table that is referenced by {@link #primaryTableKey}. */
    public abstract Column factTableKey();

    /** The column on the fact table that is represented by the {@link #attribute}. */
    public abstract Column factColumn();

    @Override
    public <R> R accept(Visitor<R> visitor) {
      return visitor.visitNormalizedColumn(this);
    }

    public static Builder builder() {
      return new AutoValue_AttributeMapping_NormalizedColumn.Builder();
    }

    /** Builder for {@link NormalizedColumn}. */
    @AutoValue.Builder
    public abstract static class Builder {

      public abstract Builder attribute(Attribute attribute);

      public abstract Builder primaryTableKey(Column primaryTableKey);

      public abstract Builder factTableKey(Column factTableKey);

      public abstract Builder factColumn(Column factColumn);

      public abstract NormalizedColumn build();
    }
  }
}

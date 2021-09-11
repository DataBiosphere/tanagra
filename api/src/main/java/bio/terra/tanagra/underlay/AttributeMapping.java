package bio.terra.tanagra.underlay;

import bio.terra.tanagra.model.Attribute;
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

    R visitLookupColumn(LookupColumn lookupColumn);
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

  /**
   * An {@link AttributeMapping} where an attribute maps to a column on a lookup table referenced by
   * a foreign key column on the primary table.
   */
  @AutoValue
  abstract class LookupColumn implements AttributeMapping {
    @Override
    public abstract Attribute attribute();

    /** The column on Entity's primary table that references {@link #lookupTableKey}. */
    public abstract Column primaryTableLookupKey();

    /** The column on the lookup table that is referenced by {@link #primaryTableLookupKey}. */
    public abstract Column lookupTableKey();

    /** The column on the lookup table that is represented by the {@link #attribute}. */
    public abstract Column lookupColumn();

    @Override
    public <R> R accept(Visitor<R> visitor) {
      return visitor.visitLookupColumn(this);
    }

    public static Builder builder() {
      return new AutoValue_AttributeMapping_LookupColumn.Builder();
    }

    /** Builder for {@link LookupColumn}. */
    @AutoValue.Builder
    public abstract static class Builder {

      public abstract Builder attribute(Attribute attribute);

      public abstract Builder primaryTableLookupKey(Column primaryTableKey);

      public abstract Builder lookupTableKey(Column lookupTableKey);

      public abstract Builder lookupColumn(Column lookupColumn);

      public abstract LookupColumn build();
    }
  }
}

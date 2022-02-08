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

    R visitLookupColumn(LookupColumn lookupColumn);

    R visitHierarchyPathColumn(HierarchyPathColumn hierarchyPathColumn);

    R visitHierarchyNumChildrenColumn(HierarchyNumChildrenColumn hierarchyNumChildrenColumn);
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

  /**
   * An {@link AttributeMapping} where an attribute maps to the path column in the node-path table
   * for the hierarchy. This type of attribute mapping is never specified in the underlay
   * explicitly. It is added automatically for any attribute that is part of a hierarchy.
   */
  @AutoValue
  abstract class HierarchyPathColumn implements AttributeMapping {
    @Override
    public abstract Attribute attribute();

    /* The hierarchy for the attribute. */
    public abstract Hierarchy hierarchy();

    /**
     * The mapping for the attribute that the hierarchy is based on. This cannot be a {@link
     * HierarchyPathColumn} mapping.
     */
    public abstract AttributeMapping hierarchyAttributeMapping();

    @Override
    public <R> R accept(Visitor<R> visitor) {
      return visitor.visitHierarchyPathColumn(this);
    }

    public static HierarchyPathColumn create(
        Attribute attribute, Hierarchy hierarchy, AttributeMapping attributeMapping) {
      return new AutoValue_AttributeMapping_HierarchyPathColumn(
          attribute, hierarchy, attributeMapping);
    }
  }

  /**
   * An {@link AttributeMapping} where an attribute maps to the numChildren column in the node-path
   * table for the hierarchy. This type of attribute mapping is never specified in the underlay
   * explicitly. It is added automatically for any attribute that is part of a hierarchy.
   */
  @AutoValue
  abstract class HierarchyNumChildrenColumn implements AttributeMapping {
    @Override
    public abstract Attribute attribute();

    /* The hierarchy for the attribute. */
    public abstract Hierarchy hierarchy();

    /**
     * The mapping for the attribute that the hierarchy is based on. This cannot be a {@link
     * HierarchyNumChildrenColumn} mapping.
     */
    public abstract AttributeMapping hierarchyAttributeMapping();

    @Override
    public <R> R accept(Visitor<R> visitor) {
      return visitor.visitHierarchyNumChildrenColumn(this);
    }

    public static HierarchyNumChildrenColumn create(
        Attribute attribute, Hierarchy hierarchy, AttributeMapping attributeMapping) {
      return new AutoValue_AttributeMapping_HierarchyNumChildrenColumn(
          attribute, hierarchy, attributeMapping);
    }
  }
}

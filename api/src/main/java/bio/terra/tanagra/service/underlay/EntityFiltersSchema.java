package bio.terra.tanagra.service.underlay;

import bio.terra.tanagra.proto.underlay.FilterableAttribute;
import bio.terra.tanagra.service.search.Attribute;
import bio.terra.tanagra.service.search.Entity;
import bio.terra.tanagra.service.search.Relationship;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;
import java.util.Map;

/** The schema of how an entity filter can be constructed for a given entity. */
@AutoValue
public abstract class EntityFiltersSchema {
  /** The entity that this filter is for. */
  public abstract Entity entity();

  /** Map from the entity's attributes to how they can be used in filters. */
  public abstract ImmutableMap<Attribute, FilterableAttribute> filterableAttributes();

  /** Map from the entity's relationships to the entity filters on the related entity. */
  public abstract ImmutableMap<Relationship, EntityFiltersSchema> filterableRelationships();

  public static Builder builder() {
    return new AutoValue_EntityFiltersSchema.Builder();
  }

  /** Builder for {@link EntityFiltersSchema}. */
  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder entity(Entity entity);

    public abstract Builder filterableAttributes(
        Map<Attribute, FilterableAttribute> filterableAttributes);

    public abstract Builder filterableRelationships(
        Map<Relationship, EntityFiltersSchema> filterableRelationships);

    public abstract EntityFiltersSchema build();
  }
}

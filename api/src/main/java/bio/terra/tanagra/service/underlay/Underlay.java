package bio.terra.tanagra.service.underlay;

import bio.terra.tanagra.service.search.Attribute;
import bio.terra.tanagra.service.search.Entity;
import bio.terra.tanagra.service.search.Relationship;
import com.google.auto.value.AutoValue;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableTable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * An underlay dataset used to power a Tanagra experience.
 *
 * <p>Each underlying dataset that a user wants to explore with Tanagra is represented by an
 * "underlay." That underlay represents what logical entities are being modeled, what physical
 * tables and columns are in the underlying dataset, and the mapping between Tanagra concepts like
 * entities and searches and the physical datasets.
 *
 * <p>An Underlay instance is what powers a Tanagra search experience for an external backing
 * dataset.
 */
@AutoValue
public abstract class Underlay {
  public abstract String name();
  /** Map from entity names to entities. */
  public abstract ImmutableMap<String, Entity> entities();
  /** Table of entity and attribute names to attributes. */
  public abstract ImmutableTable<Entity, String, Attribute> attributes();
  /** Map from relationship name to relationships between entities. */
  public abstract ImmutableMap<String, Relationship> relationships();

  /** Map from entities to the columns for their primary keys. */
  public abstract ImmutableMap<Entity, Column> primaryKeys();
  /** Map from attributes to their {@link AttributeMapping}s. */
  public abstract ImmutableMap<Attribute, AttributeMapping> attributeMappings();
  /**
   * Map from relationships to the foreign keys describing the relation between the tables. The
   * {@link Relationship#entity1()} should correspond to the {@link ForeignKey#primaryKey()}.
   */
  public abstract ImmutableMap<Relationship, ForeignKey> foreignKeys();

  /**
   * Map from entities to the filters schema that supports the entity, if any.
   *
   * <p>Entities may not be allowed to be filtered on.
   */
  public abstract ImmutableMap<Entity, EntityFiltersSchema> entityFiltersSchemas();

  /**
   * Find a relationship between 2 entities. The relationship's entity ordering may be reversed from
   * the arguments.
   */
  public Optional<Relationship> getRelationship(Entity x, Entity y) {
    List<Relationship> matching =
        relationships().values().stream()
            .filter(relationship -> relationship.hasEntitiesUnordered(x, y))
            .collect(Collectors.toList());
    if (matching.isEmpty()) {
      return Optional.empty();
    }
    // TODO consider adding relationship names if we need to support multiple relationships between
    // entities.
    Preconditions.checkState(
        matching.size() <= 1,
        "Unable to pick a relationship among multiple relationships between entities. %s",
        matching);
    return Optional.of(matching.get(0));
  }

  /** Returns all the relationships that the entity is a member of. */
  public Set<Relationship> getRelationshipsOf(Entity entity) {
    return relationships().values().stream()
        .filter(relationship -> relationship.hasEntity(entity))
        .collect(ImmutableSet.toImmutableSet());
  }

  public static Builder builder() {
    return new AutoValue_Underlay.Builder();
  }

  /** A builder for {@link Underlay}. */
  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder name(String name);

    public abstract Builder entities(Map<String, Entity> entities);

    public abstract Builder attributes(
        com.google.common.collect.Table<Entity, String, Attribute> attributes);

    public abstract Builder relationships(Map<String, Relationship> value);

    public abstract Builder primaryKeys(Map<Entity, Column> primaryKeys);

    public abstract Builder attributeMappings(Map<Attribute, AttributeMapping> attributeMappings);

    public abstract Builder foreignKeys(Map<Relationship, ForeignKey> value);

    public abstract Builder entityFiltersSchemas(Map<Entity, EntityFiltersSchema> value);

    abstract Underlay build();
  }
}

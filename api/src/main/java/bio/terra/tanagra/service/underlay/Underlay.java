package bio.terra.tanagra.service.underlay;

import bio.terra.tanagra.service.search.Attribute;
import bio.terra.tanagra.service.search.Entity;
import bio.terra.tanagra.service.search.Relationship;
import com.google.auto.value.AutoValue;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableTable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
  /** Map where an attribute maps simply to a column on the primary table. */
  public abstract ImmutableMap<Attribute, Column> simpleAttributesToColumns();
  /**
   * Map from relationships to the foreign keys describing the relation between the tables. The
   * {@link Relationship#entity1()} should correspond to the {@link ForeignKey#primaryKey()}.
   */
  public abstract ImmutableMap<Relationship, ForeignKey> foreignKeys();

  // DO NOT SUBMIT delete me?
  public Optional<Entity> getEntity(String entityName) {
    return Optional.ofNullable(entities().get(entityName));
  }

  public Optional<Attribute> getAttribute(Entity entity, String attributeName) {
    return Optional.ofNullable(attributes().get(entity, attributeName));
  }

  /**
   * Find a relationship between 2 entities. The relationship's entity ordering may be reversed from
   * the arguments.
   */
  public Optional<Relationship> getRelationship(Entity x, Entity y) {
    List<Relationship> matching =
        relationships().values().stream()
            .filter(
                relationship ->
                    (relationship.entity1().equals(x) && relationship.entity2().equals(y))
                        || (relationship.entity1().equals(y) && relationship.entity2().equals(x)))
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

    public abstract Builder simpleAttributesToColumns(
        Map<Attribute, Column> simpleAttributesToColumns);

    public abstract Builder foreignKeys(Map<Relationship, ForeignKey> value);

    abstract Underlay build();
  }
}

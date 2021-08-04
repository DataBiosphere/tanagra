package bio.terra.tanagra.service.underlay;

import bio.terra.tanagra.service.search.Attribute;
import bio.terra.tanagra.service.search.AttributeVariable;
import bio.terra.tanagra.service.search.Entity;
import bio.terra.tanagra.service.search.EntityVariable;
import bio.terra.tanagra.service.search.UnderlaySqlResolver;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Table;
import java.util.Map;
import java.util.Optional;

/**
 * An underlay dataset used to power a Tanagra experience.
 *
 * <p>Each underlying dataset that a user wants to explore with Tanagra is represented by an
 * "underlay." That underlay represents what logical entities are being modeled, what physical
 * tables and columns are in the underlying dataset, and the mapping between Tanagra concepts like
 * entities and searches and the physical datasets.
 *
 * <p>An Underlay instance is what powers a Tanagra search experience for an extenral backing
 * dataset.
 */
@AutoValue
public abstract class Underlay {
  public abstract String name();
  /** Map from entity names to entities. */
  abstract ImmutableMap<String, Entity> entities();
  /** Table of entity and attribute names to attributes. */
  abstract ImmutableTable<Entity, String, Attribute> attributes();

  /** Map from entities to the columns for their primary keys. */
  abstract ImmutableMap<Entity, Column> primaryKeys();
  /** Map where an attribute maps simply to a column on the primary table. */
  abstract ImmutableMap<Attribute, Column> simpleAttributesToColumns();

  public Optional<Entity> getEntity(String entityName) {
    return Optional.ofNullable(entities().get(entityName));
  }

  public Optional<Attribute> getAttribute(Entity entity, String attributeName) {
    return Optional.ofNullable(attributes().get(entity, attributeName));
  }

  public UnderlaySqlResolver getUnderlaySqlResolver() {
    return new MappingResolver();
  }

  // TODO resolve relationships.

  public static Builder builder() {
    return new AutoValue_Underlay.Builder();
  }

  /** A {@link UnderlaySqlResolver} based on the Underlay mappings. */
  private class MappingResolver implements UnderlaySqlResolver {
    @Override
    public String resolveTable(EntityVariable entityVariable) {
      Column primaryKey = primaryKeys().get(entityVariable.entity());
      if (primaryKey == null) {
        throw new IllegalArgumentException(
            String.format("Unable to find primary key for entity %s", entityVariable.entity()));
      }
      // projectId.datasetId.table AS variableName
      return String.format(
          "%s.%s.%s AS %s",
          primaryKey.table().dataset().projectId(),
          primaryKey.table().dataset().datasetId(),
          primaryKey.table().name(),
          entityVariable.variable().name());
    }

    @Override
    public String resolve(AttributeVariable attributeVariable) {
      Column column = simpleAttributesToColumns().get(attributeVariable.attribute());
      if (column == null) {
        // TODO implement other kinds of attribute mappings.
        throw new IllegalArgumentException(
            String.format(
                "Unable to find column mapping for attribute %s", attributeVariable.attribute()));
      }
      // variableName.column
      return String.format("%s.%s", attributeVariable.variable().name(), column.name());
    }
  }

  /** A builder for {@link Underlay}. */
  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder name(String name);

    public abstract Builder entities(Map<String, Entity> entities);

    public abstract Builder attributes(Table<Entity, String, Attribute> attributes);

    public abstract Builder primaryKeys(Map<Entity, Column> primaryKeys);

    public abstract Builder simpleAttributesToColumns(
        Map<Attribute, Column> simpleAttributesToColumns);

    abstract Underlay build();
  }
}

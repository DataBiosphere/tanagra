package bio.terra.tanagra.underlay2;

import bio.terra.common.exception.NotFoundException;
import bio.terra.tanagra.exception.SystemException;
import bio.terra.tanagra.query.QueryExecutor;
import bio.terra.tanagra.underlay2.entitygroup.EntityGroup;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

public final class Underlay {
  private final String name;
  private final String displayName;
  private @Nullable final String description;
  private final ImmutableMap<String, String> metadataProperties;
  private final ImmutableList<Entity> entities;
  private final ImmutableList<EntityGroup> entityGroups;
  private final QueryExecutor queryExecutor;

  public Underlay(
      String name,
      String displayName,
      @Nullable String description,
      @Nullable Map<String, String> metadataProperties,
      List<Entity> entities,
      List<EntityGroup> entityGroups,
      QueryExecutor queryExecutor) {
    this.name = name;
    this.displayName = displayName;
    this.description = description;
    this.metadataProperties =
        metadataProperties == null ? ImmutableMap.of() : ImmutableMap.copyOf(metadataProperties);
    this.entities = ImmutableList.copyOf(entities);
    this.entityGroups = ImmutableList.copyOf(entityGroups);
    this.queryExecutor = queryExecutor;
  }

  public String getName() {
    return name;
  }

  public String getDisplayName() {
    return displayName;
  }

  public String getDescription() {
    return description;
  }

  public ImmutableMap<String, String> getMetadataProperties() {
    return metadataProperties;
  }

  public String getMetadataProperty(String key) {
    return metadataProperties.get(key);
  }

  public ImmutableList<Entity> getEntities() {
    return entities;
  }

  public Entity getEntity(String name) {
    return entities.stream()
        .filter(e -> name.equals(e.getName()))
        .findFirst()
        .orElseThrow(() -> new NotFoundException("Entity not found: " + name));
  }

  public Entity getPrimaryEntity() {
    return entities.stream()
        .filter(e -> e.isPrimary())
        .findFirst()
        .orElseThrow(() -> new SystemException("No primary entity defined"));
  }

  public ImmutableList<EntityGroup> getEntityGroups() {
    return entityGroups;
  }

  public EntityGroup getEntityGroup(String name) {
    return entityGroups.stream()
        .filter(eg -> name.equals(eg.getName()))
        .findFirst()
        .orElseThrow(() -> new NotFoundException("Entity group not found: " + name));
  }

  public QueryExecutor getQueryExecutor() {
    return queryExecutor;
  }
}

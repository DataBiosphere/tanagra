package bio.terra.tanagra.api2.query.hint;

import bio.terra.tanagra.query.Literal;
import bio.terra.tanagra.underlay2.Entity;
import bio.terra.tanagra.underlay2.entitygroup.EntityGroup;
import javax.annotation.Nullable;

public class HintQueryRequest {
  private final Entity entity;
  private final @Nullable Entity relatedEntity;
  private final @Nullable Literal relatedEntityId;
  private final @Nullable EntityGroup entityGroup;

  public HintQueryRequest(
      Entity entity,
      @Nullable Entity relatedEntity,
      @Nullable Literal relatedEntityId,
      @Nullable EntityGroup entityGroup) {
    this.entity = entity;
    this.relatedEntity = relatedEntity;
    this.relatedEntityId = relatedEntityId;
    this.entityGroup = entityGroup;
  }

  public boolean isEntityLevel() {
    return relatedEntity == null || relatedEntityId == null || entityGroup == null;
  }

  public Entity getEntity() {
    return entity;
  }

  public Entity getRelatedEntity() {
    return relatedEntity;
  }

  public Literal getRelatedEntityId() {
    return relatedEntityId;
  }

  public EntityGroup getEntityGroup() {
    return entityGroup;
  }
}

package bio.terra.tanagra.api.query.hint;

import bio.terra.tanagra.api.shared.Literal;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.entitymodel.Entity;
import bio.terra.tanagra.underlay.entitymodel.entitygroup.EntityGroup;
import javax.annotation.Nullable;

public class HintQueryRequest {
  private final Underlay underlay;
  private final Entity hintedEntity;
  private final @Nullable Entity relatedEntity;
  private final @Nullable Literal relatedEntityId;
  private final @Nullable EntityGroup entityGroup;
  private final boolean isDryRun;

  public HintQueryRequest(
      Underlay underlay,
      Entity hintedEntity,
      @Nullable Entity relatedEntity,
      @Nullable Literal relatedEntityId,
      @Nullable EntityGroup entityGroup,
      boolean isDryRun) {
    this.underlay = underlay;
    this.hintedEntity = hintedEntity;
    this.relatedEntity = relatedEntity;
    this.relatedEntityId = relatedEntityId;
    this.entityGroup = entityGroup;
    this.isDryRun = isDryRun;
  }

  public boolean isEntityLevel() {
    return relatedEntity == null || relatedEntityId == null || entityGroup == null;
  }

  public Underlay getUnderlay() {
    return underlay;
  }

  public Entity getHintedEntity() {
    return hintedEntity;
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

  public boolean isDryRun() {
    return isDryRun;
  }
}

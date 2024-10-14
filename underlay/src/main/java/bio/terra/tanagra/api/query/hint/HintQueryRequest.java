package bio.terra.tanagra.api.query.hint;

import bio.terra.tanagra.api.shared.Literal;
import bio.terra.tanagra.query.sql.SqlQueryRequest;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.entitymodel.Entity;
import bio.terra.tanagra.underlay.entitymodel.entitygroup.EntityGroup;
import jakarta.annotation.Nullable;

public class HintQueryRequest {
  private final Underlay underlay;
  private final Entity hintedEntity;
  private final Entity relatedEntity;
  private final Literal relatedEntityId;
  private final EntityGroup entityGroup;
  private final SqlQueryRequest sqlQueryRequest;
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
    this.sqlQueryRequest = null;
    this.isDryRun = isDryRun;
  }

  public HintQueryRequest(
      Underlay underlay,
      Entity hintedEntity,
      @Nullable SqlQueryRequest sqlQueryRequest,
      boolean isDryRun) {
    this.underlay = underlay;
    this.hintedEntity = hintedEntity;
    this.sqlQueryRequest = sqlQueryRequest;
    this.relatedEntity = null;
    this.relatedEntityId = null;
    this.entityGroup = null;
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

  @Nullable
  public Entity getRelatedEntity() {
    return relatedEntity;
  }

  public @Nullable Literal getRelatedEntityId() {
    return relatedEntityId;
  }

  public @Nullable EntityGroup getEntityGroup() {
    return entityGroup;
  }

  public @Nullable SqlQueryRequest getSqlQueryRequest() {
    return sqlQueryRequest;
  }

  public boolean isDryRun() {
    return isDryRun;
  }
}

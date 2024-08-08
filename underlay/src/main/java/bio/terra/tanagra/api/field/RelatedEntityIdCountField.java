package bio.terra.tanagra.api.field;

import bio.terra.tanagra.api.shared.DataType;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.entitymodel.Entity;
import bio.terra.tanagra.underlay.entitymodel.Hierarchy;
import bio.terra.tanagra.underlay.entitymodel.entitygroup.EntityGroup;
import jakarta.annotation.Nullable;

public class RelatedEntityIdCountField extends ValueDisplayField {
  private final Underlay underlay;
  private final Entity countForEntity;
  private final Entity countedEntity;
  private final EntityGroup entityGroup;
  private final @Nullable Hierarchy hierarchy;

  public RelatedEntityIdCountField(
      Underlay underlay,
      Entity countForEntity,
      Entity countedEntity,
      EntityGroup entityGroup,
      @Nullable Hierarchy hierarchy) {
    this.underlay = underlay;
    this.countForEntity = countForEntity;
    this.countedEntity = countedEntity;
    this.entityGroup = entityGroup;
    this.hierarchy = hierarchy;
  }

  public EntityGroup getEntityGroup() {
    return entityGroup;
  }

  public Underlay getUnderlay() {
    return underlay;
  }

  public Entity getCountForEntity() {
    return countForEntity;
  }

  public Entity getCountedEntity() {
    return countedEntity;
  }

  public Hierarchy getHierarchy() {
    return hierarchy;
  }

  public boolean hasHierarchy() {
    return hierarchy != null;
  }

  @Override
  public Entity getEntity() {
    return getCountForEntity();
  }

  @Override
  public DataType getDataType() {
    return DataType.INT64;
  }
}

package bio.terra.tanagra.api.field;

import bio.terra.tanagra.api.shared.DataType;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.entitymodel.Entity;
import bio.terra.tanagra.underlay.entitymodel.Hierarchy;

public class HierarchyNumChildrenField extends ValueDisplayField {
  private final Underlay underlay;
  private final Entity entity;
  private final Hierarchy hierarchy;

  public HierarchyNumChildrenField(Underlay underlay, Entity entity, Hierarchy hierarchy) {
    this.underlay = underlay;
    this.entity = entity;
    this.hierarchy = hierarchy;
  }

  public Underlay getUnderlay() {
    return underlay;
  }

  public Entity getEntity() {
    return entity;
  }

  public Hierarchy getHierarchy() {
    return hierarchy;
  }

  @Override
  public DataType getDataType() {
    return DataType.INT64;
  }
}

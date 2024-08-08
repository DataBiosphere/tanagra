package bio.terra.tanagra.api.field;

import bio.terra.tanagra.api.shared.DataType;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.entitymodel.Entity;
import bio.terra.tanagra.underlay.entitymodel.Hierarchy;

public class HierarchyIsMemberField extends ValueDisplayField {
  private final Underlay underlay;
  private final Entity entity;
  private final Hierarchy hierarchy;

  public HierarchyIsMemberField(Underlay underlay, Entity entity, Hierarchy hierarchy) {
    this.underlay = underlay;
    this.entity = entity;
    this.hierarchy = hierarchy;
  }

  public Underlay getUnderlay() {
    return underlay;
  }

  @Override
  public Entity getEntity() {
    return entity;
  }

  public Hierarchy getHierarchy() {
    return hierarchy;
  }

  @Override
  public DataType getDataType() {
    return DataType.BOOLEAN;
  }
}

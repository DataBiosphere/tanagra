package bio.terra.tanagra.api.filter;

import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.entitymodel.Entity;
import bio.terra.tanagra.underlay.entitymodel.Hierarchy;

public class HierarchyIsLeafFilter extends EntityFilter {
  private final Underlay underlay;
  private final Entity entity;
  private final Hierarchy hierarchy;

  public HierarchyIsLeafFilter(Underlay underlay, Entity entity, Hierarchy hierarchy) {
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
}

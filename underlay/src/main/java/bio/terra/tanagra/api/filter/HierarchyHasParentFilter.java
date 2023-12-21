package bio.terra.tanagra.api.filter;

import bio.terra.tanagra.query.Literal;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.entitymodel.Entity;
import bio.terra.tanagra.underlay.entitymodel.Hierarchy;

public class HierarchyHasParentFilter extends EntityFilter {
  private final Underlay underlay;
  private final Entity entity;
  private final Hierarchy hierarchy;
  private final Literal parentId;

  public HierarchyHasParentFilter(
      Underlay underlay, Entity entity, Hierarchy hierarchy, Literal parentId) {
    this.underlay = underlay;
    this.entity = entity;
    this.hierarchy = hierarchy;
    this.parentId = parentId;
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

  public Literal getParentId() {
    return parentId;
  }
}

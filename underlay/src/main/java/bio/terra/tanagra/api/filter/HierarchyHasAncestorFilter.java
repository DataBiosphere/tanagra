package bio.terra.tanagra.api.filter;

import bio.terra.tanagra.query.Literal;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.entitymodel.Entity;
import bio.terra.tanagra.underlay.entitymodel.Hierarchy;

public class HierarchyHasAncestorFilter extends EntityFilter {
  private final Underlay underlay;
  private final Entity entity;
  private final Hierarchy hierarchy;
  private final Literal ancestorId;

  public HierarchyHasAncestorFilter(
      Underlay underlay, Entity entity, Hierarchy hierarchy, Literal ancestorId) {
    this.underlay = underlay;
    this.entity = entity;
    this.hierarchy = hierarchy;
    this.ancestorId = ancestorId;
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

  public Literal getAncestorId() {
    return ancestorId;
  }
}

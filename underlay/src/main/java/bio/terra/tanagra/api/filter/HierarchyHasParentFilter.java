package bio.terra.tanagra.api.filter;

import bio.terra.tanagra.api.shared.Literal;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.entitymodel.Entity;
import bio.terra.tanagra.underlay.entitymodel.Hierarchy;
import com.google.common.collect.ImmutableList;
import java.util.List;

public class HierarchyHasParentFilter extends EntityFilter {
  private final Underlay underlay;
  private final Entity entity;
  private final Hierarchy hierarchy;
  private final ImmutableList<Literal> parentIds;

  public HierarchyHasParentFilter(
      Underlay underlay, Entity entity, Hierarchy hierarchy, Literal parentId) {
    this.underlay = underlay;
    this.entity = entity;
    this.hierarchy = hierarchy;
    this.parentIds = ImmutableList.of(parentId);
  }

  public HierarchyHasParentFilter(
      Underlay underlay, Entity entity, Hierarchy hierarchy, List<Literal> parentIds) {
    this.underlay = underlay;
    this.entity = entity;
    this.hierarchy = hierarchy;
    this.parentIds = ImmutableList.copyOf(parentIds);
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

  public ImmutableList<Literal> getParentIds() {
    return parentIds;
  }
}

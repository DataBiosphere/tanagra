package bio.terra.tanagra.api.filter;

import bio.terra.tanagra.api.shared.Literal;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.entitymodel.Entity;
import bio.terra.tanagra.underlay.entitymodel.Hierarchy;
import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Objects;

public class HierarchyHasAncestorFilter extends EntityFilter {
  private final Underlay underlay;
  private final Entity entity;
  private final Hierarchy hierarchy;
  private final ImmutableList<Literal> ancestorIds;

  public HierarchyHasAncestorFilter(
      Underlay underlay, Entity entity, Hierarchy hierarchy, Literal ancestorId) {
    this.underlay = underlay;
    this.entity = entity;
    this.hierarchy = hierarchy;
    this.ancestorIds = ImmutableList.of(ancestorId);
  }

  public HierarchyHasAncestorFilter(
      Underlay underlay, Entity entity, Hierarchy hierarchy, List<Literal> ancestorIds) {
    this.underlay = underlay;
    this.entity = entity;
    this.hierarchy = hierarchy;
    this.ancestorIds = ImmutableList.copyOf(ancestorIds);
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

  public ImmutableList<Literal> getAncestorIds() {
    return ancestorIds;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    HierarchyHasAncestorFilter that = (HierarchyHasAncestorFilter) o;
    return underlay.equals(that.underlay)
        && entity.equals(that.entity)
        && hierarchy.equals(that.hierarchy)
        && ancestorIds.equals(that.ancestorIds);
  }

  @Override
  public int hashCode() {
    return Objects.hash(underlay, entity, hierarchy, ancestorIds);
  }
}

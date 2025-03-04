package bio.terra.tanagra.api.filter;

import bio.terra.tanagra.api.shared.Literal;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.entitymodel.Entity;
import bio.terra.tanagra.underlay.entitymodel.Hierarchy;
import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Objects;
import org.slf4j.LoggerFactory;

public class HierarchyHasAncestorFilter extends EntityFilter {
  private final Hierarchy hierarchy;
  private final ImmutableList<Literal> ancestorIds;

  public HierarchyHasAncestorFilter(
      Underlay underlay, Entity entity, Hierarchy hierarchy, Literal ancestorId) {
    super(LoggerFactory.getLogger(HierarchyHasAncestorFilter.class), underlay, entity);
    this.hierarchy = hierarchy;
    this.ancestorIds = ImmutableList.of(ancestorId);
  }

  public HierarchyHasAncestorFilter(
      Underlay underlay, Entity entity, Hierarchy hierarchy, List<Literal> ancestorIds) {
    super(LoggerFactory.getLogger(HierarchyHasAncestorFilter.class), underlay, entity);
    this.hierarchy = hierarchy;
    this.ancestorIds = ImmutableList.copyOf(ancestorIds);
  }

  public Hierarchy getHierarchy() {
    return hierarchy;
  }

  public ImmutableList<Literal> getAncestorIds() {
    return ancestorIds;
  }

  @Override
  public boolean equals(Object o) {
    if (!super.equals(o)) {
      return false;
    }
    HierarchyHasAncestorFilter that = (HierarchyHasAncestorFilter) o;
    return hierarchy.equals(that.hierarchy) && ancestorIds.equals(that.ancestorIds);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), hierarchy, ancestorIds);
  }
}

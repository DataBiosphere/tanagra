package bio.terra.tanagra.api.filter;

import bio.terra.tanagra.api.shared.Literal;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.entitymodel.Entity;
import bio.terra.tanagra.underlay.entitymodel.Hierarchy;
import com.google.common.collect.ImmutableList;
import java.util.List;
import org.slf4j.LoggerFactory;

public class HierarchyHasParentFilter extends EntityFilter {
  private final Hierarchy hierarchy;
  private final ImmutableList<Literal> parentIds;

  public HierarchyHasParentFilter(
      Underlay underlay, Entity entity, Hierarchy hierarchy, Literal parentId) {
    super(LoggerFactory.getLogger(HierarchyHasParentFilter.class), underlay, entity);
    this.hierarchy = hierarchy;
    this.parentIds = ImmutableList.of(parentId);
  }

  public HierarchyHasParentFilter(
      Underlay underlay, Entity entity, Hierarchy hierarchy, List<Literal> parentIds) {
    super(LoggerFactory.getLogger(HierarchyHasParentFilter.class), underlay, entity);
    this.hierarchy = hierarchy;
    this.parentIds = ImmutableList.copyOf(parentIds);
  }

  public Hierarchy getHierarchy() {
    return hierarchy;
  }

  public ImmutableList<Literal> getParentIds() {
    return parentIds;
  }
}

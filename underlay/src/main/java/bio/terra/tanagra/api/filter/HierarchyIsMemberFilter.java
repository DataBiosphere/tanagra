package bio.terra.tanagra.api.filter;

import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.entitymodel.Entity;
import bio.terra.tanagra.underlay.entitymodel.Hierarchy;
import org.slf4j.LoggerFactory;

public class HierarchyIsMemberFilter extends EntityFilter {
  private final Hierarchy hierarchy;

  public HierarchyIsMemberFilter(Underlay underlay, Entity entity, Hierarchy hierarchy) {
    super(LoggerFactory.getLogger(HierarchyIsMemberFilter.class), underlay, entity);
    this.hierarchy = hierarchy;
  }

  public Hierarchy getHierarchy() {
    return hierarchy;
  }
}

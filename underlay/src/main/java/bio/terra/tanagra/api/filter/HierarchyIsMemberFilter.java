package bio.terra.tanagra.api.filter;

import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.entitymodel.Entity;
import bio.terra.tanagra.underlay.entitymodel.Hierarchy;
import java.util.Objects;
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

  @Override
  public boolean equals(Object o) {
    if (!super.equals(o)) {
      return false;
    }
    return hierarchy.equals(((HierarchyIsMemberFilter) o).hierarchy);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), hierarchy);
  }
}

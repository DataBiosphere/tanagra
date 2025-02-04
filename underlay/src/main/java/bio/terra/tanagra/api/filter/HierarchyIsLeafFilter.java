package bio.terra.tanagra.api.filter;

import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.entitymodel.Entity;
import bio.terra.tanagra.underlay.entitymodel.Hierarchy;
import java.util.Objects;
import org.slf4j.LoggerFactory;

public class HierarchyIsLeafFilter extends EntityFilter {
  private final Hierarchy hierarchy;

  public HierarchyIsLeafFilter(Underlay underlay, Entity entity, Hierarchy hierarchy) {
    super(LoggerFactory.getLogger(HierarchyIsLeafFilter.class), underlay, entity);
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
    return hierarchy.equals(((HierarchyIsLeafFilter) o).hierarchy);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), hierarchy);
  }
}

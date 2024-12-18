package bio.terra.tanagra.api.filter;

import bio.terra.tanagra.underlay.entitymodel.*;
import java.util.List;

public abstract class EntityFilter {
  public abstract Entity getEntity();

  // TODO: Add logic here to merge filters automatically to get a simpler filter overall.
  public boolean isMergeable(EntityFilter entityFilter) {
    return false;
  }

  @SuppressWarnings("PMD.EmptyMethodInAbstractClassShouldBeAbstract")
  public EntityFilter mergeWith(EntityFilter entityFilter) {
    return null;
  }

  public static boolean areSameFilterType(List<EntityFilter> filters) {
    String filterType = filters.get(0).getClass().getName();
    return filters.stream().allMatch(filter -> filter.getClass().getName().equals(filterType));
  }
}

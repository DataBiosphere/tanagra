package bio.terra.tanagra.api.filter;

import bio.terra.tanagra.underlay.entitymodel.*;

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
}

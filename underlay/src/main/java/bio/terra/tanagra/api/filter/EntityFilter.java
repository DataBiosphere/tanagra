package bio.terra.tanagra.api.filter;

public abstract class EntityFilter {
  // TODO: Add logic here to merge filters automatically to get a simpler filter overall.
  public boolean isMergeable(EntityFilter entityFilter) {
    return false;
  }

  @SuppressWarnings("PMD.EmptyMethodInAbstractClassShouldBeAbstract")
  public EntityFilter mergeWith(EntityFilter entityFilter) {
    return null;
  }
}

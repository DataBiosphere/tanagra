package bio.terra.tanagra.api.filter;

import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.entitymodel.Entity;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;

public abstract class EntityFilter {

  private final Logger logger;
  private final Underlay underlay;
  private final Entity entity;

  protected EntityFilter(Logger logger, Underlay underlay, Entity entity) {
    this.logger = logger;
    this.underlay = underlay;
    this.entity = entity;
  }

  protected Logger getLogger() {
    return logger;
  }

  public Underlay getUnderlay() {
    return underlay;
  }

  public Entity getEntity() {
    return entity;
  }

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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    EntityFilter that = (EntityFilter) o;
    return Objects.equals(underlay, that.underlay) && Objects.equals(entity, that.entity);
  }

  @Override
  public int hashCode() {
    return Objects.hash(underlay, entity);
  }
}

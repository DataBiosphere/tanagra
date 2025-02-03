package bio.terra.tanagra.api.filter;

import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.entitymodel.Attribute;
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

  public List<Attribute> getFilterAttributes() {
    // not supported or not implemented
    return List.of();
  }

  public List<String> getFilterAttributeNames() {
    return getFilterAttributes().stream().map(Attribute::getName).toList();
  }

  // TODO: Add logic here to merge filters automatically to get a simpler filter overall.
  // Current filter generation logic is in both UI and Underlay, hence only merge translation
  public boolean isMergeable(EntityFilter entityFilter) {
    return false;
  }

  @SuppressWarnings("PMD.EmptyMethodInAbstractClassShouldBeAbstract")
  public EntityFilter mergeWith(EntityFilter entityFilter) {
    return null;
  }

  public static boolean areSameFilterTypeAndEntity(List<EntityFilter> filters) {
    Class<?> firstClazz = filters.get(0).getClass();
    String firstEntityName = filters.get(0).getEntity().getName();
    return filters.stream()
        .skip(1)
        .allMatch(
            filter ->
                filter.getClass().equals(firstClazz)
                    && filter.getEntity().getName().equals(firstEntityName));
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

package bio.terra.tanagra.filterbuilder;

import bio.terra.tanagra.api.filter.EntityFilter;
import bio.terra.tanagra.underlay.entitymodel.Entity;
import java.util.Objects;
import javax.annotation.Nullable;

public final class EntityOutput {
  private final Entity entity;
  private final @Nullable EntityFilter dataFeatureFilter;

  private EntityOutput(Entity entity, @Nullable EntityFilter dataFeatureFilter) {
    this.entity = entity;
    this.dataFeatureFilter = dataFeatureFilter;
  }

  public static EntityOutput filtered(Entity entity, EntityFilter entityFilter) {
    return new EntityOutput(entity, entityFilter);
  }

  public static EntityOutput unfiltered(Entity entity) {
    return new EntityOutput(entity, null);
  }

  public Entity getEntity() {
    return entity;
  }

  public boolean hasDataFeatureFilter() {
    return dataFeatureFilter != null;
  }

  @Nullable
  public EntityFilter getDataFeatureFilter() {
    return dataFeatureFilter;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    EntityOutput that = (EntityOutput) o;
    return entity.equals(that.entity) && Objects.equals(dataFeatureFilter, that.dataFeatureFilter);
  }

  @Override
  public int hashCode() {
    return Objects.hash(entity, dataFeatureFilter);
  }
}

package bio.terra.tanagra.filterbuilder;

import bio.terra.tanagra.api.filter.EntityFilter;
import bio.terra.tanagra.underlay.entitymodel.Entity;
import java.util.Objects;
import javax.annotation.Nullable;

public final class EntityOutput {
  private final Entity entity;
  private final @Nullable EntityFilter entityFilter;

  private EntityOutput(Entity entity, @Nullable EntityFilter entityFilter) {
    this.entity = entity;
    this.entityFilter = entityFilter;
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

  @Nullable
  public EntityFilter getEntityFilter() {
    return entityFilter;
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
    return entity.equals(that.entity) && Objects.equals(entityFilter, that.entityFilter);
  }

  @Override
  public int hashCode() {
    return Objects.hash(entity, entityFilter);
  }
}

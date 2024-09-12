package bio.terra.tanagra.filterbuilder;

import bio.terra.tanagra.api.filter.EntityFilter;
import bio.terra.tanagra.underlay.entitymodel.Attribute;
import bio.terra.tanagra.underlay.entitymodel.Entity;
import com.google.common.collect.ImmutableList;
import jakarta.annotation.Nullable;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public final class EntityOutput {
  private final Entity entity;
  private final @Nullable EntityFilter dataFeatureFilter;
  private final ImmutableList<Attribute> attributes;

  private EntityOutput(
      Entity entity, @Nullable EntityFilter dataFeatureFilter, List<Attribute> attributes) {
    this.entity = entity;
    this.dataFeatureFilter = dataFeatureFilter;

    // Order the attributes consistently, same as defined in the entity config file.
    this.attributes =
        ImmutableList.copyOf(
            attributes.stream()
                .sorted(Comparator.comparingInt(entity.getAttributes()::indexOf))
                .toList());
  }

  public static EntityOutput filtered(Entity entity, EntityFilter entityFilter) {
    return new EntityOutput(entity, entityFilter, entity.getAttributes());
  }

  public static EntityOutput filtered(
      Entity entity, EntityFilter entityFilter, List<Attribute> attributes) {
    return new EntityOutput(entity, entityFilter, attributes);
  }

  public static EntityOutput unfiltered(Entity entity) {
    return new EntityOutput(entity, null, entity.getAttributes());
  }

  public static EntityOutput unfiltered(Entity entity, List<Attribute> attributes) {
    return new EntityOutput(entity, null, attributes);
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

  public List<Attribute> getAttributes() {
    return attributes;
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
    return entity.equals(that.entity)
        && Objects.equals(dataFeatureFilter, that.dataFeatureFilter)
        && attributes.equals(that.attributes);
  }

  @Override
  public int hashCode() {
    return Objects.hash(entity, dataFeatureFilter, attributes);
  }
}

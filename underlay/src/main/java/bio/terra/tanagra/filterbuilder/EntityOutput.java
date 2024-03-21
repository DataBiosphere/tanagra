package bio.terra.tanagra.filterbuilder;

import bio.terra.tanagra.api.filter.EntityFilter;
import bio.terra.tanagra.underlay.entitymodel.Attribute;
import bio.terra.tanagra.underlay.entitymodel.Entity;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nullable;

public final class EntityOutput {
  private final Entity entity;
  private final @Nullable EntityFilter dataFeatureFilter;
  private final ImmutableSet<Attribute> attributes;

  private EntityOutput(
      Entity entity, @Nullable EntityFilter dataFeatureFilter, Set<Attribute> attributes) {
    this.entity = entity;
    this.dataFeatureFilter = dataFeatureFilter;
    this.attributes = ImmutableSet.copyOf(attributes);
  }

  public static EntityOutput filtered(Entity entity, EntityFilter entityFilter) {
    return new EntityOutput(entity, entityFilter, new HashSet<>(entity.getAttributes()));
  }

  public static EntityOutput filtered(
      Entity entity, EntityFilter entityFilter, List<Attribute> attributes) {
    return new EntityOutput(entity, entityFilter, new HashSet<>(attributes));
  }

  public static EntityOutput unfiltered(Entity entity) {
    return new EntityOutput(entity, null, new HashSet<>(entity.getAttributes()));
  }

  public static EntityOutput unfiltered(Entity entity, List<Attribute> attributes) {
    return new EntityOutput(entity, null, new HashSet<>(attributes));
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
    return ImmutableList.copyOf(attributes);
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

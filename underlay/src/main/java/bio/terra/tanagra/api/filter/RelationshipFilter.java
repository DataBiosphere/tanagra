package bio.terra.tanagra.api.filter;

import bio.terra.tanagra.api.shared.BinaryOperator;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.entitymodel.Attribute;
import bio.terra.tanagra.underlay.entitymodel.Entity;
import bio.terra.tanagra.underlay.entitymodel.Relationship;
import bio.terra.tanagra.underlay.entitymodel.entitygroup.EntityGroup;
import com.google.common.collect.ImmutableList;
import jakarta.annotation.Nullable;
import java.util.List;
import java.util.Objects;

public class RelationshipFilter extends EntityFilter {
  private final Underlay underlay;
  private final EntityGroup entityGroup;
  private final Entity selectEntity;
  private final Entity filterEntity;
  private final Relationship relationship;
  private final @Nullable EntityFilter subFilter;
  private final @Nullable List<Attribute> groupByCountAttributes;
  private final @Nullable BinaryOperator groupByCountOperator;
  private final @Nullable Integer groupByCountValue;

  @SuppressWarnings("checkstyle:ParameterNumber")
  public RelationshipFilter(
      Underlay underlay,
      EntityGroup entityGroup,
      Entity selectEntity,
      Relationship relationship,
      @Nullable EntityFilter subFilter,
      @Nullable List<Attribute> groupByCountAttributes,
      @Nullable BinaryOperator groupByCountOperator,
      @Nullable Integer groupByCountValue) {
    this.underlay = underlay;
    this.entityGroup = entityGroup;
    this.selectEntity = selectEntity;
    this.filterEntity =
        relationship.getEntityA().equals(selectEntity)
            ? relationship.getEntityB()
            : relationship.getEntityA();
    this.relationship = relationship;
    this.subFilter = subFilter;
    this.groupByCountAttributes =
        groupByCountAttributes == null
            ? ImmutableList.of()
            : ImmutableList.copyOf(groupByCountAttributes);
    this.groupByCountOperator = groupByCountOperator;
    this.groupByCountValue = groupByCountValue;
  }

  public Underlay getUnderlay() {
    return underlay;
  }

  public EntityGroup getEntityGroup() {
    return entityGroup;
  }

  public Entity getSelectEntity() {
    return selectEntity;
  }

  public Entity getFilterEntity() {
    return filterEntity;
  }

  public Relationship getRelationship() {
    return relationship;
  }

  public boolean hasSubFilter() {
    return subFilter != null;
  }

  public EntityFilter getSubFilter() {
    return subFilter;
  }

  public boolean hasGroupByCountAttributes() {
    return !groupByCountAttributes.isEmpty();
  }

  public boolean hasGroupByFilter() {
    return groupByCountOperator != null && groupByCountValue != null;
  }

  public List<Attribute> getGroupByCountAttributes() {
    return groupByCountAttributes;
  }

  @Nullable
  public BinaryOperator getGroupByCountOperator() {
    return groupByCountOperator;
  }

  @Nullable
  public Integer getGroupByCountValue() {
    return groupByCountValue;
  }

  public boolean isForeignKeyOnSelectTable() {
    return relationship.isForeignKeyAttribute(selectEntity);
  }

  public boolean isForeignKeyOnFilterTable() {
    return relationship.isForeignKeyAttribute(filterEntity);
  }

  @Override
  public Entity getEntity() {
    return getSelectEntity();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RelationshipFilter that = (RelationshipFilter) o;
    return underlay.equals(that.underlay)
        && entityGroup.equals(that.entityGroup)
        && selectEntity.equals(that.selectEntity)
        && filterEntity.equals(that.filterEntity)
        && relationship.equals(that.relationship)
        && Objects.equals(subFilter, that.subFilter)
        && Objects.equals(groupByCountAttributes, that.groupByCountAttributes)
        && groupByCountOperator == that.groupByCountOperator
        && Objects.equals(groupByCountValue, that.groupByCountValue);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        underlay,
        entityGroup,
        selectEntity,
        filterEntity,
        relationship,
        subFilter,
        groupByCountAttributes,
        groupByCountOperator,
        groupByCountValue);
  }
}

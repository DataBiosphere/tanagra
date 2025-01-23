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
import org.slf4j.LoggerFactory;

public class RelationshipFilter extends EntityFilter {
  private final EntityGroup entityGroup;
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
    super(LoggerFactory.getLogger(RelationshipFilter.class), underlay, selectEntity);
    this.entityGroup = entityGroup;
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

  @Override
  public List<Attribute> getFilterAttributes() {
    Attribute attribute = relationship.getForeignKeyAttribute(getSelectEntity());
    return attribute != null ? List.of(attribute) : List.of();
  }

  public EntityGroup getEntityGroup() {
    return entityGroup;
  }

  public Entity getSelectEntity() {
    return getEntity();
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

  @Nullable
  public EntityFilter getSubFilter() {
    return subFilter;
  }

  public boolean hasGroupByCountAttributes() {
    return !groupByCountAttributes.isEmpty();
  }

  public boolean hasGroupByFilter() {
    return groupByCountOperator != null && groupByCountValue != null;
  }

  @Nullable
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
    return relationship.isForeignKeyAttribute(getSelectEntity());
  }

  public boolean isForeignKeyOnFilterTable() {
    return relationship.isForeignKeyAttribute(filterEntity);
  }

  @Override
  public boolean equals(Object o) {
    if (!super.equals(o)) {
      return false;
    }
    RelationshipFilter that = (RelationshipFilter) o;
    return entityGroup.equals(that.entityGroup)
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
        super.hashCode(),
        entityGroup,
        filterEntity,
        relationship,
        subFilter,
        groupByCountAttributes,
        groupByCountOperator,
        groupByCountValue);
  }
}

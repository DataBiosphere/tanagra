package bio.terra.tanagra.api.filter;

import bio.terra.tanagra.query.filtervariable.BinaryFilterVariable;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.entitymodel.Attribute;
import bio.terra.tanagra.underlay.entitymodel.Entity;
import bio.terra.tanagra.underlay.entitymodel.Relationship;
import bio.terra.tanagra.underlay.entitymodel.entitygroup.EntityGroup;
import javax.annotation.Nullable;

public class RelationshipFilter extends EntityFilter {
  private final Underlay underlay;
  private final EntityGroup entityGroup;
  private final Entity selectEntity;
  private final Entity filterEntity;
  private final Relationship relationship;
  private final EntityFilter subFilter;
  private final @Nullable Attribute groupByCountAttribute;
  private final @Nullable BinaryFilterVariable.BinaryOperator groupByCountOperator;
  private final @Nullable Integer groupByCountValue;

  @SuppressWarnings("checkstyle:ParameterNumber")
  public RelationshipFilter(
      Underlay underlay,
      EntityGroup entityGroup,
      Entity selectEntity,
      Relationship relationship,
      EntityFilter subFilter,
      @Nullable Attribute groupByCountAttribute,
      @Nullable BinaryFilterVariable.BinaryOperator groupByCountOperator,
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
    this.groupByCountAttribute = groupByCountAttribute;
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

  public EntityFilter getSubFilter() {
    return subFilter;
  }

  public boolean hasGroupByCountAttribute() {
    return groupByCountAttribute != null;
  }

  public boolean hasGroupByFilter() {
    return groupByCountOperator != null && groupByCountValue != null;
  }

  public Attribute getGroupByCountAttribute() {
    return groupByCountAttribute;
  }

  @Nullable
  public BinaryFilterVariable.BinaryOperator getGroupByCountOperator() {
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
}

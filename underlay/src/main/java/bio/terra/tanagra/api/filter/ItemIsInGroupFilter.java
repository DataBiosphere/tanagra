package bio.terra.tanagra.api.filter;

import bio.terra.tanagra.query.FilterVariable;
import bio.terra.tanagra.query.TableVariable;
import bio.terra.tanagra.query.filtervariable.BinaryFilterVariable;
import bio.terra.tanagra.underlay.entitymodel.Attribute;
import bio.terra.tanagra.underlay.entitymodel.entitygroup.GroupItems;
import java.util.List;
import javax.annotation.Nullable;

public class ItemIsInGroupFilter extends EntityFilter {
  private final GroupItems entityGroup;
  private final EntityFilter subFilter;
  private final @Nullable Attribute groupByCountAttribute;
  private final @Nullable BinaryFilterVariable.BinaryOperator groupByCountOperator;
  private final @Nullable Integer groupByCountValue;

  public ItemIsInGroupFilter(
      GroupItems entityGroup,
      EntityFilter subFilter,
      @Nullable Attribute groupByCountAttribute,
      @Nullable BinaryFilterVariable.BinaryOperator groupByCountOperator,
      @Nullable Integer groupByCountValue) {
    this.entityGroup = entityGroup;
    this.subFilter = subFilter;
    this.groupByCountAttribute = groupByCountAttribute;
    this.groupByCountOperator = groupByCountOperator;
    this.groupByCountValue = groupByCountValue;
  }

  public GroupItems getEntityGroup() {
    return entityGroup;
  }

  public EntityFilter getSubFilter() {
    return subFilter;
  }

  @Nullable
  public Attribute getGroupByCountAttribute() {
    return groupByCountAttribute;
  }

  public boolean hasGroupByCountAttribute() {
    return groupByCountAttribute != null;
  }

  @Nullable
  public BinaryFilterVariable.BinaryOperator getGroupByCountOperator() {
    return groupByCountOperator;
  }

  @Nullable
  public Integer getGroupByCountValue() {
    return groupByCountValue;
  }

  public boolean hasGroupByCount() {
    return groupByCountOperator != null && groupByCountValue != null;
  }

  @Override
  public FilterVariable getFilterVariable(
      TableVariable entityTableVar, List<TableVariable> tableVars) {
    return null;
  }
}

package bio.terra.tanagra.api.filter;

import bio.terra.tanagra.api.shared.BinaryOperator;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.entitymodel.Attribute;
import bio.terra.tanagra.underlay.entitymodel.entitygroup.GroupItems;
import javax.annotation.Nullable;

public class ItemInGroupFilter extends EntityFilter {
  private final Underlay underlay;
  private final GroupItems groupItems;
  private final EntityFilter groupSubFilter;
  private final @Nullable Attribute groupByCountAttribute;
  private final @Nullable BinaryOperator groupByCountOperator;
  private final @Nullable Integer groupByCountValue;

  public ItemInGroupFilter(
      Underlay underlay,
      GroupItems groupItems,
      EntityFilter groupSubFilter,
      @Nullable Attribute groupByCountAttribute,
      @Nullable BinaryOperator groupByCountOperator,
      @Nullable Integer groupByCountValue) {
    this.underlay = underlay;
    this.groupItems = groupItems;
    this.groupSubFilter = groupSubFilter;
    this.groupByCountAttribute = groupByCountAttribute;
    this.groupByCountOperator = groupByCountOperator;
    this.groupByCountValue = groupByCountValue;
  }

  public Underlay getUnderlay() {
    return underlay;
  }

  public GroupItems getGroupItems() {
    return groupItems;
  }

  public EntityFilter getGroupSubFilter() {
    return groupSubFilter;
  }

  @Nullable
  public Attribute getGroupByCountAttribute() {
    return groupByCountAttribute;
  }

  @Nullable
  public BinaryOperator getGroupByCountOperator() {
    return groupByCountOperator;
  }

  @Nullable
  public Integer getGroupByCountValue() {
    return groupByCountValue;
  }
}

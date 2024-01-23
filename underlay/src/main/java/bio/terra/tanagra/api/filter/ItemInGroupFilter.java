package bio.terra.tanagra.api.filter;

import bio.terra.tanagra.api.shared.BinaryOperator;
import bio.terra.tanagra.api.shared.Literal;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.entitymodel.Attribute;
import bio.terra.tanagra.underlay.entitymodel.entitygroup.GroupItems;
import javax.annotation.Nullable;

public class ItemInGroupFilter extends EntityFilter {
  private final Underlay underlay;
  private final GroupItems groupItems;
  private final Literal groupId;
  private final @Nullable Attribute groupByCountAttribute;
  private final @Nullable BinaryOperator groupByCountOperator;
  private final @Nullable Integer groupByCountValue;

  public ItemInGroupFilter(
      Underlay underlay,
      GroupItems groupItems,
      Literal groupId,
      @Nullable Attribute groupByCountAttribute,
      @Nullable BinaryOperator groupByCountOperator,
      @Nullable Integer groupByCountValue) {
    this.underlay = underlay;
    this.groupItems = groupItems;
    this.groupId = groupId;
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

  public Literal getGroupId() {
    return groupId;
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

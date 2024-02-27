package bio.terra.tanagra.api.filter;

import bio.terra.tanagra.api.shared.BinaryOperator;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.entitymodel.Attribute;
import bio.terra.tanagra.underlay.entitymodel.entitygroup.GroupItems;
import java.util.Objects;
import javax.annotation.Nullable;

public class GroupHasItemsFilter extends EntityFilter {
  private final Underlay underlay;
  private final GroupItems groupItems;
  private final EntityFilter itemsSubFilter;
  private final @Nullable Attribute groupByCountAttribute;
  private final @Nullable BinaryOperator groupByCountOperator;
  private final @Nullable Integer groupByCountValue;

  public GroupHasItemsFilter(
      Underlay underlay,
      GroupItems groupItems,
      @Nullable EntityFilter itemsSubFilter,
      @Nullable Attribute groupByCountAttribute,
      @Nullable BinaryOperator groupByCountOperator,
      @Nullable Integer groupByCountValue) {
    this.underlay = underlay;
    this.groupItems = groupItems;
    this.itemsSubFilter = itemsSubFilter;
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

  public EntityFilter getItemsSubFilter() {
    return itemsSubFilter;
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    GroupHasItemsFilter that = (GroupHasItemsFilter) o;
    return underlay.equals(that.underlay)
        && groupItems.equals(that.groupItems)
        && Objects.equals(itemsSubFilter, that.itemsSubFilter)
        && Objects.equals(groupByCountAttribute, that.groupByCountAttribute)
        && groupByCountOperator == that.groupByCountOperator
        && Objects.equals(groupByCountValue, that.groupByCountValue);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        underlay,
        groupItems,
        itemsSubFilter,
        groupByCountAttribute,
        groupByCountOperator,
        groupByCountValue);
  }
}

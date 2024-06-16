package bio.terra.tanagra.api.filter;

import bio.terra.tanagra.api.shared.BinaryOperator;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.entitymodel.Attribute;
import bio.terra.tanagra.underlay.entitymodel.entitygroup.GroupItems;
import com.google.common.collect.ImmutableList;
import jakarta.annotation.Nullable;
import java.util.List;
import java.util.Objects;

public class ItemInGroupFilter extends EntityFilter {
  private final Underlay underlay;
  private final GroupItems groupItems;
  private final @Nullable EntityFilter groupSubFilter;
  private final @Nullable List<Attribute> groupByCountAttributes;
  private final @Nullable BinaryOperator groupByCountOperator;
  private final @Nullable Integer groupByCountValue;

  public ItemInGroupFilter(
      Underlay underlay,
      GroupItems groupItems,
      @Nullable EntityFilter groupSubFilter,
      @Nullable List<Attribute> groupByCountAttributes,
      @Nullable BinaryOperator groupByCountOperator,
      @Nullable Integer groupByCountValue) {
    this.underlay = underlay;
    this.groupItems = groupItems;
    this.groupSubFilter = groupSubFilter;
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

  public GroupItems getGroupItems() {
    return groupItems;
  }

  public EntityFilter getGroupSubFilter() {
    return groupSubFilter;
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ItemInGroupFilter that = (ItemInGroupFilter) o;
    return underlay.equals(that.underlay)
        && groupItems.equals(that.groupItems)
        && Objects.equals(groupSubFilter, that.groupSubFilter)
        && Objects.equals(groupByCountAttributes, that.groupByCountAttributes)
        && groupByCountOperator == that.groupByCountOperator
        && Objects.equals(groupByCountValue, that.groupByCountValue);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        underlay,
        groupItems,
        groupSubFilter,
        groupByCountAttributes,
        groupByCountOperator,
        groupByCountValue);
  }
}

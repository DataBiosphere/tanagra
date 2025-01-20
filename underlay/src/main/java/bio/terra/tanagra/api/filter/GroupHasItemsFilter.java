package bio.terra.tanagra.api.filter;

import bio.terra.tanagra.api.shared.BinaryOperator;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.entitymodel.Attribute;
import bio.terra.tanagra.underlay.entitymodel.entitygroup.GroupItems;
import com.google.common.collect.ImmutableList;
import jakarta.annotation.Nullable;
import java.util.List;
import java.util.Objects;
import org.slf4j.LoggerFactory;

public class GroupHasItemsFilter extends EntityFilter {
  private final GroupItems groupItems;
  private final EntityFilter itemsSubFilter;
  private final @Nullable List<Attribute> groupByCountAttributes;
  private final @Nullable BinaryOperator groupByCountOperator;
  private final @Nullable Integer groupByCountValue;

  public GroupHasItemsFilter(
      Underlay underlay,
      GroupItems groupItems,
      @Nullable EntityFilter itemsSubFilter,
      @Nullable List<Attribute> groupByCountAttributes,
      @Nullable BinaryOperator groupByCountOperator,
      @Nullable Integer groupByCountValue) {
    super(
        LoggerFactory.getLogger(GroupHasItemsFilter.class), underlay, groupItems.getGroupEntity());
    this.groupItems = groupItems;
    this.itemsSubFilter = itemsSubFilter;
    this.groupByCountAttributes =
        groupByCountAttributes == null
            ? ImmutableList.of()
            : ImmutableList.copyOf(groupByCountAttributes);
    this.groupByCountOperator = groupByCountOperator;
    this.groupByCountValue = groupByCountValue;
  }

  public GroupItems getGroupItems() {
    return groupItems;
  }

  public EntityFilter getItemsSubFilter() {
    return itemsSubFilter;
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
    if (!super.equals(o)) {
      return false;
    }
    GroupHasItemsFilter that = (GroupHasItemsFilter) o;
    return groupItems.equals(that.groupItems)
        && Objects.equals(itemsSubFilter, that.itemsSubFilter)
        && Objects.equals(groupByCountAttributes, that.groupByCountAttributes)
        && groupByCountOperator == that.groupByCountOperator
        && Objects.equals(groupByCountValue, that.groupByCountValue);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        super.hashCode(),
        groupItems,
        itemsSubFilter,
        groupByCountAttributes,
        groupByCountOperator,
        groupByCountValue);
  }
}

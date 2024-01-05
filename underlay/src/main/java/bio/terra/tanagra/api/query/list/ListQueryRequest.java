package bio.terra.tanagra.api.query.list;

import bio.terra.tanagra.api.field.ValueDisplayField;
import bio.terra.tanagra.api.filter.EntityFilter;
import bio.terra.tanagra.api.query.PageMarker;
import bio.terra.tanagra.api.shared.OrderByDirection;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.entitymodel.Entity;
import com.google.common.collect.ImmutableList;
import java.util.List;
import javax.annotation.Nullable;

public class ListQueryRequest {
  private static final Integer DEFAULT_PAGE_SIZE = 250;

  private final Underlay underlay;
  private final Entity entity;
  private final ImmutableList<ValueDisplayField> selectFields;
  private final @Nullable EntityFilter filter;
  private final ImmutableList<OrderBy> orderBys;
  private final @Nullable Integer limit;
  private final @Nullable PageMarker pageMarker;
  private final Integer pageSize;
  private final boolean isDryRun;

  @SuppressWarnings("checkstyle:ParameterNumber")
  public ListQueryRequest(
      Underlay underlay,
      Entity entity,
      List<ValueDisplayField> selectFields,
      @Nullable EntityFilter filter,
      @Nullable List<OrderBy> orderBys,
      @Nullable Integer limit,
      @Nullable PageMarker pageMarker,
      @Nullable Integer pageSize,
      boolean isDryRun) {
    this.underlay = underlay;
    this.entity = entity;
    this.selectFields =
        selectFields == null ? ImmutableList.of() : ImmutableList.copyOf(selectFields);
    this.filter = filter;
    this.orderBys = orderBys == null ? ImmutableList.of() : ImmutableList.copyOf(orderBys);
    this.limit = limit;
    this.pageMarker = pageMarker;
    this.pageSize = (pageMarker == null && pageSize == null) ? DEFAULT_PAGE_SIZE : pageSize;
    this.isDryRun = isDryRun;
  }

  public Underlay getUnderlay() {
    return underlay;
  }

  public Entity getEntity() {
    return entity;
  }

  public ImmutableList<ValueDisplayField> getSelectFields() {
    return selectFields;
  }

  public EntityFilter getFilter() {
    return filter;
  }

  public ImmutableList<OrderBy> getOrderBys() {
    return orderBys;
  }

  public Integer getLimit() {
    return limit;
  }

  public PageMarker getPageMarker() {
    return pageMarker;
  }

  public Integer getPageSize() {
    return pageSize;
  }

  public boolean isDryRun() {
    return isDryRun;
  }

  public ListQueryRequest cloneAndSetDryRun() {
    return new ListQueryRequest(
        underlay, entity, selectFields, filter, orderBys, limit, pageMarker, pageSize, true);
  }

  public static class OrderBy {
    private final @Nullable ValueDisplayField valueDisplayField;
    private final @Nullable OrderByDirection direction;
    private final boolean isRandom;

    public OrderBy(ValueDisplayField valueDisplayField, OrderByDirection direction) {
      this(valueDisplayField, direction, false);
    }

    public static OrderBy random() {
      return new OrderBy(null, null, true);
    }

    private OrderBy(
        @Nullable ValueDisplayField valueDisplayField,
        @Nullable OrderByDirection direction,
        boolean isRandom) {
      this.valueDisplayField = valueDisplayField;
      this.direction = direction;
      this.isRandom = isRandom;
    }

    public ValueDisplayField getEntityField() {
      return valueDisplayField;
    }

    public OrderByDirection getDirection() {
      return direction;
    }

    public boolean isRandom() {
      return isRandom;
    }
  }
}

package bio.terra.tanagra.api2.query.list;

import bio.terra.tanagra.api2.field.ValueDisplayField;
import bio.terra.tanagra.api2.filter.EntityFilter;
import bio.terra.tanagra.query.OrderByDirection;
import bio.terra.tanagra.query.PageMarker;
import bio.terra.tanagra.underlay2.Underlay;
import bio.terra.tanagra.underlay2.entitymodel.Entity;
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

  @SuppressWarnings("checkstyle:ParameterNumber")
  public ListQueryRequest(
      Underlay underlay,
      Entity entity,
      List<ValueDisplayField> selectFields,
      @Nullable EntityFilter filter,
      @Nullable List<OrderBy> orderBys,
      @Nullable Integer limit,
      @Nullable PageMarker pageMarker,
      @Nullable Integer pageSize) {
    this.underlay = underlay;
    this.entity = entity;
    this.selectFields =
        selectFields == null ? ImmutableList.of() : ImmutableList.copyOf(selectFields);
    this.filter = filter;
    this.orderBys = orderBys == null ? ImmutableList.of() : ImmutableList.copyOf(orderBys);
    this.limit = limit;
    this.pageMarker = pageMarker;
    this.pageSize = (pageMarker == null && pageSize == null) ? DEFAULT_PAGE_SIZE : pageSize;
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

  public static class OrderBy {
    private final ValueDisplayField valueDisplayField;
    private final OrderByDirection direction;

    public OrderBy(ValueDisplayField valueDisplayField, OrderByDirection direction) {
      this.valueDisplayField = valueDisplayField;
      this.direction = direction;
    }

    public ValueDisplayField getEntityField() {
      return valueDisplayField;
    }

    public OrderByDirection getDirection() {
      return direction;
    }
  }
}

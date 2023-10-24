package bio.terra.tanagra.api2.query.list;

import bio.terra.tanagra.api.query.filter.EntityFilter;
import bio.terra.tanagra.api2.field.EntityField;
import bio.terra.tanagra.query.*;
import bio.terra.tanagra.underlay2.Entity;
import com.google.common.collect.ImmutableList;
import java.util.List;
import javax.annotation.Nullable;

public class ListQueryRequest {
  private static final Integer DEFAULT_PAGE_SIZE = 250;

  private final Entity entity;
  private final ImmutableList<EntityField> selectFields;
  private final @Nullable EntityFilter filter;
  private final ImmutableList<OrderBy> orderBys;
  private final @Nullable Integer limit;
  private final @Nullable PageMarker pageMarker;
  private final Integer pageSize;

  public ListQueryRequest(
      Entity entity,
      List<EntityField> selectFields,
      @Nullable EntityFilter filter,
      @Nullable List<OrderBy> orderBys,
      @Nullable Integer limit,
      @Nullable PageMarker pageMarker,
      @Nullable Integer pageSize) {
    this.entity = entity;
    this.selectFields =
        selectFields == null ? ImmutableList.of() : ImmutableList.copyOf(selectFields);
    this.filter = filter;
    this.orderBys = orderBys == null ? ImmutableList.of() : ImmutableList.copyOf(orderBys);
    this.limit = limit;
    this.pageMarker = pageMarker;
    this.pageSize = (pageMarker == null && pageSize == null) ? DEFAULT_PAGE_SIZE : pageSize;
  }

  public Entity getEntity() {
    return entity;
  }

  public ImmutableList<EntityField> getSelectFields() {
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
    private final EntityField entityField;
    private final OrderByDirection direction;

    public OrderBy(EntityField entityField, OrderByDirection direction) {
      this.entityField = entityField;
      this.direction = direction;
    }

    public EntityField getEntityField() {
      return entityField;
    }

    public OrderByDirection getDirection() {
      return direction;
    }
  }
}

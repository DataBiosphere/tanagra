package bio.terra.tanagra.api.query.list;

import bio.terra.tanagra.api.field.ValueDisplayField;
import bio.terra.tanagra.api.filter.EntityFilter;
import bio.terra.tanagra.api.query.PageMarker;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.entitymodel.Entity;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import jakarta.annotation.Nullable;
import java.util.List;

public final class ListQueryRequest {
  @VisibleForTesting public static final Integer DEFAULT_PAGE_SIZE = 250;

  private final Underlay underlay;
  private final Entity entity;
  private final ImmutableList<ValueDisplayField> selectFields;
  private final @Nullable EntityFilter filter;
  private final ImmutableList<OrderBy> orderBys;
  private final @Nullable Integer limit;
  private final @Nullable PageMarker pageMarker;
  private final Integer pageSize;
  private final boolean isDryRun;
  private final boolean isAgainstSourceData;

  @SuppressWarnings({"checkstyle:ParameterNumber", "PMD.ExcessiveParameterList"})
  private ListQueryRequest(
      Underlay underlay,
      Entity entity,
      List<ValueDisplayField> selectFields,
      @Nullable EntityFilter filter,
      @Nullable List<OrderBy> orderBys,
      @Nullable Integer limit,
      @Nullable PageMarker pageMarker,
      @Nullable Integer pageSize,
      boolean isDryRun,
      boolean isAgainstSourceData) {
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
    this.isAgainstSourceData = isAgainstSourceData;
  }

  @SuppressWarnings("checkstyle:ParameterNumber")
  public static ListQueryRequest againstIndexData(
      Underlay underlay,
      Entity entity,
      List<ValueDisplayField> selectFields,
      @Nullable EntityFilter filter,
      @Nullable List<OrderBy> orderBys,
      @Nullable Integer limit,
      @Nullable PageMarker pageMarker,
      @Nullable Integer pageSize) {
    return new ListQueryRequest(
        underlay,
        entity,
        selectFields,
        filter,
        orderBys,
        limit,
        pageMarker,
        pageSize,
        false,
        false);
  }

  public static ListQueryRequest dryRunAgainstIndexData(
      Underlay underlay,
      Entity entity,
      List<ValueDisplayField> selectFields,
      @Nullable EntityFilter filter,
      @Nullable List<OrderBy> orderBys,
      @Nullable Integer limit) {
    return new ListQueryRequest(
        underlay, entity, selectFields, filter, orderBys, limit, null, null, true, false);
  }

  public static ListQueryRequest dryRunAgainstSourceData(
      Underlay underlay,
      Entity entity,
      List<ValueDisplayField> selectFields,
      @Nullable EntityFilter filter) {
    return new ListQueryRequest(
        underlay, entity, selectFields, filter, null, null, null, null, true, true);
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

  public boolean isAgainstSourceData() {
    return isAgainstSourceData;
  }
}

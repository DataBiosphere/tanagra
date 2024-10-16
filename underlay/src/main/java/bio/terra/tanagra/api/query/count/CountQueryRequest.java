package bio.terra.tanagra.api.query.count;

import bio.terra.tanagra.api.field.ValueDisplayField;
import bio.terra.tanagra.api.filter.EntityFilter;
import bio.terra.tanagra.api.query.PageMarker;
import bio.terra.tanagra.api.query.hint.HintQueryResult;
import bio.terra.tanagra.api.shared.OrderByDirection;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.entitymodel.*;
import com.google.common.collect.ImmutableList;
import jakarta.annotation.Nullable;
import java.util.List;

public class CountQueryRequest {
  // Choose a very large default page size for count queries because, unlike list queries, we expect
  // callers to always paginate through all results.
  private static final Integer DEFAULT_PAGE_SIZE = 20_000;

  private final Underlay underlay;
  private final Entity entity;
  private final Attribute countDistinctAttribute;
  private final ImmutableList<ValueDisplayField> groupByFields;
  private final EntityFilter filter;
  private final OrderByDirection orderByDirection;
  private final Integer limit;
  private final PageMarker pageMarker;
  private final Integer pageSize;
  private final HintQueryResult entityLevelHints;
  private final boolean isDryRun;

  @SuppressWarnings({"checkstyle:ParameterNumber", "PMD.ExcessiveParameterList"})
  public CountQueryRequest(
      Underlay underlay,
      Entity entity,
      @Nullable Attribute countDistinctAttribute,
      List<ValueDisplayField> groupByFields,
      @Nullable EntityFilter filter,
      OrderByDirection orderByDirection,
      @Nullable Integer limit,
      @Nullable PageMarker pageMarker,
      @Nullable Integer pageSize,
      @Nullable HintQueryResult entityLevelHints,
      boolean isDryRun) {
    this.underlay = underlay;
    this.entity = entity;
    this.countDistinctAttribute = countDistinctAttribute;
    this.groupByFields = ImmutableList.copyOf(groupByFields);
    this.filter = filter;
    this.orderByDirection = orderByDirection;
    this.limit = limit;
    this.pageMarker = pageMarker;
    this.pageSize = (pageMarker == null && pageSize == null) ? DEFAULT_PAGE_SIZE : pageSize;
    this.entityLevelHints = entityLevelHints;
    this.isDryRun = isDryRun;
  }

  public Underlay getUnderlay() {
    return underlay;
  }

  public Entity getEntity() {
    return entity;
  }

  public Attribute getCountDistinctAttribute() {
    return countDistinctAttribute == null ? entity.getIdAttribute() : countDistinctAttribute;
  }

  public ImmutableList<ValueDisplayField> getGroupByFields() {
    return groupByFields;
  }

  public @Nullable EntityFilter getFilter() {
    return filter;
  }

  public OrderByDirection getOrderByDirection() {
    return orderByDirection;
  }

  public @Nullable Integer getLimit() {
    return limit;
  }

  public @Nullable PageMarker getPageMarker() {
    return pageMarker;
  }

  public @Nullable Integer getPageSize() {
    return pageSize;
  }

  public @Nullable HintQueryResult getEntityLevelHints() {
    return entityLevelHints;
  }

  public boolean isDryRun() {
    return isDryRun;
  }
}

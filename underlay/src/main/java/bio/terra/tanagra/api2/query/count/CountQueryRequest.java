package bio.terra.tanagra.api2.query.count;

import bio.terra.tanagra.api.query.filter.EntityFilter;
import bio.terra.tanagra.api2.field.EntityField;
import bio.terra.tanagra.query.PageMarker;
import bio.terra.tanagra.underlay2.Entity;
import com.google.common.collect.ImmutableList;
import java.util.List;

public class CountQueryRequest {
  private static final Integer DEFAULT_PAGE_SIZE = 250;

  private final Entity entity;
  private final ImmutableList<EntityField> groupByFields;
  private final EntityFilter filter;
  private final PageMarker pageMarker;
  private final Integer pageSize;

  public CountQueryRequest(
      Entity entity,
      List<EntityField> groupByFields,
      EntityFilter filter,
      PageMarker pageMarker,
      Integer pageSize) {
    this.entity = entity;
    this.groupByFields = ImmutableList.copyOf(groupByFields);
    this.filter = filter;
    this.pageMarker = pageMarker;
    this.pageSize = (pageMarker == null && pageSize == null) ? DEFAULT_PAGE_SIZE : pageSize;
  }

  public Entity getEntity() {
    return entity;
  }

  public ImmutableList<EntityField> getGroupByFields() {
    return groupByFields;
  }

  public EntityFilter getFilter() {
    return filter;
  }

  public PageMarker getPageMarker() {
    return pageMarker;
  }

  public Integer getPageSize() {
    return pageSize;
  }
}

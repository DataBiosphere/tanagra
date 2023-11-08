package bio.terra.tanagra.api2.query.count;

import bio.terra.tanagra.api2.field.ValueDisplayField;
import bio.terra.tanagra.api2.filter.EntityFilter;
import bio.terra.tanagra.query.PageMarker;
import bio.terra.tanagra.underlay2.Underlay;
import bio.terra.tanagra.underlay2.entitymodel.Entity;
import com.google.common.collect.ImmutableList;
import java.util.List;

public class CountQueryRequest {
  private static final Integer DEFAULT_PAGE_SIZE = 250;

  private final Underlay underlay;
  private final Entity entity;
  private final ImmutableList<ValueDisplayField> groupByFields;
  private final EntityFilter filter;
  private final PageMarker pageMarker;
  private final Integer pageSize;

  public CountQueryRequest(
      Underlay underlay,
      Entity entity,
      List<ValueDisplayField> groupByFields,
      EntityFilter filter,
      PageMarker pageMarker,
      Integer pageSize) {
    this.underlay = underlay;
    this.entity = entity;
    this.groupByFields = ImmutableList.copyOf(groupByFields);
    this.filter = filter;
    this.pageMarker = pageMarker;
    this.pageSize = (pageMarker == null && pageSize == null) ? DEFAULT_PAGE_SIZE : pageSize;
  }

  public Underlay getUnderlay() {
    return underlay;
  }

  public Entity getEntity() {
    return entity;
  }

  public ImmutableList<ValueDisplayField> getGroupByFields() {
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

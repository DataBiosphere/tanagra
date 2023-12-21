package bio.terra.tanagra.api.query.count;

import bio.terra.tanagra.api.field.valuedisplay.ValueDisplayField;
import bio.terra.tanagra.api.filter.EntityFilter;
import bio.terra.tanagra.api.query.hint.HintQueryResult;
import bio.terra.tanagra.query.PageMarker;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.entitymodel.Entity;
import com.google.common.collect.ImmutableList;
import java.util.List;
import javax.annotation.Nullable;

public class CountQueryRequest {
  // TODO: Lower this once the UI paginates through count results.
  private static final Integer DEFAULT_PAGE_SIZE = 2000;

  private final Underlay underlay;
  private final Entity entity;
  private final ImmutableList<ValueDisplayField> groupByFields;
  private final EntityFilter filter;
  private final PageMarker pageMarker;
  private final Integer pageSize;
  private final @Nullable HintQueryResult entityLevelHints;
  private final boolean isDryRun;

  @SuppressWarnings("checkstyle:ParameterNumber")
  public CountQueryRequest(
      Underlay underlay,
      Entity entity,
      List<ValueDisplayField> groupByFields,
      EntityFilter filter,
      PageMarker pageMarker,
      Integer pageSize,
      @Nullable HintQueryResult entityLevelHints,
      boolean isDryRun) {
    this.underlay = underlay;
    this.entity = entity;
    this.groupByFields = ImmutableList.copyOf(groupByFields);
    this.filter = filter;
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

  public @Nullable HintQueryResult getEntityLevelHints() {
    return entityLevelHints;
  }

  public boolean isDryRun() {
    return isDryRun;
  }
}

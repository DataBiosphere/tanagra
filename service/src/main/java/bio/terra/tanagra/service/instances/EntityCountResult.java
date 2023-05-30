package bio.terra.tanagra.service.instances;

import bio.terra.tanagra.query.PageMarker;
import java.util.List;

public class EntityCountResult {
  private final String sql;
  private final List<EntityInstanceCount> entityCounts;
  private final PageMarker pageMarker;

  public EntityCountResult(
      String sql, List<EntityInstanceCount> entityCounts, PageMarker pageMarker) {
    this.sql = sql;
    this.entityCounts = entityCounts;
    this.pageMarker = pageMarker;
  }

  public String getSql() {
    return sql;
  }

  public List<EntityInstanceCount> getEntityCounts() {
    return entityCounts;
  }

  public PageMarker getPageMarker() {
    return pageMarker;
  }
}

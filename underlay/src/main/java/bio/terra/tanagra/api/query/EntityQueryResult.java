package bio.terra.tanagra.api.query;

import bio.terra.tanagra.query.PageMarker;
import java.util.List;

public class EntityQueryResult {
  private final String sql;
  private final List<EntityInstance> entityInstances;
  private final PageMarker pageMarker;

  public EntityQueryResult(
      String sql, List<EntityInstance> entityInstances, PageMarker pageMarker) {
    this.sql = sql;
    this.entityInstances = entityInstances;
    this.pageMarker = pageMarker;
  }

  public String getSql() {
    return sql;
  }

  public List<EntityInstance> getEntityInstances() {
    return entityInstances;
  }

  public PageMarker getPageMarker() {
    return pageMarker;
  }
}

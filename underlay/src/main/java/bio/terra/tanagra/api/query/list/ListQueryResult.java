package bio.terra.tanagra.api.query.list;

import bio.terra.tanagra.api.query.PageMarker;
import com.google.common.collect.ImmutableList;
import java.util.List;

public class ListQueryResult {
  private final String sql;
  private final ImmutableList<ListInstance> listInstances;
  private final PageMarker pageMarker;

  public ListQueryResult(String sql, List<ListInstance> listInstances, PageMarker pageMarker) {
    this.sql = sql;
    this.listInstances = ImmutableList.copyOf(listInstances);
    this.pageMarker = pageMarker;
  }

  public String getSql() {
    return sql;
  }

  public ImmutableList<ListInstance> getListInstances() {
    return listInstances;
  }

  public PageMarker getPageMarker() {
    return pageMarker;
  }
}

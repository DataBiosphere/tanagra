package bio.terra.tanagra.api.query.list;

import bio.terra.tanagra.api.query.PageMarker;
import com.google.common.collect.ImmutableList;
import java.util.List;

public class ListQueryResult {
  private final String sql;
  private final String sqlNoParams;
  private final ImmutableList<ListInstance> listInstances;
  private final PageMarker pageMarker;
  private final Long numRowsAcrossAllPages;

  public ListQueryResult(
      String sql,
      String sqlNoParams,
      List<ListInstance> listInstances,
      PageMarker pageMarker,
      Long numRowsAcrossAllPages) {
    this.sql = sql;
    this.sqlNoParams = sqlNoParams;
    this.listInstances = ImmutableList.copyOf(listInstances);
    this.pageMarker = pageMarker;
    this.numRowsAcrossAllPages = numRowsAcrossAllPages;
  }

  public String getSql() {
    return sql;
  }

  public String getSqlNoParams() {
    return sqlNoParams;
  }

  public ImmutableList<ListInstance> getListInstances() {
    return listInstances;
  }

  public PageMarker getPageMarker() {
    return pageMarker;
  }

  public Long getNumRowsAcrossAllPages() {
    return numRowsAcrossAllPages;
  }
}

package bio.terra.tanagra.api.query.count;

import bio.terra.tanagra.api.query.PageMarker;
import com.google.common.collect.ImmutableList;
import java.util.List;

public class CountQueryResult {
  private final String sql;
  private final ImmutableList<CountInstance> countInstances;
  private final PageMarker pageMarker;
  private final Long numRowsAcrossAllPages;

  public CountQueryResult(
      String sql,
      List<CountInstance> countInstances,
      PageMarker pageMarker,
      Long numRowsAcrossAllPages) {
    this.sql = sql;
    this.countInstances = ImmutableList.copyOf(countInstances);
    this.pageMarker = pageMarker;
    this.numRowsAcrossAllPages = numRowsAcrossAllPages;
  }

  public String getSql() {
    return sql;
  }

  public ImmutableList<CountInstance> getCountInstances() {
    return countInstances;
  }

  public PageMarker getPageMarker() {
    return pageMarker;
  }

  public Long getNumRowsAcrossAllPages() {
    return numRowsAcrossAllPages;
  }
}

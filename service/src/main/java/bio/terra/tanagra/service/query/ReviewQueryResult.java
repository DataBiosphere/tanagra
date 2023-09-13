package bio.terra.tanagra.service.query;

import bio.terra.tanagra.query.PageMarker;
import java.util.List;

public class ReviewQueryResult {
  private final String sql;
  private final List<ReviewInstance> reviewInstances;
  private final PageMarker pageMarker;

  public ReviewQueryResult(
      String sql, List<ReviewInstance> reviewInstances, PageMarker pageMarker) {
    this.sql = sql;
    this.reviewInstances = reviewInstances;
    this.pageMarker = pageMarker;
  }

  public String getSql() {
    return sql;
  }

  public List<ReviewInstance> getReviewInstances() {
    return reviewInstances;
  }

  public PageMarker getPageMarker() {
    return pageMarker;
  }
}

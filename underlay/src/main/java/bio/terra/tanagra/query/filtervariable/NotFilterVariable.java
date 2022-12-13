package bio.terra.tanagra.query.filtervariable;

import bio.terra.tanagra.query.FilterVariable;

public class NotFilterVariable extends FilterVariable {
  private final FilterVariable subFilter;

  public NotFilterVariable(FilterVariable subFilter) {
    this.subFilter = subFilter;
  }

  @Override
  public String renderSQL() {
    return "(NOT " + subFilter.renderSQL() + ")";
  }
}

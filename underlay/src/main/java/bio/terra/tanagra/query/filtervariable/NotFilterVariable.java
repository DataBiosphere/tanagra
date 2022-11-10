package bio.terra.tanagra.query.filtervariable;

import bio.terra.tanagra.query.FilterVariable;
import bio.terra.tanagra.query.TableVariable;
import java.util.List;

public class NotFilterVariable extends FilterVariable {
  private final FilterVariable subfilter;

  public NotFilterVariable(FilterVariable subfilter) {
    this.subfilter = subfilter;
  }

  @Override
  public String renderSQL() {
    return "(NOT " + subfilter.renderSQL() + ")";
  }

  @Override
  public List<TableVariable> getTableVariables() {
    return null;
  }
}

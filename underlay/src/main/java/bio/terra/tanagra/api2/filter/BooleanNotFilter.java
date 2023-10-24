package bio.terra.tanagra.api2.filter;

import bio.terra.tanagra.query.FilterVariable;
import bio.terra.tanagra.query.TableVariable;
import bio.terra.tanagra.query.filtervariable.NotFilterVariable;
import java.util.List;

public class BooleanNotFilter extends EntityFilter {
  private final bio.terra.tanagra.api.query.filter.EntityFilter subFilter;

  public BooleanNotFilter(bio.terra.tanagra.api.query.filter.EntityFilter subFilter) {
    this.subFilter = subFilter;
  }

  @Override
  public FilterVariable getFilterVariable(
      TableVariable entityTableVar, List<TableVariable> tableVars) {
    return new NotFilterVariable(subFilter.getFilterVariable(entityTableVar, tableVars));
  }
}

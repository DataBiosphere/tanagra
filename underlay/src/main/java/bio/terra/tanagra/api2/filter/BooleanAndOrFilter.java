package bio.terra.tanagra.api2.filter;

import bio.terra.tanagra.query.FilterVariable;
import bio.terra.tanagra.query.TableVariable;
import bio.terra.tanagra.query.filtervariable.BooleanAndOrFilterVariable;
import java.util.List;
import java.util.stream.Collectors;

public class BooleanAndOrFilter extends EntityFilter {
  private final BooleanAndOrFilterVariable.LogicalOperator operator;
  private final List<bio.terra.tanagra.api.query.filter.EntityFilter> subFilters;

  public BooleanAndOrFilter(
      BooleanAndOrFilterVariable.LogicalOperator operator,
      List<bio.terra.tanagra.api.query.filter.EntityFilter> subFilters) {
    this.operator = operator;
    this.subFilters = subFilters;
  }

  @Override
  public FilterVariable getFilterVariable(
      TableVariable entityTableVar, List<TableVariable> tableVars) {
    return new BooleanAndOrFilterVariable(
        operator,
        subFilters.stream()
            .map(subFilter -> subFilter.getFilterVariable(entityTableVar, tableVars))
            .collect(Collectors.toList()));
  }
}

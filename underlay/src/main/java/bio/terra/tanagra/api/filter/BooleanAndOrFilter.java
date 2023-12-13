package bio.terra.tanagra.api.filter;

import bio.terra.tanagra.query.FilterVariable;
import bio.terra.tanagra.query.TableVariable;
import bio.terra.tanagra.query.filtervariable.BooleanAndOrFilterVariable;
import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.stream.Collectors;

public class BooleanAndOrFilter extends EntityFilter {
  private final BooleanAndOrFilterVariable.LogicalOperator operator;
  private final List<EntityFilter> subFilters;

  public BooleanAndOrFilter(
      BooleanAndOrFilterVariable.LogicalOperator operator, List<EntityFilter> subFilters) {
    this.operator = operator;
    this.subFilters = subFilters;
  }

  public BooleanAndOrFilterVariable.LogicalOperator getOperator() {
    return operator;
  }

  public ImmutableList<EntityFilter> getSubFilters() {
    return ImmutableList.copyOf(subFilters);
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

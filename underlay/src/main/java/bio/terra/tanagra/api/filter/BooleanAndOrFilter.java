package bio.terra.tanagra.api.filter;

import bio.terra.tanagra.query.filtervariable.BooleanAndOrFilterVariable;
import com.google.common.collect.ImmutableList;
import java.util.List;

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
}

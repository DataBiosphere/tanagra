package bio.terra.tanagra.api.filter;

import com.google.common.collect.ImmutableList;
import java.util.List;

public class BooleanAndOrFilter extends EntityFilter {
  public enum LogicalOperator {
    AND,
    OR
  }

  private final LogicalOperator operator;
  private final List<? extends EntityFilter> subFilters;

  public BooleanAndOrFilter(LogicalOperator operator, List<? extends EntityFilter> subFilters) {
    this.operator = operator;
    this.subFilters = subFilters;
  }

  public LogicalOperator getOperator() {
    return operator;
  }

  public ImmutableList<? extends EntityFilter> getSubFilters() {
    return ImmutableList.copyOf(subFilters);
  }
}

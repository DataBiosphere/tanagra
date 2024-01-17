package bio.terra.tanagra.api.filter;

import com.google.common.collect.ImmutableList;
import java.util.List;

public class BooleanAndOrFilter extends EntityFilter {
  public enum LogicalOperator {
    AND,
    OR
  }

  private final LogicalOperator operator;
  private final List<EntityFilter> subFilters;

  public BooleanAndOrFilter(LogicalOperator operator, List<EntityFilter> subFilters) {
    this.operator = operator;
    this.subFilters = subFilters;
  }

  public LogicalOperator getOperator() {
    return operator;
  }

  public ImmutableList<EntityFilter> getSubFilters() {
    return ImmutableList.copyOf(subFilters);
  }
}

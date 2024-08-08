package bio.terra.tanagra.api.filter;

import bio.terra.tanagra.exception.*;
import bio.terra.tanagra.underlay.entitymodel.*;
import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Objects;

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

  @Override
  public Entity getEntity() {
    Entity entity = subFilters.get(0).getEntity();
    if (subFilters.stream()
        .filter(subFilter -> !subFilter.getEntity().equals(entity))
        .findAny()
        .isPresent()) {
      throw new InvalidQueryException(
          "All sub-filters of a boolean and/or filter must be for the same entity.");
    }
    return entity;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    BooleanAndOrFilter that = (BooleanAndOrFilter) o;
    return operator == that.operator && subFilters.equals(that.subFilters);
  }

  @Override
  public int hashCode() {
    return Objects.hash(operator, subFilters);
  }
}

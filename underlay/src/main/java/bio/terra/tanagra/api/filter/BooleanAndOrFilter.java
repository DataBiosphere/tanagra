package bio.terra.tanagra.api.filter;

import bio.terra.tanagra.exception.InvalidQueryException;
import bio.terra.tanagra.underlay.entitymodel.Entity;
import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Objects;
import org.slf4j.LoggerFactory;

public class BooleanAndOrFilter extends EntityFilter {

  public enum LogicalOperator {
    AND,
    OR
  }

  private final LogicalOperator operator;
  private final List<EntityFilter> subFilters;

  public BooleanAndOrFilter(LogicalOperator operator, List<EntityFilter> subFilters) {
    super(
        LoggerFactory.getLogger(BooleanAndOrFilter.class),
        /* underlay= */ null,
        getSubFiltersEntity(subFilters));
    this.operator = operator;
    this.subFilters = subFilters;
  }

  public LogicalOperator getOperator() {
    return operator;
  }

  public ImmutableList<EntityFilter> getSubFilters() {
    return ImmutableList.copyOf(subFilters);
  }

  private static Entity getSubFiltersEntity(List<EntityFilter> filters) {
    Entity entity = filters.get(0).getEntity();
    if (filters.stream().anyMatch(filter -> !filter.getEntity().equals(entity))) {
      throw new InvalidQueryException(
          "All sub-filters of a boolean and/or filter must be for the same entity.");
    }
    return entity;
  }

  @Override
  public boolean equals(Object o) {
    if (!super.equals(o)) {
      return false;
    }
    BooleanAndOrFilter that = (BooleanAndOrFilter) o;
    return operator == that.operator && subFilters.equals(that.subFilters);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), operator, subFilters);
  }
}

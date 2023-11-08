package bio.terra.tanagra.query.filter;

import bio.terra.tanagra.query.Filter;
import bio.terra.tanagra.query.TableVariable;
import bio.terra.tanagra.query.filtervariable.BooleanAndOrFilterVariable;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public final class BooleanAndOrFilter extends Filter {
  private final BooleanAndOrFilterVariable.LogicalOperator operator;
  private final List<Filter> subfilters;

  private BooleanAndOrFilter(
      BooleanAndOrFilterVariable.LogicalOperator operator, List<Filter> subfilters) {
    this.operator = operator;
    this.subfilters = subfilters;
  }

  @Override
  public Type getType() {
    return Type.BOOLEAN_AND_OR;
  }

  @Override
  public BooleanAndOrFilterVariable buildVariable(
      TableVariable primaryTable, List<TableVariable> tables) {
    return new BooleanAndOrFilterVariable(
        operator,
        subfilters.stream()
            .map(sf -> sf.buildVariable(primaryTable, tables))
            .collect(Collectors.toList()));
  }

  public BooleanAndOrFilterVariable.LogicalOperator getOperator() {
    return operator;
  }

  public List<Filter> getSubfilters() {
    return Collections.unmodifiableList(subfilters);
  }
}

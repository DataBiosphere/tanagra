package bio.terra.tanagra.query;

import bio.terra.tanagra.underlay.TableFilter;
import java.util.List;
import java.util.stream.Collectors;

public class ArrayFilterVariable extends FilterVariable {
  private TableFilter.LogicalOperator operator;
  private List<FilterVariable> subfilters;

  public ArrayFilterVariable(
      TableFilter.LogicalOperator operator, List<FilterVariable> subfilters) {
    this.operator = operator;
    this.subfilters = subfilters;
  }

  @Override
  public String renderSQL() {
    return subfilters.stream()
        .map(sf -> sf.renderSQL())
        .collect(Collectors.joining(" " + operator.renderSQL() + " "));
  }

  @Override
  public List<TableVariable> getTableVariables() {
    return null;
  }
}

package bio.terra.tanagra.query.filtervariable;

import bio.terra.tanagra.query.FilterVariable;
import bio.terra.tanagra.query.SQLExpression;
import java.util.List;
import java.util.stream.Collectors;

public class BooleanAndOrFilterVariable extends FilterVariable {
  private final LogicalOperator operator;
  private final List<FilterVariable> subFilters;

  public BooleanAndOrFilterVariable(LogicalOperator operator, List<FilterVariable> subFilters) {
    this.operator = operator;
    this.subFilters = subFilters;
  }

  @Override
  public String renderSQL() {
    return "("
        + subFilters.stream()
            .map(sf -> sf.renderSQL())
            .collect(Collectors.joining(" " + operator.renderSQL() + " "))
        + ")";
  }

  public enum LogicalOperator implements SQLExpression {
    AND,
    OR;

    @Override
    public String renderSQL() {
      return name();
    }
  }
}

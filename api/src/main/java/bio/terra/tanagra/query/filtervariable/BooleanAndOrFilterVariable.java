package bio.terra.tanagra.query.filtervariable;

import bio.terra.tanagra.query.FilterVariable;
import bio.terra.tanagra.query.SQLExpression;
import bio.terra.tanagra.query.TableVariable;
import java.util.List;
import java.util.stream.Collectors;

public class BooleanAndOrFilterVariable extends FilterVariable {
  private final LogicalOperator operator;
  private final List<FilterVariable> subfilters;

  public BooleanAndOrFilterVariable(LogicalOperator operator, List<FilterVariable> subfilters) {
    this.operator = operator;
    this.subfilters = subfilters;
  }

  @Override
  public String renderSQL() {
    return "("
        + subfilters.stream()
            .map(sf -> sf.renderSQL())
            .collect(Collectors.joining(" " + operator.renderSQL() + " "))
        + ")";
  }

  @Override
  public List<TableVariable> getTableVariables() {
    return null;
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

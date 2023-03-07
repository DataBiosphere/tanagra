package bio.terra.tanagra.query.filtervariable;

import bio.terra.tanagra.query.Literal;
import bio.terra.tanagra.query.SQLExpression;

/** Example: HAVING COUNT(*) > 1 */
public class HavingFilterVariable implements SQLExpression {
  private final BinaryFilterVariable.BinaryOperator operator;
  private final Literal value;

  public HavingFilterVariable(BinaryFilterVariable.BinaryOperator operator, Literal value) {
    this.operator = operator;
    this.value = value;
  }

  @Override
  public String renderSQL() {
    return String.format("HAVING COUNT(*) %s %s", operator.renderSQL(), value.renderSQL());
  }
}

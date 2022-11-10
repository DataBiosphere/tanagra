package bio.terra.tanagra.query;

public class OrderByVariable implements SQLExpression {
  private final FieldVariable fieldVariable;
  private final OrderByDirection direction;

  public OrderByVariable(FieldVariable fieldVariable) {
    this.fieldVariable = fieldVariable;
    this.direction = OrderByDirection.ASCENDING;
  }

  public OrderByVariable(FieldVariable fieldVariable, OrderByDirection direction) {
    this.fieldVariable = fieldVariable;
    this.direction = direction;
  }

  @Override
  public String renderSQL() {
    return fieldVariable.renderSqlForOrderBy() + " " + direction.renderSQL();
  }
}

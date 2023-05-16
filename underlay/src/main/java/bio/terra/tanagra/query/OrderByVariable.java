package bio.terra.tanagra.query;

public class OrderByVariable implements SQLExpression {
  private final FieldVariable fieldVariable;
  private final OrderByDirection direction;
  private final boolean isRandom;
  private boolean isIncludedInSelect;

  public OrderByVariable(FieldVariable fieldVariable) {
    this.fieldVariable = fieldVariable;
    this.direction = OrderByDirection.ASCENDING;
    this.isRandom = false;
  }

  public OrderByVariable(FieldVariable fieldVariable, OrderByDirection direction) {
    this.fieldVariable = fieldVariable;
    this.direction = direction;
    this.isRandom = false;
  }

  private OrderByVariable() {
    this.fieldVariable = null;
    this.direction = null;
    this.isRandom = true;
  }

  public static OrderByVariable forRandom() {
    return new OrderByVariable();
  }

  public OrderByVariable setIsIncludedInSelect(boolean isIncludedInSelect) {
    this.isIncludedInSelect = isIncludedInSelect;
    return this;
  }

  @Override
  public String renderSQL() {
    return isRandom
        ? "RAND()"
        : fieldVariable.renderSqlForOrderOrGroupBy(isIncludedInSelect)
            + " "
            + direction.renderSQL();
  }

  public FieldVariable getFieldVariable() {
    return fieldVariable;
  }

  public boolean isRandom() {
    return isRandom;
  }
}

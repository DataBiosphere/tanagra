package bio.terra.tanagra.query;

import bio.terra.tanagra.underlay.Literal;
import bio.terra.tanagra.underlay.TableFilter;
import java.util.List;

public class BinaryFilterVariable extends FilterVariable {
  private FieldVariable fieldVariable;
  private TableFilter.BinaryOperator operator;
  private Literal value;

  public BinaryFilterVariable(
      FieldVariable fieldVariable, TableFilter.BinaryOperator operator, Literal value) {
    this.fieldVariable = fieldVariable;
    this.operator = operator;
    this.value = value;
  }

  @Override
  public String renderSQL() {
    return "BinaryFilterVariable";
  }

  @Override
  public List<TableVariable> getTableVariables() {
    return null;
  }
}

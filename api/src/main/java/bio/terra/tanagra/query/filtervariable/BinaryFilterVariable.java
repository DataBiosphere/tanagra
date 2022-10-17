package bio.terra.tanagra.query.filtervariable;

import bio.terra.tanagra.query.FieldVariable;
import bio.terra.tanagra.query.FilterVariable;
import bio.terra.tanagra.query.SQLExpression;
import bio.terra.tanagra.query.TableVariable;
import bio.terra.tanagra.query.Literal;
import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.text.StringSubstitutor;

public class BinaryFilterVariable extends FilterVariable {
  private final FieldVariable fieldVariable;
  private final BinaryOperator operator;
  private final Literal value;

  public BinaryFilterVariable(FieldVariable fieldVariable, BinaryOperator operator, Literal value) {
    this.fieldVariable = fieldVariable;
    this.operator = operator;
    this.value = value;
  }

  @Override
  public String renderSQL() {
    String template = "${fieldSQL} ${operator} ${value}";
    Map<String, String> params =
        ImmutableMap.<String, String>builder()
            .put("fieldSQL", fieldVariable.renderSqlForWhere())
            .put("operator", operator.renderSQL())
            .put("value", value.renderSQL())
            .build();
    return StringSubstitutor.replace(template, params);
  }

  @Override
  public List<TableVariable> getTableVariables() {
    return null;
  }

  public enum BinaryOperator implements SQLExpression {
    EQUALS("="),
    NOT_EQUALS("!="),
    LESS_THAN("<"),
    GREATER_THAN(">"),
    LESS_THAN_OR_EQUAL("<="),
    GREATER_THAN_OR_EQUAL(">="),
    IS("IS"),
    IS_NOT("IS NOT");

    private String sql;

    BinaryOperator(String sql) {
      this.sql = sql;
    }

    @Override
    public String renderSQL() {
      return sql;
    }
  }
}

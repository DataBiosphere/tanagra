package bio.terra.tanagra.query;

import bio.terra.tanagra.underlay.Literal;
import bio.terra.tanagra.underlay.TableFilter;
import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.text.StringSubstitutor;

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
    String template = "${fieldSQL} ${operator} ${value}";
    Map<String, String> params =
        ImmutableMap.<String, String>builder()
            .put("fieldSQL", fieldVariable.renderSQL())
            .put("operator", operator.renderSQL())
            .put("value", value.renderSQL())
            .build();
    return StringSubstitutor.replace(template, params);
  }

  @Override
  public List<TableVariable> getTableVariables() {
    return null;
  }
}

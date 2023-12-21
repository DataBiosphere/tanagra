package bio.terra.tanagra.query.filtervariable;

import bio.terra.tanagra.query.FieldVariable;
import bio.terra.tanagra.query.FilterVariable;
import bio.terra.tanagra.query.FunctionTemplate;
import bio.terra.tanagra.query.Literal;
import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.text.StringSubstitutor;

public class FunctionFilterVariable extends FilterVariable {
  private final FieldVariable fieldVariable;
  private final FunctionTemplate functionTemplate;
  private final List<Literal> values;

  public FunctionFilterVariable(
      FunctionTemplate functionTemplate, FieldVariable fieldVariable, Literal... values) {
    this.functionTemplate = functionTemplate;
    this.fieldVariable = fieldVariable;
    this.values = List.of(values);
  }

  @Override
  protected String getSubstitutionTemplate() {
    String valuesSQL =
        values.size() > 1
            ? values.stream().map(Literal::renderSQL).collect(Collectors.joining(","))
            : values.get(0).renderSQL();
    Map<String, String> params =
        ImmutableMap.<String, String>builder().put("value", valuesSQL).build();
    return StringSubstitutor.replace(functionTemplate.getSqlTemplate(), params);
  }

  @Override
  public List<FieldVariable> getFieldVariables() {
    return List.of(fieldVariable);
  }
}

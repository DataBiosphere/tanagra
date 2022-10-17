package bio.terra.tanagra.query.filtervariable;

import bio.terra.tanagra.query.FieldVariable;
import bio.terra.tanagra.query.FilterVariable;
import bio.terra.tanagra.query.TableVariable;
import bio.terra.tanagra.query.Literal;
import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.text.StringSubstitutor;

public class FunctionFilterVariable extends FilterVariable {
  private final FieldVariable fieldVariable;
  private final FunctionTemplate functionTemplate;
  private final Literal value;

  public FunctionFilterVariable(
      FunctionTemplate functionTemplate, FieldVariable fieldVariable, Literal value) {
    this.functionTemplate = functionTemplate;
    this.fieldVariable = fieldVariable;
    this.value = value;
  }

  @Override
  public String renderSQL() {
    Map<String, String> params =
        ImmutableMap.<String, String>builder()
            .put("fieldVariable", fieldVariable.renderSqlForWhere())
            .put("value", value.renderSQL())
            .build();
    return StringSubstitutor.replace(functionTemplate.getSqlTemplate(), params);
  }

  @Override
  public List<TableVariable> getTableVariables() {
    return null;
  }

  public enum FunctionTemplate {
    TEXT_EXACT_MATCH("CONTAINS_SUBSTR(${fieldVariable}, ${value})"),
    TEXT_FUZZY_MATCH("bqutil.fn.levenshtein(UPPER(${fieldVariable}), UPPER(${value}))<5");

    private String sqlTemplate;

    FunctionTemplate(String sqlTemplate) {
      this.sqlTemplate = sqlTemplate;
    }

    String getSqlTemplate() {
      return sqlTemplate;
    }
  }
}

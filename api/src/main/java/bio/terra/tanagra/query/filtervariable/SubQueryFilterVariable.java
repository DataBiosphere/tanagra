package bio.terra.tanagra.query.filtervariable;

import bio.terra.tanagra.query.FieldVariable;
import bio.terra.tanagra.query.FilterVariable;
import bio.terra.tanagra.query.Query;
import bio.terra.tanagra.query.SQLExpression;
import bio.terra.tanagra.query.TableVariable;
import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.text.StringSubstitutor;

public class SubQueryFilterVariable extends FilterVariable {
  public enum Operator implements SQLExpression {
    IN("IN"),
    NOT_IN("NOT IN");

    private String sql;

    Operator(String sql) {
      this.sql = sql;
    }

    @Override
    public String renderSQL() {
      return sql;
    }
  }

  private final FieldVariable fieldVariable;
  private final Operator operator;
  private final Query subQuery;

  public SubQueryFilterVariable(FieldVariable fieldVariable, Operator operator, Query subQuery) {
    this.fieldVariable = fieldVariable;
    this.operator = operator;
    this.subQuery = subQuery;
  }

  @Override
  public String renderSQL() {
    String template = "${fieldSQL} ${operator} (${subQuerySQL})";
    Map<String, String> params =
        ImmutableMap.<String, String>builder()
            .put("fieldSQL", fieldVariable.renderSQL())
            .put("operator", operator.renderSQL())
            .put("subQuerySQL", subQuery.renderSQL())
            .build();
    return StringSubstitutor.replace(template, params);
  }

  @Override
  public List<TableVariable> getTableVariables() {
    return null;
  }
}

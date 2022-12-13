package bio.terra.tanagra.query;

import com.google.common.collect.ImmutableMap;
import java.util.Collections;
import java.util.List;
import org.apache.commons.text.StringSubstitutor;

public abstract class FilterVariable implements SQLExpression {
  protected String getSubstitutionTemplate() {
    throw new UnsupportedOperationException("Substitution template required");
  }

  protected List<FieldVariable> getFieldVariables() {
    return Collections.EMPTY_LIST;
  }

  @Override
  public String renderSQL() {
    ImmutableMap.Builder paramsBuilder = ImmutableMap.<String, String>builder();
    List<FieldVariable> fieldVars = getFieldVariables();
    for (int ctr = 0; ctr < fieldVars.size(); ctr++) {
      paramsBuilder.put(
          "fieldVariable" + (ctr == 0 ? "" : ctr), fieldVars.get(ctr).renderSqlForWhere());
    }
    return StringSubstitutor.replace(getSubstitutionTemplate(), paramsBuilder.build());
  }
}

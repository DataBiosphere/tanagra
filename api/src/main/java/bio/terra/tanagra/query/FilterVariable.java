package bio.terra.tanagra.query;

import java.util.List;

public abstract class FilterVariable implements SQLExpression {
  protected FilterVariable() {}

  public abstract List<TableVariable> getTableVariables();
}

package bio.terra.tanagra.query;

import java.util.List;

public abstract class FilterVariable implements SQLExpression {
  public abstract List<TableVariable> getTableVariables();
}

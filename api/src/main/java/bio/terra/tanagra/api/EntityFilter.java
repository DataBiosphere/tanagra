package bio.terra.tanagra.api;

import bio.terra.tanagra.query.FilterVariable;
import bio.terra.tanagra.query.TableVariable;
import java.util.List;

public abstract class EntityFilter {
  public abstract FilterVariable getFilterVariable(
      TableVariable entityTableVar, List<TableVariable> tableVars);
}

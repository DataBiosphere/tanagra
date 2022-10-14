package bio.terra.tanagra.api.entityfilter;

import bio.terra.tanagra.api.EntityFilter;
import bio.terra.tanagra.query.FilterVariable;
import bio.terra.tanagra.query.TableVariable;
import bio.terra.tanagra.query.filtervariable.NotFilterVariable;
import bio.terra.tanagra.underlay.Entity;
import bio.terra.tanagra.underlay.EntityMapping;
import java.util.List;

public class BooleanNotFilter extends EntityFilter {
  private final EntityFilter subFilter;

  public BooleanNotFilter(Entity entity, EntityMapping entityMapping, EntityFilter subFilter) {
    super(entity, entityMapping);
    this.subFilter = subFilter;
  }

  @Override
  public FilterVariable getFilterVariable(
      TableVariable entityTableVar, List<TableVariable> tableVars) {
    return new NotFilterVariable(subFilter.getFilterVariable(entityTableVar, tableVars));
  }
}

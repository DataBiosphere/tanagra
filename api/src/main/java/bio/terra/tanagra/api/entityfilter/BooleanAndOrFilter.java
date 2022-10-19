package bio.terra.tanagra.api.entityfilter;

import bio.terra.tanagra.api.EntityFilter;
import bio.terra.tanagra.query.FilterVariable;
import bio.terra.tanagra.query.TableVariable;
import bio.terra.tanagra.query.filtervariable.ArrayFilterVariable;
import bio.terra.tanagra.underlay.Entity;
import bio.terra.tanagra.underlay.EntityMapping;
import java.util.List;
import java.util.stream.Collectors;

public class BooleanAndOrFilter extends EntityFilter {
  private final ArrayFilterVariable.LogicalOperator operator;
  private final List<EntityFilter> subFilters;

  public BooleanAndOrFilter(
      Entity entity,
      EntityMapping entityMapping,
      ArrayFilterVariable.LogicalOperator operator,
      List<EntityFilter> subFilters) {
    super(entity, entityMapping);
    this.operator = operator;
    this.subFilters = subFilters;
  }

  @Override
  public FilterVariable getFilterVariable(
      TableVariable entityTableVar, List<TableVariable> tableVars) {
    return new ArrayFilterVariable(
        operator,
        subFilters.stream()
            .map(subFilter -> subFilter.getFilterVariable(entityTableVar, tableVars))
            .collect(Collectors.toList()));
  }
}

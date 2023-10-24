package bio.terra.tanagra.api2.filter;

import bio.terra.tanagra.query.*;
import bio.terra.tanagra.query.filtervariable.BinaryFilterVariable;
import bio.terra.tanagra.underlay2.Hierarchy;
import java.util.List;

public class HierarchyIsMemberFilter extends EntityFilter {
  private final Hierarchy hierarchy;

  public HierarchyIsMemberFilter(Hierarchy hierarchy) {
    this.hierarchy = hierarchy;
  }

  @Override
  public FilterVariable getFilterVariable(
      TableVariable entityTableVar, List<TableVariable> tableVars) {
    // IS_MEMBER means path IS NOT NULL.
    FieldVariable pathFieldVar =
        hierarchy.getIndexPathField().buildVariable(entityTableVar, tableVars);
    return new BinaryFilterVariable(
        pathFieldVar, BinaryFilterVariable.BinaryOperator.IS_NOT, new Literal(null));
  }
}

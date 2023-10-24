package bio.terra.tanagra.api2.filter;

import bio.terra.tanagra.query.*;
import bio.terra.tanagra.query.filtervariable.BinaryFilterVariable;
import bio.terra.tanagra.underlay2.Hierarchy;
import java.util.List;

public class HierarchyIsRootFilter extends EntityFilter {
  private final Hierarchy hierarchy;

  public HierarchyIsRootFilter(Hierarchy hierarchy) {
    this.hierarchy = hierarchy;
  }

  @Override
  public FilterVariable getFilterVariable(
      TableVariable entityTableVar, List<TableVariable> tableVars) {
    // IS_ROOT means path=''.
    FieldVariable pathFieldVar =
        hierarchy.getIndexPathField().buildVariable(entityTableVar, tableVars);
    return new BinaryFilterVariable(
        pathFieldVar, BinaryFilterVariable.BinaryOperator.EQUALS, new Literal(""));
  }
}

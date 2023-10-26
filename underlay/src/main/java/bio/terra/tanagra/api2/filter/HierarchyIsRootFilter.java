package bio.terra.tanagra.api2.filter;

import bio.terra.tanagra.query.*;
import bio.terra.tanagra.query.filtervariable.BinaryFilterVariable;
import bio.terra.tanagra.underlay2.Underlay;
import bio.terra.tanagra.underlay2.entitymodel.Entity;
import bio.terra.tanagra.underlay2.entitymodel.Hierarchy;
import bio.terra.tanagra.underlay2.indextable.ITEntityMain;
import java.util.List;

public class HierarchyIsRootFilter extends EntityFilter {
  private final ITEntityMain indexTable;
  private final Hierarchy hierarchy;

  public HierarchyIsRootFilter(Underlay underlay, Entity entity, Hierarchy hierarchy) {
    this.indexTable = underlay.getIndexSchema().getEntityMain(entity.getName());
    this.hierarchy = hierarchy;
  }

  @Override
  public FilterVariable getFilterVariable(
      TableVariable entityTableVar, List<TableVariable> tableVars) {
    // IS_ROOT means path=''.
    FieldVariable pathFieldVar =
        indexTable
            .getHierarchyPathField(hierarchy.getName())
            .buildVariable(entityTableVar, tableVars);
    return new BinaryFilterVariable(
        pathFieldVar, BinaryFilterVariable.BinaryOperator.EQUALS, new Literal(""));
  }
}

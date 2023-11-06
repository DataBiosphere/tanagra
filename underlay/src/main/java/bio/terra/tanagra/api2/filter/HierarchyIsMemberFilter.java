package bio.terra.tanagra.api2.filter;

import bio.terra.tanagra.query.FieldVariable;
import bio.terra.tanagra.query.FilterVariable;
import bio.terra.tanagra.query.Literal;
import bio.terra.tanagra.query.TableVariable;
import bio.terra.tanagra.query.filtervariable.BinaryFilterVariable;
import bio.terra.tanagra.underlay2.Underlay;
import bio.terra.tanagra.underlay2.entitymodel.Entity;
import bio.terra.tanagra.underlay2.entitymodel.Hierarchy;
import bio.terra.tanagra.underlay2.indextable.ITEntityMain;
import java.util.List;

public class HierarchyIsMemberFilter extends EntityFilter {
  private final ITEntityMain indexTable;
  private final Hierarchy hierarchy;

  public HierarchyIsMemberFilter(Underlay underlay, Entity entity, Hierarchy hierarchy) {
    this.indexTable = underlay.getIndexSchema().getEntityMain(entity.getName());
    this.hierarchy = hierarchy;
  }

  @Override
  public FilterVariable getFilterVariable(
      TableVariable entityTableVar, List<TableVariable> tableVars) {
    // IS_MEMBER means path IS NOT NULL.
    FieldVariable pathFieldVar =
        indexTable
            .getHierarchyPathField(hierarchy.getName())
            .buildVariable(entityTableVar, tableVars);
    return new BinaryFilterVariable(
        pathFieldVar, BinaryFilterVariable.BinaryOperator.IS_NOT, new Literal(null));
  }
}

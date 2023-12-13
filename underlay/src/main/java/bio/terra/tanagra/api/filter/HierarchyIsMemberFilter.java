package bio.terra.tanagra.api.filter;

import bio.terra.tanagra.query.FieldVariable;
import bio.terra.tanagra.query.FilterVariable;
import bio.terra.tanagra.query.Literal;
import bio.terra.tanagra.query.TableVariable;
import bio.terra.tanagra.query.filtervariable.BinaryFilterVariable;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.entitymodel.Entity;
import bio.terra.tanagra.underlay.entitymodel.Hierarchy;
import bio.terra.tanagra.underlay.indextable.ITEntityMain;
import java.util.List;

public class HierarchyIsMemberFilter extends EntityFilter {
  private final ITEntityMain indexTable;
  private final Entity entity;
  private final Hierarchy hierarchy;

  public HierarchyIsMemberFilter(Underlay underlay, Entity entity, Hierarchy hierarchy) {
    this.indexTable = underlay.getIndexSchema().getEntityMain(entity.getName());
    this.entity = entity;
    this.hierarchy = hierarchy;
  }

  public Entity getEntity() {
    return entity;
  }

  public Hierarchy getHierarchy() {
    return hierarchy;
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

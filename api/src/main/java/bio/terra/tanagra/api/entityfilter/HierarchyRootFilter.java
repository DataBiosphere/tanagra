package bio.terra.tanagra.api.entityfilter;

import bio.terra.tanagra.api.EntityFilter;
import bio.terra.tanagra.query.FieldVariable;
import bio.terra.tanagra.query.FilterVariable;
import bio.terra.tanagra.query.TableVariable;
import bio.terra.tanagra.query.filtervariable.BinaryFilterVariable;
import bio.terra.tanagra.underlay.Entity;
import bio.terra.tanagra.underlay.EntityMapping;
import bio.terra.tanagra.underlay.FieldPointer;
import bio.terra.tanagra.underlay.HierarchyMapping;
import bio.terra.tanagra.underlay.Literal;
import java.util.List;

public class HierarchyRootFilter extends EntityFilter {
  private final HierarchyMapping hierarchyMapping;

  public HierarchyRootFilter(
      Entity entity, EntityMapping entityMapping, HierarchyMapping hierarchyMapping) {
    super(entity, entityMapping);
    this.hierarchyMapping = hierarchyMapping;
  }

  @Override
  public FilterVariable getFilterVariable(
      TableVariable entityTableVar, List<TableVariable> tableVars) {
    FieldPointer entityIdFieldPointer = getEntityMapping().getIdAttributeMapping().getValue();
    FieldPointer pathFieldPointer =
        hierarchyMapping.buildPathFieldPointerFromEntityId(entityIdFieldPointer);
    FieldVariable pathFieldVar = pathFieldPointer.buildVariable(entityTableVar, tableVars);

    // IS_ROOT translates to path=""
    return new BinaryFilterVariable(
        pathFieldVar, BinaryFilterVariable.BinaryOperator.EQUALS, new Literal(""));
  }
}

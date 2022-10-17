package bio.terra.tanagra.api.entityfilter;

import bio.terra.tanagra.api.EntityFilter;
import bio.terra.tanagra.query.FieldVariable;
import bio.terra.tanagra.query.FilterVariable;
import bio.terra.tanagra.query.TableVariable;
import bio.terra.tanagra.query.filtervariable.BinaryFilterVariable;
import bio.terra.tanagra.underlay.Entity;
import bio.terra.tanagra.underlay.EntityMapping;
import bio.terra.tanagra.query.FieldPointer;
import bio.terra.tanagra.underlay.HierarchyField;
import bio.terra.tanagra.underlay.HierarchyMapping;
import bio.terra.tanagra.query.Literal;
import bio.terra.tanagra.underlay.hierarchyfield.Path;
import java.util.List;

public class HierarchyRootFilter extends EntityFilter {
  private final HierarchyMapping hierarchyMapping;
  private final String hierarchyName;

  public HierarchyRootFilter(
      Entity entity,
      EntityMapping entityMapping,
      HierarchyMapping hierarchyMapping,
      String hierarchyName) {
    super(entity, entityMapping);
    this.hierarchyMapping = hierarchyMapping;
    this.hierarchyName = hierarchyName;
  }

  @Override
  public FilterVariable getFilterVariable(
      TableVariable entityTableVar, List<TableVariable> tableVars) {
    FieldPointer entityIdFieldPointer = getEntityMapping().getIdAttributeMapping().getValue();
    HierarchyField pathField = new Path(hierarchyName);
    FieldVariable pathFieldVar =
        pathField.buildFieldVariableFromEntityId(
            hierarchyMapping, entityIdFieldPointer, entityTableVar, tableVars);

    // IS_ROOT translates to path=""
    return new BinaryFilterVariable(
        pathFieldVar, BinaryFilterVariable.BinaryOperator.EQUALS, new Literal(""));
  }
}

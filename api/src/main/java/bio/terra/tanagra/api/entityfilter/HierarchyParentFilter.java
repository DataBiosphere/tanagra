package bio.terra.tanagra.api.entityfilter;

import static bio.terra.tanagra.indexing.job.beam.BigQueryUtils.CHILD_COLUMN_NAME;
import static bio.terra.tanagra.indexing.job.beam.BigQueryUtils.PARENT_COLUMN_NAME;

import bio.terra.tanagra.api.EntityFilter;
import bio.terra.tanagra.query.FieldPointer;
import bio.terra.tanagra.query.FieldVariable;
import bio.terra.tanagra.query.FilterVariable;
import bio.terra.tanagra.query.Literal;
import bio.terra.tanagra.query.Query;
import bio.terra.tanagra.query.TableVariable;
import bio.terra.tanagra.query.filtervariable.BinaryFilterVariable;
import bio.terra.tanagra.query.filtervariable.SubQueryFilterVariable;
import bio.terra.tanagra.underlay.AuxiliaryDataMapping;
import bio.terra.tanagra.underlay.Entity;
import bio.terra.tanagra.underlay.EntityMapping;
import bio.terra.tanagra.underlay.HierarchyMapping;
import bio.terra.tanagra.underlay.Underlay;
import java.util.List;

public class HierarchyParentFilter extends EntityFilter {
  private final HierarchyMapping hierarchyMapping;
  private final Literal nodeId;

  public HierarchyParentFilter(
      Entity entity,
      EntityMapping entityMapping,
      HierarchyMapping hierarchyMapping,
      Literal nodeId) {
    super(entity, entityMapping);
    this.hierarchyMapping = hierarchyMapping;
    this.nodeId = nodeId;
  }

  @Override
  public FilterVariable getFilterVariable(
      TableVariable entityTableVar, List<TableVariable> tableVars) {
    FieldPointer entityIdFieldPointer =
        getEntity().getIdAttribute().getMapping(Underlay.MappingType.INDEX).getValue();

    // build a query to get a node's children:
    //   SELECT child FROM childParentTable WHERE parent=nodeId
    AuxiliaryDataMapping childParentAuxData = hierarchyMapping.getChildParent();
    TableVariable childParentTableVar =
        TableVariable.forPrimary(childParentAuxData.getTablePointer());
    FieldVariable childFieldVar =
        new FieldVariable(
            childParentAuxData.getFieldPointers().get(CHILD_COLUMN_NAME), childParentTableVar);
    FieldVariable parentFieldVar =
        new FieldVariable(
            childParentAuxData.getFieldPointers().get(PARENT_COLUMN_NAME), childParentTableVar);
    BinaryFilterVariable parentEqualsNodeId =
        new BinaryFilterVariable(
            parentFieldVar, BinaryFilterVariable.BinaryOperator.EQUALS, nodeId);
    Query subQuery =
        new Query.Builder()
            .select(List.of(childFieldVar))
            .tables(List.of(childParentTableVar))
            .where(parentEqualsNodeId)
            .build();

    // build a filter variable on the sub query
    FieldVariable entityIdFieldVar = entityIdFieldPointer.buildVariable(entityTableVar, tableVars);
    return new SubQueryFilterVariable(
        entityIdFieldVar, SubQueryFilterVariable.Operator.IN, subQuery);
  }
}

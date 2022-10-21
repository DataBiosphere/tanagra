package bio.terra.tanagra.api.entityfilter;

import static bio.terra.tanagra.indexing.job.beam.BigQueryUtils.ANCESTOR_COLUMN_NAME;
import static bio.terra.tanagra.indexing.job.beam.BigQueryUtils.DESCENDANT_COLUMN_NAME;

import bio.terra.tanagra.api.EntityFilter;
import bio.terra.tanagra.query.FieldPointer;
import bio.terra.tanagra.query.FieldVariable;
import bio.terra.tanagra.query.FilterVariable;
import bio.terra.tanagra.query.Literal;
import bio.terra.tanagra.query.Query;
import bio.terra.tanagra.query.TableVariable;
import bio.terra.tanagra.query.filtervariable.BinaryFilterVariable;
import bio.terra.tanagra.query.filtervariable.BooleanAndOrFilterVariable;
import bio.terra.tanagra.query.filtervariable.SubQueryFilterVariable;
import bio.terra.tanagra.underlay.AuxiliaryDataMapping;
import bio.terra.tanagra.underlay.Hierarchy;
import bio.terra.tanagra.underlay.Underlay;
import java.util.List;

public class HierarchyAncestorFilter extends EntityFilter {
  private final Hierarchy hierarchy;
  private final Literal nodeId;

  public HierarchyAncestorFilter(Hierarchy hierarchy, Literal nodeId) {
    this.hierarchy = hierarchy;
    this.nodeId = nodeId;
  }

  @Override
  public FilterVariable getFilterVariable(
      TableVariable entityTableVar, List<TableVariable> tableVars) {
    FieldPointer entityIdFieldPointer =
        hierarchy.getEntity().getIdAttribute().getMapping(Underlay.MappingType.INDEX).getValue();

    // build a query to get a node's descendants:
    //   SELECT descendant FROM ancestorDescendantTable WHERE ancestor=nodeId
    AuxiliaryDataMapping ancestorDescendantAuxData =
        hierarchy.getMapping(Underlay.MappingType.INDEX).getAncestorDescendant();
    TableVariable ancestorDescendantTableVar =
        TableVariable.forPrimary(ancestorDescendantAuxData.getTablePointer());
    FieldVariable descendantFieldVar =
        new FieldVariable(
            ancestorDescendantAuxData.getFieldPointers().get(DESCENDANT_COLUMN_NAME),
            ancestorDescendantTableVar);
    FieldVariable ancestorFieldVar =
        new FieldVariable(
            ancestorDescendantAuxData.getFieldPointers().get(ANCESTOR_COLUMN_NAME),
            ancestorDescendantTableVar);
    BinaryFilterVariable ancestorEqualsNodeId =
        new BinaryFilterVariable(
            ancestorFieldVar, BinaryFilterVariable.BinaryOperator.EQUALS, nodeId);
    Query justDescendants =
        new Query.Builder()
            .select(List.of(descendantFieldVar))
            .tables(List.of(ancestorDescendantTableVar))
            .where(ancestorEqualsNodeId)
            .build();

    // build a filter variable on the sub query
    FieldVariable entityIdFieldVar = entityIdFieldPointer.buildVariable(entityTableVar, tableVars);
    SubQueryFilterVariable justDescendantsFilterVar =
        new SubQueryFilterVariable(
            entityIdFieldVar, SubQueryFilterVariable.Operator.IN, justDescendants);

    // build a filter variable on the exact node match: WHERE entityId=nodeId
    BinaryFilterVariable itselfFilterVar =
        new BinaryFilterVariable(
            entityIdFieldVar, BinaryFilterVariable.BinaryOperator.EQUALS, nodeId);

    // build an array filter variable with both:
    //   WHERE entityId IN (SELECT descendant FROM ancestorDescendantTable WHERE ancestor=nodeId)
    //   OR entityId=nodeId
    return new BooleanAndOrFilterVariable(
        BooleanAndOrFilterVariable.LogicalOperator.OR,
        List.of(justDescendantsFilterVar, itselfFilterVar));
  }
}

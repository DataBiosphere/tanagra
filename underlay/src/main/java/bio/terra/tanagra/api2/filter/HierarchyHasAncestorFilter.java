package bio.terra.tanagra.api2.filter;

import bio.terra.tanagra.query.*;
import bio.terra.tanagra.query.filtervariable.BinaryFilterVariable;
import bio.terra.tanagra.query.filtervariable.BooleanAndOrFilterVariable;
import bio.terra.tanagra.query.filtervariable.SubQueryFilterVariable;
import bio.terra.tanagra.underlay2.Entity;
import bio.terra.tanagra.underlay2.Hierarchy;
import java.util.List;

public class HierarchyHasAncestorFilter extends EntityFilter {
  private final Entity entity;
  private final Hierarchy hierarchy;
  private final Literal ancestorId;

  public HierarchyHasAncestorFilter(Entity entity, Hierarchy hierarchy, Literal ancestorId) {
    this.entity = entity;
    this.hierarchy = hierarchy;
    this.ancestorId = ancestorId;
  }

  @Override
  public FilterVariable getFilterVariable(
      TableVariable entityTableVar, List<TableVariable> tableVars) {
    // Query to get a node's descendants.
    //   SELECT descendant FROM ancestorDescendantTable WHERE ancestor=ancestor id
    TableVariable descendantTableVar =
        TableVariable.forPrimary(hierarchy.getIndexDescendantField().getTablePointer());
    Query isDescendantQuery =
        new Query.Builder()
            .select(
                List.of(new FieldVariable(hierarchy.getIndexDescendantField(), descendantTableVar)))
            .tables(List.of(descendantTableVar))
            .where(
                new BinaryFilterVariable(
                    new FieldVariable(hierarchy.getIndexAncestorField(), descendantTableVar),
                    BinaryFilterVariable.BinaryOperator.EQUALS,
                    ancestorId))
            .build();

    // Filter for entity id IN descendant sub-query.
    FieldVariable entityIdFieldVar =
        entity.getIdAttribute().getIndexValueField().buildVariable(entityTableVar, tableVars);
    SubQueryFilterVariable isDescendantFilterVar =
        new SubQueryFilterVariable(
            entityIdFieldVar, SubQueryFilterVariable.Operator.IN, isDescendantQuery);

    // Filter the entity id = ancestor id.
    BinaryFilterVariable exactMatchFilterVar =
        new BinaryFilterVariable(
            entityIdFieldVar, BinaryFilterVariable.BinaryOperator.EQUALS, ancestorId);

    // Combined filter for entity id IN descendant sub-query OR entity id = ancestor id.
    return new BooleanAndOrFilterVariable(
        BooleanAndOrFilterVariable.LogicalOperator.OR,
        List.of(isDescendantFilterVar, exactMatchFilterVar));
  }
}

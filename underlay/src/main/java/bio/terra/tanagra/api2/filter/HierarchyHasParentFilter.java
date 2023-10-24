package bio.terra.tanagra.api2.filter;

import bio.terra.tanagra.query.*;
import bio.terra.tanagra.query.filtervariable.BinaryFilterVariable;
import bio.terra.tanagra.query.filtervariable.SubQueryFilterVariable;
import bio.terra.tanagra.underlay2.Entity;
import bio.terra.tanagra.underlay2.Hierarchy;
import java.util.List;

public class HierarchyHasParentFilter extends EntityFilter {
  private final Entity entity;
  private final Hierarchy hierarchy;
  private final Literal parentId;

  public HierarchyHasParentFilter(Entity entity, Hierarchy hierarchy, Literal parentId) {
    this.entity = entity;
    this.hierarchy = hierarchy;
    this.parentId = parentId;
  }

  @Override
  public FilterVariable getFilterVariable(
      TableVariable entityTableVar, List<TableVariable> tableVars) {
    // Query to get a node's children.
    //   SELECT child FROM childParentTable WHERE parent=parentId
    TableVariable childTableVar =
        TableVariable.forPrimary(hierarchy.getIndexChildField().getTablePointer());
    Query isChildQuery =
        new Query.Builder()
            .select(List.of(new FieldVariable(hierarchy.getIndexChildField(), childTableVar)))
            .tables(List.of(childTableVar))
            .where(
                new BinaryFilterVariable(
                    new FieldVariable(hierarchy.getIndexParentField(), childTableVar),
                    BinaryFilterVariable.BinaryOperator.EQUALS,
                    parentId))
            .build();

    // Filter for entity id IN children sub-query.
    FieldVariable entityIdFieldVar =
        entity.getIdAttribute().getIndexValueField().buildVariable(entityTableVar, tableVars);
    return new SubQueryFilterVariable(
        entityIdFieldVar, SubQueryFilterVariable.Operator.IN, isChildQuery);
  }
}

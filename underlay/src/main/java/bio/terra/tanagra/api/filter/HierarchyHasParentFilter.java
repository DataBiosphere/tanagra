package bio.terra.tanagra.api.filter;

import bio.terra.tanagra.query.FieldVariable;
import bio.terra.tanagra.query.FilterVariable;
import bio.terra.tanagra.query.Literal;
import bio.terra.tanagra.query.Query;
import bio.terra.tanagra.query.TableVariable;
import bio.terra.tanagra.query.filtervariable.BinaryFilterVariable;
import bio.terra.tanagra.query.filtervariable.SubQueryFilterVariable;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.entitymodel.Attribute;
import bio.terra.tanagra.underlay.entitymodel.Entity;
import bio.terra.tanagra.underlay.entitymodel.Hierarchy;
import bio.terra.tanagra.underlay.indextable.ITEntityMain;
import bio.terra.tanagra.underlay.indextable.ITHierarchyChildParent;
import java.util.List;

public class HierarchyHasParentFilter extends EntityFilter {
  private final ITEntityMain indexEntityTable;
  private final ITHierarchyChildParent indexChildParentTable;
  private final Attribute idAttribute;
  private final Literal parentId;

  public HierarchyHasParentFilter(
      Underlay underlay, Entity entity, Hierarchy hierarchy, Literal parentId) {
    this.indexEntityTable = underlay.getIndexSchema().getEntityMain(entity.getName());
    this.indexChildParentTable =
        underlay.getIndexSchema().getHierarchyChildParent(entity.getName(), hierarchy.getName());
    this.idAttribute = entity.getIdAttribute();
    this.parentId = parentId;
  }

  @Override
  public FilterVariable getFilterVariable(
      TableVariable entityTableVar, List<TableVariable> tableVars) {
    // Query to get a node's children.
    //   SELECT child FROM childParentTable WHERE parent=parentId
    TableVariable childParentTableVar =
        TableVariable.forPrimary(indexChildParentTable.getTablePointer());
    Query isChildQuery =
        new Query.Builder()
            .select(
                List.of(
                    new FieldVariable(indexChildParentTable.getChildField(), childParentTableVar)))
            .tables(List.of(childParentTableVar))
            .where(
                new BinaryFilterVariable(
                    new FieldVariable(indexChildParentTable.getParentField(), childParentTableVar),
                    BinaryFilterVariable.BinaryOperator.EQUALS,
                    parentId))
            .build();

    // Filter for entity id IN children sub-query.
    FieldVariable entityIdFieldVar =
        indexEntityTable
            .getAttributeValueField(idAttribute.getName())
            .buildVariable(entityTableVar, tableVars);
    return new SubQueryFilterVariable(
        entityIdFieldVar, SubQueryFilterVariable.Operator.IN, isChildQuery);
  }
}

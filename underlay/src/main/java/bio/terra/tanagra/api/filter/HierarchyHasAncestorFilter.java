package bio.terra.tanagra.api.filter;

import bio.terra.tanagra.query.FieldVariable;
import bio.terra.tanagra.query.FilterVariable;
import bio.terra.tanagra.query.Literal;
import bio.terra.tanagra.query.Query;
import bio.terra.tanagra.query.TableVariable;
import bio.terra.tanagra.query.filtervariable.BinaryFilterVariable;
import bio.terra.tanagra.query.filtervariable.BooleanAndOrFilterVariable;
import bio.terra.tanagra.query.filtervariable.SubQueryFilterVariable;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.entitymodel.Attribute;
import bio.terra.tanagra.underlay.entitymodel.Entity;
import bio.terra.tanagra.underlay.entitymodel.Hierarchy;
import bio.terra.tanagra.underlay.indextable.ITEntityMain;
import bio.terra.tanagra.underlay.indextable.ITHierarchyAncestorDescendant;
import java.util.List;

public class HierarchyHasAncestorFilter extends EntityFilter {
  private final ITEntityMain indexEntityTable;
  private final ITHierarchyAncestorDescendant indexAncestorDescendantTable;
  private final Attribute idAttribute;
  private final Literal ancestorId;

  public HierarchyHasAncestorFilter(
      Underlay underlay, Entity entity, Hierarchy hierarchy, Literal ancestorId) {
    this.indexEntityTable = underlay.getIndexSchema().getEntityMain(entity.getName());
    this.indexAncestorDescendantTable =
        underlay
            .getIndexSchema()
            .getHierarchyAncestorDescendant(entity.getName(), hierarchy.getName());
    this.idAttribute = entity.getIdAttribute();
    this.ancestorId = ancestorId;
  }

  @Override
  public FilterVariable getFilterVariable(
      TableVariable entityTableVar, List<TableVariable> tableVars) {
    // Query to get a node's descendants.
    //   SELECT descendant FROM ancestorDescendantTable WHERE ancestor=ancestor id
    TableVariable ancestorDescendantTableVar =
        TableVariable.forPrimary(indexAncestorDescendantTable.getTablePointer());
    Query isDescendantQuery =
        new Query.Builder()
            .select(
                List.of(
                    new FieldVariable(
                        indexAncestorDescendantTable.getDescendantField(),
                        ancestorDescendantTableVar)))
            .tables(List.of(ancestorDescendantTableVar))
            .where(
                new BinaryFilterVariable(
                    new FieldVariable(
                        indexAncestorDescendantTable.getAncestorField(),
                        ancestorDescendantTableVar),
                    BinaryFilterVariable.BinaryOperator.EQUALS,
                    ancestorId))
            .build();

    // Filter for entity id IN descendant sub-query.
    FieldVariable entityIdFieldVar =
        indexEntityTable
            .getAttributeValueField(idAttribute.getName())
            .buildVariable(entityTableVar, tableVars);
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

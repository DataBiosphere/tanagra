package bio.terra.tanagra.query.bigquery.translator.filter;

import bio.terra.tanagra.api.filter.HierarchyHasAncestorFilter;
import bio.terra.tanagra.api.shared.BinaryOperator;
import bio.terra.tanagra.api.shared.Literal;
import bio.terra.tanagra.api.shared.NaryOperator;
import bio.terra.tanagra.query.sql.SqlField;
import bio.terra.tanagra.query.sql.SqlParams;
import bio.terra.tanagra.query.sql.translator.ApiFilterTranslator;
import bio.terra.tanagra.query.sql.translator.ApiTranslator;
import bio.terra.tanagra.underlay.entitymodel.Attribute;
import bio.terra.tanagra.underlay.indextable.ITHierarchyAncestorDescendant;

public class BQHierarchyHasAncestorFilterTranslator extends ApiFilterTranslator {
  private final HierarchyHasAncestorFilter hierarchyHasAncestorFilter;

  public BQHierarchyHasAncestorFilterTranslator(
      ApiTranslator apiTranslator, HierarchyHasAncestorFilter hierarchyHasAncestorFilter) {
    super(apiTranslator);
    this.hierarchyHasAncestorFilter = hierarchyHasAncestorFilter;
  }

  @Override
  public String buildSql(SqlParams sqlParams, String tableAlias) {
    //  entity.id IN (SELECT ancestorId UNION ALL SELECT descendant FROM ancestorDescendantTable
    // FILTER ON ancestorId)
    ITHierarchyAncestorDescendant ancestorDescendantTable =
        hierarchyHasAncestorFilter
            .getUnderlay()
            .getIndexSchema()
            .getHierarchyAncestorDescendant(
                hierarchyHasAncestorFilter.getEntity().getName(),
                hierarchyHasAncestorFilter.getHierarchy().getName());
    Attribute idAttribute = hierarchyHasAncestorFilter.getEntity().getIdAttribute();
    SqlField idField =
        attributeSwapFields.containsKey(idAttribute)
            ? attributeSwapFields.get(idAttribute)
            : hierarchyHasAncestorFilter
                .getUnderlay()
                .getIndexSchema()
                .getEntityMain(hierarchyHasAncestorFilter.getEntity().getName())
                .getAttributeValueField(idAttribute.getName());

    // FILTER ON ancestorId = [WHERE ancestor IN (ancestorIds)] or [WHERE ancestor = ancestorId]
    String ancestorIdFilterSql =
        hierarchyHasAncestorFilter.getAncestorIds().size() > 1
            ? apiTranslator.naryFilterSql(
                ancestorDescendantTable.getAncestorField(),
                NaryOperator.IN,
                hierarchyHasAncestorFilter.getAncestorIds(),
                null,
                sqlParams)
            : apiTranslator.binaryFilterSql(
                ancestorDescendantTable.getAncestorField(),
                BinaryOperator.EQUALS,
                hierarchyHasAncestorFilter.getAncestorIds().get(0),
                null,
                sqlParams);

    return apiTranslator.inSelectFilterSql(
        idField,
        tableAlias,
        ancestorDescendantTable.getDescendantField(),
        ancestorDescendantTable.getTablePointer(),
        ancestorIdFilterSql,
        null,
        sqlParams,
        hierarchyHasAncestorFilter.getAncestorIds().toArray(new Literal[0]));
  }

  @Override
  public boolean isFilterOnAttribute(Attribute attribute) {
    return attribute.isId();
  }
}

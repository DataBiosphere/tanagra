package bio.terra.tanagra.query2.bigquery.filtertranslator;

import bio.terra.tanagra.api.filter.HierarchyHasAncestorFilter;
import bio.terra.tanagra.api.shared.BinaryOperator;
import bio.terra.tanagra.query2.sql.SqlField;
import bio.terra.tanagra.query2.sql.SqlFilterTranslator;
import bio.terra.tanagra.query2.sql.SqlParams;
import bio.terra.tanagra.query2.sql.SqlTranslator;
import bio.terra.tanagra.underlay.entitymodel.Attribute;
import bio.terra.tanagra.underlay.indextable.ITHierarchyAncestorDescendant;

public class BQHierarchyHasAncestorFilterTranslator extends SqlFilterTranslator {
  private final HierarchyHasAncestorFilter hierarchyHasAncestorFilter;

  public BQHierarchyHasAncestorFilterTranslator(
      SqlTranslator sqlTranslator, HierarchyHasAncestorFilter hierarchyHasAncestorFilter) {
    super(sqlTranslator);
    this.hierarchyHasAncestorFilter = hierarchyHasAncestorFilter;
  }

  @Override
  public String buildSql(SqlParams sqlParams, String tableAlias) {
    //  entity.id IN (SELECT ancestorId UNION ALL SELECT descendant FROM ancestorDescendantTable
    // WHERE ancestor=ancestorId)
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
    return sqlTranslator.inSelectFilterSql(
        idField,
        tableAlias,
        ancestorDescendantTable.getDescendantField(),
        ancestorDescendantTable.getTablePointer(),
        sqlTranslator.binaryFilterSql(
            ancestorDescendantTable.getAncestorField(),
            BinaryOperator.EQUALS,
            hierarchyHasAncestorFilter.getAncestorId(),
            null,
            sqlParams),
        sqlParams,
        hierarchyHasAncestorFilter.getAncestorId());
  }

  @Override
  public boolean isFilterOnAttribute(Attribute attribute) {
    return attribute.isId();
  }
}

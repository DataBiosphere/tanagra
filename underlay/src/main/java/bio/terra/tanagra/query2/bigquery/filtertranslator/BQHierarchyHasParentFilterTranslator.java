package bio.terra.tanagra.query2.bigquery.filtertranslator;

import bio.terra.tanagra.api.filter.HierarchyHasParentFilter;
import bio.terra.tanagra.api.shared.BinaryOperator;
import bio.terra.tanagra.query2.sql.SqlField;
import bio.terra.tanagra.query2.sql.SqlFilterTranslator;
import bio.terra.tanagra.query2.sql.SqlParams;
import bio.terra.tanagra.query2.sql.SqlTranslator;
import bio.terra.tanagra.underlay.entitymodel.Attribute;
import bio.terra.tanagra.underlay.indextable.ITHierarchyChildParent;

public class BQHierarchyHasParentFilterTranslator extends SqlFilterTranslator {
  private final HierarchyHasParentFilter hierarchyHasParentFilter;

  public BQHierarchyHasParentFilterTranslator(
      SqlTranslator sqlTranslator, HierarchyHasParentFilter hierarchyHasParentFilter) {
    super(sqlTranslator);
    this.hierarchyHasParentFilter = hierarchyHasParentFilter;
  }

  @Override
  public String buildSql(SqlParams sqlParams, String tableAlias) {
    //  entity.id IN (SELECT child FROM childParentTable WHERE parent=parentId)
    ITHierarchyChildParent childParentIndexTable =
        hierarchyHasParentFilter
            .getUnderlay()
            .getIndexSchema()
            .getHierarchyChildParent(
                hierarchyHasParentFilter.getEntity().getName(),
                hierarchyHasParentFilter.getHierarchy().getName());
    Attribute idAttribute = hierarchyHasParentFilter.getEntity().getIdAttribute();
    SqlField idField =
        attributeSwapFields.containsKey(idAttribute)
            ? attributeSwapFields.get(idAttribute)
            : hierarchyHasParentFilter
                .getUnderlay()
                .getIndexSchema()
                .getEntityMain(hierarchyHasParentFilter.getEntity().getName())
                .getAttributeValueField(idAttribute.getName());
    return sqlTranslator.inSelectFilterSql(
        idField,
        tableAlias,
        childParentIndexTable.getChildField(),
        childParentIndexTable.getTablePointer(),
        sqlTranslator.binaryFilterSql(
            childParentIndexTable.getParentField(),
            BinaryOperator.EQUALS,
            hierarchyHasParentFilter.getParentId(),
            null,
            sqlParams),
        sqlParams);
  }

  @Override
  public boolean isFilterOnAttribute(Attribute attribute) {
    return attribute.isId();
  }
}

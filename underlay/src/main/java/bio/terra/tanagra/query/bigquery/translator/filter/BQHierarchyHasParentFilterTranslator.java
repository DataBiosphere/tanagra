package bio.terra.tanagra.query.bigquery.translator.filter;

import bio.terra.tanagra.api.filter.HierarchyHasParentFilter;
import bio.terra.tanagra.api.shared.BinaryOperator;
import bio.terra.tanagra.query.sql.SqlField;
import bio.terra.tanagra.query.sql.SqlParams;
import bio.terra.tanagra.query.sql.translator.ApiFilterTranslator;
import bio.terra.tanagra.query.sql.translator.ApiTranslator;
import bio.terra.tanagra.underlay.entitymodel.Attribute;
import bio.terra.tanagra.underlay.indextable.ITHierarchyChildParent;

public class BQHierarchyHasParentFilterTranslator extends ApiFilterTranslator {
  private final HierarchyHasParentFilter hierarchyHasParentFilter;

  public BQHierarchyHasParentFilterTranslator(
          ApiTranslator apiTranslator, HierarchyHasParentFilter hierarchyHasParentFilter) {
    super(apiTranslator);
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
    return apiTranslator.inSelectFilterSql(
        idField,
        tableAlias,
        childParentIndexTable.getChildField(),
        childParentIndexTable.getTablePointer(),
        apiTranslator.binaryFilterSql(
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

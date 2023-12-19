package bio.terra.tanagra.query2.bigquery.filtertranslator;

import static bio.terra.tanagra.query2.sql.SqlGeneration.inSelectFilterSql;

import bio.terra.tanagra.api.filter.HierarchyHasParentFilter;
import bio.terra.tanagra.query.FieldPointer;
import bio.terra.tanagra.query.filtervariable.BinaryFilterVariable;
import bio.terra.tanagra.query2.sql.SqlFilterTranslator;
import bio.terra.tanagra.query2.sql.SqlGeneration;
import bio.terra.tanagra.query2.sql.SqlParams;
import bio.terra.tanagra.underlay.entitymodel.Attribute;
import bio.terra.tanagra.underlay.indextable.ITHierarchyChildParent;

public class BQHierarchyHasParentFilterTranslator implements SqlFilterTranslator {
  private final HierarchyHasParentFilter hierarchyHasParentFilter;

  public BQHierarchyHasParentFilterTranslator(HierarchyHasParentFilter hierarchyHasParentFilter) {
    this.hierarchyHasParentFilter = hierarchyHasParentFilter;
  }

  @Override
  public String buildSql(SqlParams sqlParams, String tableAlias, FieldPointer idField) {
    //  entity.id IN (SELECT child FROM childParentTable WHERE parent=parentId)
    ITHierarchyChildParent childParentIndexTable =
        hierarchyHasParentFilter
            .getUnderlay()
            .getIndexSchema()
            .getHierarchyChildParent(
                hierarchyHasParentFilter.getEntity().getName(),
                hierarchyHasParentFilter.getHierarchy().getName());
    return inSelectFilterSql(
        idField,
        tableAlias,
        childParentIndexTable.getChildField(),
        childParentIndexTable.getTablePointer(),
        SqlGeneration.binaryFilterSql(
            childParentIndexTable.getParentField(),
            BinaryFilterVariable.BinaryOperator.EQUALS,
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

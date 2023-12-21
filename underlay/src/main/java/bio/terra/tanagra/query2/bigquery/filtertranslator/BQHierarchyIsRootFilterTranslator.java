package bio.terra.tanagra.query2.bigquery.filtertranslator;

import bio.terra.tanagra.api.filter.HierarchyIsRootFilter;
import bio.terra.tanagra.query.FieldPointer;
import bio.terra.tanagra.query.FunctionTemplate;
import bio.terra.tanagra.query2.sql.SqlFilterTranslator;
import bio.terra.tanagra.query2.sql.SqlParams;
import bio.terra.tanagra.query2.sql.SqlTranslator;
import bio.terra.tanagra.underlay.entitymodel.Attribute;
import bio.terra.tanagra.underlay.indextable.ITEntityMain;
import java.util.List;

public class BQHierarchyIsRootFilterTranslator extends SqlFilterTranslator {
  private final HierarchyIsRootFilter hierarchyIsRootFilter;

  public BQHierarchyIsRootFilterTranslator(
      SqlTranslator sqlTranslator, HierarchyIsRootFilter hierarchyIsRootFilter) {
    super(sqlTranslator);
    this.hierarchyIsRootFilter = hierarchyIsRootFilter;
  }

  @Override
  public String buildSql(SqlParams sqlParams, String tableAlias) {
    ITEntityMain indexTable =
        hierarchyIsRootFilter
            .getUnderlay()
            .getIndexSchema()
            .getEntityMain(hierarchyIsRootFilter.getEntity().getName());

    // IS_ROOT means path=''.
    FieldPointer pathField =
        indexTable.getHierarchyPathField(hierarchyIsRootFilter.getHierarchy().getName());
    return sqlTranslator.functionFilterSql(
        pathField,
        sqlTranslator.functionTemplateSql(FunctionTemplate.IS_EMPTY_STRING),
        List.of(),
        tableAlias,
        sqlParams);
  }

  @Override
  public boolean isFilterOnAttribute(Attribute attribute) {
    return false;
  }
}

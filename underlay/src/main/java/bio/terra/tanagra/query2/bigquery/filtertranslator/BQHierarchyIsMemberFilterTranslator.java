package bio.terra.tanagra.query2.bigquery.filtertranslator;

import bio.terra.tanagra.api.filter.HierarchyIsMemberFilter;
import bio.terra.tanagra.query.FieldPointer;
import bio.terra.tanagra.query.FunctionTemplate;
import bio.terra.tanagra.query2.sql.SqlFilterTranslator;
import bio.terra.tanagra.query2.sql.SqlParams;
import bio.terra.tanagra.query2.sql.SqlTranslator;
import bio.terra.tanagra.underlay.entitymodel.Attribute;
import bio.terra.tanagra.underlay.indextable.ITEntityMain;
import java.util.List;

public class BQHierarchyIsMemberFilterTranslator extends SqlFilterTranslator {
  private final HierarchyIsMemberFilter hierarchyIsMemberFilter;

  public BQHierarchyIsMemberFilterTranslator(
      SqlTranslator sqlTranslator, HierarchyIsMemberFilter hierarchyIsMemberFilter) {
    super(sqlTranslator);
    this.hierarchyIsMemberFilter = hierarchyIsMemberFilter;
  }

  @Override
  public String buildSql(SqlParams sqlParams, String tableAlias) {
    ITEntityMain indexTable =
        hierarchyIsMemberFilter
            .getUnderlay()
            .getIndexSchema()
            .getEntityMain(hierarchyIsMemberFilter.getEntity().getName());

    // IS_MEMBER means path IS NOT NULL.
    FieldPointer pathField =
        indexTable.getHierarchyPathField(hierarchyIsMemberFilter.getHierarchy().getName());
    return sqlTranslator.functionFilterSql(
        pathField,
        sqlTranslator.functionTemplateSql(FunctionTemplate.IS_NOT_NULL),
        List.of(),
        tableAlias,
        sqlParams);
  }

  @Override
  public boolean isFilterOnAttribute(Attribute attribute) {
    return false;
  }
}

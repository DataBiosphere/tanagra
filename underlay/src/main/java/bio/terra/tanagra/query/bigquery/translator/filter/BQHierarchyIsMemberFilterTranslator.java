package bio.terra.tanagra.query.bigquery.translator.filter;

import bio.terra.tanagra.api.filter.HierarchyIsMemberFilter;
import bio.terra.tanagra.api.shared.FunctionTemplate;
import bio.terra.tanagra.query.sql.SqlField;
import bio.terra.tanagra.query.sql.SqlParams;
import bio.terra.tanagra.query.sql.translator.ApiFilterTranslator;
import bio.terra.tanagra.query.sql.translator.ApiTranslator;
import bio.terra.tanagra.underlay.entitymodel.Attribute;
import bio.terra.tanagra.underlay.indextable.ITEntityMain;
import java.util.List;

public class BQHierarchyIsMemberFilterTranslator extends ApiFilterTranslator {
  private final HierarchyIsMemberFilter hierarchyIsMemberFilter;

  public BQHierarchyIsMemberFilterTranslator(
      ApiTranslator apiTranslator, HierarchyIsMemberFilter hierarchyIsMemberFilter) {
    super(apiTranslator);
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
    SqlField pathField =
        indexTable.getHierarchyPathField(hierarchyIsMemberFilter.getHierarchy().getName());
    return apiTranslator.functionFilterSql(
        pathField,
        apiTranslator.functionTemplateSql(FunctionTemplate.IS_NOT_NULL),
        List.of(),
        tableAlias,
        sqlParams);
  }

  @Override
  public boolean isFilterOnAttribute(Attribute attribute) {
    return false;
  }
}

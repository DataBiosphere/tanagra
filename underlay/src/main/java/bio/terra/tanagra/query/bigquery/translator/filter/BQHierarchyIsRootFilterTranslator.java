package bio.terra.tanagra.query.bigquery.translator.filter;

import bio.terra.tanagra.api.filter.HierarchyIsRootFilter;
import bio.terra.tanagra.api.shared.FunctionTemplate;
import bio.terra.tanagra.query.sql.SqlField;
import bio.terra.tanagra.query.sql.SqlParams;
import bio.terra.tanagra.query.sql.translator.ApiFilterTranslator;
import bio.terra.tanagra.query.sql.translator.ApiTranslator;
import bio.terra.tanagra.underlay.entitymodel.Attribute;
import bio.terra.tanagra.underlay.indextable.ITEntityMain;
import java.util.List;

public class BQHierarchyIsRootFilterTranslator extends ApiFilterTranslator {
  private final HierarchyIsRootFilter hierarchyIsRootFilter;

  public BQHierarchyIsRootFilterTranslator(
      ApiTranslator apiTranslator, HierarchyIsRootFilter hierarchyIsRootFilter) {
    super(apiTranslator);
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
    SqlField pathField =
        indexTable.getHierarchyPathField(hierarchyIsRootFilter.getHierarchy().getName());
    return apiTranslator.functionFilterSql(
        pathField,
        apiTranslator.functionTemplateSql(FunctionTemplate.IS_EMPTY_STRING),
        List.of(),
        tableAlias,
        sqlParams);
  }

  @Override
  public boolean isFilterOnAttribute(Attribute attribute) {
    return false;
  }
}
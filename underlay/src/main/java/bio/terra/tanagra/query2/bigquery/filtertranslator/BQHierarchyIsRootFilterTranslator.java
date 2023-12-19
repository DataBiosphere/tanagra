package bio.terra.tanagra.query2.bigquery.filtertranslator;

import bio.terra.tanagra.api.filter.HierarchyIsRootFilter;
import bio.terra.tanagra.query.FieldPointer;
import bio.terra.tanagra.query.filtervariable.FunctionFilterVariable;
import bio.terra.tanagra.query2.bigquery.BQTranslator;
import bio.terra.tanagra.query2.sql.SqlFilterTranslator;
import bio.terra.tanagra.query2.sql.SqlGeneration;
import bio.terra.tanagra.query2.sql.SqlParams;
import bio.terra.tanagra.underlay.entitymodel.Attribute;
import bio.terra.tanagra.underlay.indextable.ITEntityMain;
import java.util.List;

public class BQHierarchyIsRootFilterTranslator implements SqlFilterTranslator {
  private final HierarchyIsRootFilter hierarchyIsRootFilter;

  public BQHierarchyIsRootFilterTranslator(HierarchyIsRootFilter hierarchyIsRootFilter) {
    this.hierarchyIsRootFilter = hierarchyIsRootFilter;
  }

  @Override
  public String buildSql(SqlParams sqlParams, String tableAlias, FieldPointer idField) {
    ITEntityMain indexTable =
        hierarchyIsRootFilter
            .getUnderlay()
            .getIndexSchema()
            .getEntityMain(hierarchyIsRootFilter.getEntity().getName());

    // IS_ROOT means path=''.
    FieldPointer pathField =
        indexTable.getHierarchyPathField(hierarchyIsRootFilter.getHierarchy().getName());
    return SqlGeneration.functionFilterSql(
        pathField,
        BQTranslator.functionTemplateSql(FunctionFilterVariable.FunctionTemplate.IS_EMPTY_STRING),
        List.of(),
        tableAlias,
        sqlParams);
  }

  @Override
  public boolean isFilterOnAttribute(Attribute attribute) {
    return false;
  }
}

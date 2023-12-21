package bio.terra.tanagra.query2.bigquery.fieldtranslator;

import bio.terra.tanagra.api.field.HierarchyIsRootField;
import bio.terra.tanagra.api.shared.DataType;
import bio.terra.tanagra.api.shared.ValueDisplay;
import bio.terra.tanagra.query2.sql.SqlField;
import bio.terra.tanagra.query2.sql.SqlFieldTranslator;
import bio.terra.tanagra.query2.sql.SqlQueryField;
import bio.terra.tanagra.query2.sql.SqlRowResult;
import bio.terra.tanagra.underlay.NameHelper;
import bio.terra.tanagra.underlay.indextable.ITEntityMain;
import java.util.List;

public class BQHierarchyIsRootFieldTranslator implements SqlFieldTranslator {
  private static final String FIELD_ALIAS = "ISRT";
  private final HierarchyIsRootField hierarchyIsRootField;

  public BQHierarchyIsRootFieldTranslator(HierarchyIsRootField hierarchyIsRootField) {
    this.hierarchyIsRootField = hierarchyIsRootField;
  }

  @Override
  public List<SqlQueryField> buildSqlFieldsForListSelect() {
    return buildSqlFields();
  }

  @Override
  public List<SqlQueryField> buildSqlFieldsForCountSelect() {
    return buildSqlFields();
  }

  @Override
  public List<SqlQueryField> buildSqlFieldsForOrderBy() {
    return buildSqlFields();
  }

  @Override
  public List<SqlQueryField> buildSqlFieldsForGroupBy() {
    return buildSqlFields();
  }

  private List<SqlQueryField> buildSqlFields() {
    ITEntityMain indexTable =
        hierarchyIsRootField
            .getUnderlay()
            .getIndexSchema()
            .getEntityMain(hierarchyIsRootField.getEntity().getName());
    final String isRootFnStr = "(${fieldSql} IS NOT NULL AND ${fieldSql}='')";
    SqlField field =
        indexTable
            .getHierarchyPathField(hierarchyIsRootField.getHierarchy().getName())
            .toBuilder()
            .sqlFunctionWrapper(isRootFnStr)
            .build();
    return List.of(SqlQueryField.of(field, getFieldAlias()));
  }

  private String getFieldAlias() {
    return NameHelper.getReservedFieldName(
        FIELD_ALIAS + "_" + hierarchyIsRootField.getHierarchy().getName());
  }

  @Override
  public ValueDisplay parseValueDisplayFromResult(SqlRowResult sqlRowResult) {
    return new ValueDisplay(sqlRowResult.get(getFieldAlias(), DataType.BOOLEAN));
  }
}

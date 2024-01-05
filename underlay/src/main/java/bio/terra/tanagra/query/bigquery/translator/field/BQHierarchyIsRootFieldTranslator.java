package bio.terra.tanagra.query.bigquery.translator.field;

import bio.terra.tanagra.api.field.HierarchyIsRootField;
import bio.terra.tanagra.api.shared.DataType;
import bio.terra.tanagra.api.shared.ValueDisplay;
import bio.terra.tanagra.query.sql.SqlField;
import bio.terra.tanagra.query.sql.SqlQueryField;
import bio.terra.tanagra.query.sql.SqlRowResult;
import bio.terra.tanagra.query.sql.translator.ApiFieldTranslator;
import bio.terra.tanagra.underlay.NameHelper;
import bio.terra.tanagra.underlay.indextable.ITEntityMain;
import java.util.List;

public class BQHierarchyIsRootFieldTranslator implements ApiFieldTranslator {
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
            .cloneWithFunctionWrapper(isRootFnStr);
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

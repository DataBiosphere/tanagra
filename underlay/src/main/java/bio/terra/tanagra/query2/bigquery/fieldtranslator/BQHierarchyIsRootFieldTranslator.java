package bio.terra.tanagra.query2.bigquery.fieldtranslator;

import bio.terra.tanagra.api.field.valuedisplay.HierarchyIsRootField;
import bio.terra.tanagra.api.query.ValueDisplay;
import bio.terra.tanagra.query.FieldPointer;
import bio.terra.tanagra.query.Literal;
import bio.terra.tanagra.query2.sql.SqlField;
import bio.terra.tanagra.query2.sql.SqlFieldTranslator;
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
  public List<SqlField> buildSqlFieldsForListSelect() {
    return buildSqlFields();
  }

  @Override
  public List<SqlField> buildSqlFieldsForCountSelect() {
    return buildSqlFields();
  }

  @Override
  public List<SqlField> buildSqlFieldsForOrderBy() {
    return buildSqlFields();
  }

  @Override
  public List<SqlField> buildSqlFieldsForGroupBy() {
    return buildSqlFields();
  }

  private List<SqlField> buildSqlFields() {
    ITEntityMain indexTable =
        hierarchyIsRootField
            .getUnderlay()
            .getIndexSchema()
            .getEntityMain(hierarchyIsRootField.getEntity().getName());
    final String isRootFnStr = "(${fieldSql} IS NOT NULL AND ${fieldSql}='')";
    FieldPointer field =
        indexTable
            .getHierarchyPathField(hierarchyIsRootField.getHierarchy().getName())
            .toBuilder()
            .sqlFunctionWrapper(isRootFnStr)
            .build();
    return List.of(SqlField.of(field, getFieldAlias()));
  }

  private String getFieldAlias() {
    return NameHelper.getReservedFieldName(
        FIELD_ALIAS + "_" + hierarchyIsRootField.getHierarchy().getName());
  }

  @Override
  public ValueDisplay parseValueDisplayFromResult(SqlRowResult sqlRowResult) {
    return new ValueDisplay(sqlRowResult.get(getFieldAlias(), Literal.DataType.BOOLEAN));
  }
}

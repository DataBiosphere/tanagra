package bio.terra.tanagra.query2.bigquery.fieldtranslator;

import bio.terra.tanagra.api.field.valuedisplay.HierarchyIsMemberField;
import bio.terra.tanagra.api.query.ValueDisplay;
import bio.terra.tanagra.query.FieldPointer;
import bio.terra.tanagra.query.Literal;
import bio.terra.tanagra.query2.sql.SqlField;
import bio.terra.tanagra.query2.sql.SqlFieldTranslator;
import bio.terra.tanagra.query2.sql.SqlRowResult;
import bio.terra.tanagra.underlay.NameHelper;
import bio.terra.tanagra.underlay.indextable.ITEntityMain;
import java.util.List;

public class BQHierarchyIsMemberFieldTranslator implements SqlFieldTranslator {
  private static final String FIELD_ALIAS = "ISMEM";
  private final HierarchyIsMemberField hierarchyIsMemberField;

  public BQHierarchyIsMemberFieldTranslator(HierarchyIsMemberField hierarchyIsMemberField) {
    this.hierarchyIsMemberField = hierarchyIsMemberField;
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
        hierarchyIsMemberField
            .getUnderlay()
            .getIndexSchema()
            .getEntityMain(hierarchyIsMemberField.getEntity().getName());
    final String isMemberFnStr = "(${fieldSql} IS NOT NULL)";
    FieldPointer field =
        indexTable
            .getHierarchyPathField(hierarchyIsMemberField.getHierarchy().getName())
            .toBuilder()
            .sqlFunctionWrapper(isMemberFnStr)
            .build();
    return List.of(SqlField.of(field, getFieldAlias()));
  }

  private String getFieldAlias() {
    return NameHelper.getReservedFieldName(
        FIELD_ALIAS + "_" + hierarchyIsMemberField.getHierarchy().getName());
  }

  @Override
  public ValueDisplay parseValueDisplayFromResult(SqlRowResult sqlRowResult) {
    return new ValueDisplay(sqlRowResult.get(getFieldAlias(), Literal.DataType.BOOLEAN));
  }
}

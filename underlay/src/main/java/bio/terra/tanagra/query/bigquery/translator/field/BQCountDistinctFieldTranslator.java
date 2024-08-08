package bio.terra.tanagra.query.bigquery.translator.field;

import bio.terra.tanagra.api.field.CountDistinctField;
import bio.terra.tanagra.api.shared.DataType;
import bio.terra.tanagra.api.shared.ValueDisplay;
import bio.terra.tanagra.query.sql.SqlField;
import bio.terra.tanagra.query.sql.SqlQueryField;
import bio.terra.tanagra.query.sql.SqlRowResult;
import bio.terra.tanagra.query.sql.translator.ApiFieldTranslator;
import bio.terra.tanagra.underlay.NameHelper;
import bio.terra.tanagra.underlay.indextable.ITEntityMain;
import java.util.List;

public class BQCountDistinctFieldTranslator implements ApiFieldTranslator {
  private static final String FIELD_ALIAS = "CTDT";
  private final CountDistinctField countDistinctField;

  public BQCountDistinctFieldTranslator(CountDistinctField countDistinctField) {
    this.countDistinctField = countDistinctField;
  }

  @Override
  public List<SqlQueryField> buildSqlFieldsForListSelect() {
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
        countDistinctField
            .getUnderlay()
            .getIndexSchema()
            .getEntityMain(countDistinctField.getEntity().getName());

    final String countFnStr =
        countDistinctField.getAttribute().isId() ? "COUNT" : "COUNT(DISTINCT ${fieldSql})";
    SqlField field =
        indexTable
            .getAttributeValueField(countDistinctField.getAttribute().getName())
            .cloneWithFunctionWrapper(countFnStr);
    return List.of(SqlQueryField.of(field, getFieldAlias()));
  }

  private String getFieldAlias() {
    return NameHelper.getReservedFieldName(FIELD_ALIAS);
  }

  @Override
  public ValueDisplay parseValueDisplayFromResult(SqlRowResult sqlRowResult) {
    return new ValueDisplay(sqlRowResult.get(getFieldAlias(), DataType.INT64));
  }
}

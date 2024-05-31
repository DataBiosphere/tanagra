package bio.terra.tanagra.query.bigquery.translator.field;

import bio.terra.tanagra.api.field.EntityIdCountField;
import bio.terra.tanagra.api.shared.DataType;
import bio.terra.tanagra.api.shared.ValueDisplay;
import bio.terra.tanagra.query.sql.SqlField;
import bio.terra.tanagra.query.sql.SqlQueryField;
import bio.terra.tanagra.query.sql.SqlRowResult;
import bio.terra.tanagra.query.sql.translator.ApiFieldTranslator;
import bio.terra.tanagra.underlay.NameHelper;
import bio.terra.tanagra.underlay.indextable.ITEntityMain;
import java.util.List;

public class BQEntityIdCountFieldTranslator implements ApiFieldTranslator {
  private static final String FIELD_ALIAS = "IDCT";
  private final EntityIdCountField entityIdCountField;

  public BQEntityIdCountFieldTranslator(EntityIdCountField entityIdCountField) {
    this.entityIdCountField = entityIdCountField;
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
        entityIdCountField
            .getUnderlay()
            .getIndexSchema()
            .getEntityMain(entityIdCountField.getEntity().getName());
    final String countFnStr = "COUNT";
    SqlField field =
        indexTable
            .getAttributeValueField(entityIdCountField.getEntity().getIdAttribute().getName())
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

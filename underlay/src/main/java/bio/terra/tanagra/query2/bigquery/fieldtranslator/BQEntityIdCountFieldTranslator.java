package bio.terra.tanagra.query2.bigquery.fieldtranslator;

import bio.terra.tanagra.api.field.valuedisplay.EntityIdCountField;
import bio.terra.tanagra.api.query.ValueDisplay;
import bio.terra.tanagra.query.FieldPointer;
import bio.terra.tanagra.query.Literal;
import bio.terra.tanagra.query2.sql.SqlField;
import bio.terra.tanagra.query2.sql.SqlFieldTranslator;
import bio.terra.tanagra.query2.sql.SqlRowResult;
import bio.terra.tanagra.underlay.NameHelper;
import bio.terra.tanagra.underlay.indextable.ITEntityMain;
import java.util.List;

public class BQEntityIdCountFieldTranslator implements SqlFieldTranslator {
  private static final String FIELD_ALIAS = "IDCT";
  private final EntityIdCountField entityIdCountField;

  public BQEntityIdCountFieldTranslator(EntityIdCountField entityIdCountField) {
    this.entityIdCountField = entityIdCountField;
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
        entityIdCountField
            .getUnderlay()
            .getIndexSchema()
            .getEntityMain(entityIdCountField.getEntity().getName());
    final String countFnStr = "COUNT";
    FieldPointer field =
        indexTable
            .getAttributeValueField(entityIdCountField.getIdAttribute().getName())
            .toBuilder()
            .sqlFunctionWrapper(countFnStr)
            .build();
    return List.of(SqlField.of(field, getFieldAlias()));
  }

  private String getFieldAlias() {
    return NameHelper.getReservedFieldName(FIELD_ALIAS);
  }

  @Override
  public ValueDisplay parseValueDisplayFromResult(SqlRowResult sqlRowResult) {
    return new ValueDisplay(sqlRowResult.get(getFieldAlias(), Literal.DataType.INT64));
  }
}

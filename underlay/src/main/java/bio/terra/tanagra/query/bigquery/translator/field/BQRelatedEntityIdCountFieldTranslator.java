package bio.terra.tanagra.query.bigquery.translator.field;

import bio.terra.tanagra.api.field.RelatedEntityIdCountField;
import bio.terra.tanagra.api.shared.DataType;
import bio.terra.tanagra.api.shared.ValueDisplay;
import bio.terra.tanagra.query.sql.SqlField;
import bio.terra.tanagra.query.sql.SqlQueryField;
import bio.terra.tanagra.query.sql.SqlRowResult;
import bio.terra.tanagra.query.sql.translator.ApiFieldTranslator;
import bio.terra.tanagra.underlay.indextable.ITEntityMain;
import java.util.List;

public class BQRelatedEntityIdCountFieldTranslator implements ApiFieldTranslator {
  private final RelatedEntityIdCountField relatedEntityIdCountField;

  public BQRelatedEntityIdCountFieldTranslator(
      RelatedEntityIdCountField relatedEntityIdCountField) {
    this.relatedEntityIdCountField = relatedEntityIdCountField;
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
    return List.of(SqlQueryField.of(getField()));
  }

  private SqlField getField() {
    ITEntityMain indexTable =
        relatedEntityIdCountField
            .getUnderlay()
            .getIndexSchema()
            .getEntityMain(relatedEntityIdCountField.getCountForEntity().getName());
    return indexTable.getEntityGroupCountField(
        relatedEntityIdCountField.getEntityGroup().getName(),
        relatedEntityIdCountField.hasHierarchy()
            ? relatedEntityIdCountField.getHierarchy().getName()
            : null);
  }

  @Override
  public ValueDisplay parseValueDisplayFromResult(SqlRowResult sqlRowResult) {
    return new ValueDisplay(sqlRowResult.get(getField().getColumnName(), DataType.INT64));
  }
}

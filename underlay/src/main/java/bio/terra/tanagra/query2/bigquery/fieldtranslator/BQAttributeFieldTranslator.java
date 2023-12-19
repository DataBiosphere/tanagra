package bio.terra.tanagra.query2.bigquery.fieldtranslator;

import bio.terra.tanagra.api.field.valuedisplay.AttributeField;
import bio.terra.tanagra.api.query.ValueDisplay;
import bio.terra.tanagra.query.FieldPointer;
import bio.terra.tanagra.query2.sql.SqlField;
import bio.terra.tanagra.query2.sql.SqlFieldTranslator;
import bio.terra.tanagra.underlay.entitymodel.Attribute;
import bio.terra.tanagra.underlay.indextable.ITEntityMain;
import java.util.ArrayList;
import java.util.List;

public class BQAttributeFieldTranslator implements SqlFieldTranslator {
  private final AttributeField attributeField;
  private final ITEntityMain indexTable;

  public BQAttributeFieldTranslator(AttributeField attributeField) {
    this.attributeField = attributeField;
    this.indexTable =
        attributeField
            .getUnderlay()
            .getIndexSchema()
            .getEntityMain(attributeField.getEntity().getName());
  }

  @Override
  public List<SqlField> buildSqlFieldsForListSelect() {
    return buildSqlFields(true, true);
  }

  @Override
  public List<SqlField> buildSqlFieldsForCountSelect() {
    return buildSqlFields(true, false);
  }

  @Override
  public List<SqlField> buildSqlFieldsForOrderBy() {
    return buildSqlFields(false, true);
  }

  @Override
  public List<SqlField> buildSqlFieldsForGroupBy() {
    return buildSqlFields(true, false);
  }

  private List<SqlField> buildSqlFields(boolean includeValueField, boolean includeDisplayField) {
    Attribute attribute = attributeField.getAttribute();
    FieldPointer valueField = indexTable.getAttributeValueField(attribute.getName());
    if (attribute.hasRuntimeSqlFunctionWrapper()) {
      valueField =
          valueField
              .toBuilder()
              .sqlFunctionWrapper(attribute.getRuntimeSqlFunctionWrapper())
              .build();
    }

    SqlField valueSqlField = SqlField.of(valueField, getValueFieldAlias());
    if (attribute.isSimple() || attributeField.isExcludeDisplay()) {
      return List.of(valueSqlField);
    }

    FieldPointer displayField = indexTable.getAttributeDisplayField(attribute.getName());
    SqlField displaySqlField = SqlField.of(displayField, getDisplayFieldAlias());
    List<SqlField> sqlFields = new ArrayList<>();
    if (includeValueField) {
      sqlFields.add(valueSqlField);
    }
    if (includeDisplayField) {
      sqlFields.add(displaySqlField);
    }
    return sqlFields;
  }

  private String getValueFieldAlias() {
    return indexTable
        .getAttributeValueField(attributeField.getAttribute().getName())
        .getColumnName();
  }

  private String getDisplayFieldAlias() {
    return indexTable
        .getAttributeDisplayField(attributeField.getAttribute().getName())
        .getColumnName();
  }

  @Override
  public ValueDisplay parseValueDisplayFromResult() {
    return null;
  }
}

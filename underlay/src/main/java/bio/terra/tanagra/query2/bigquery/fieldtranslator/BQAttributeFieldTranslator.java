package bio.terra.tanagra.query2.bigquery.fieldtranslator;

import bio.terra.tanagra.api.field.AttributeField;
import bio.terra.tanagra.api.query.ValueDisplay;
import bio.terra.tanagra.api.query.hint.HintInstance;
import bio.terra.tanagra.api.query.hint.HintQueryResult;
import bio.terra.tanagra.query.FieldPointer;
import bio.terra.tanagra.query.Literal;
import bio.terra.tanagra.query2.sql.SqlField;
import bio.terra.tanagra.query2.sql.SqlFieldTranslator;
import bio.terra.tanagra.query2.sql.SqlRowResult;
import bio.terra.tanagra.underlay.entitymodel.Attribute;
import bio.terra.tanagra.underlay.indextable.ITEntityMain;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BQAttributeFieldTranslator implements SqlFieldTranslator {
  private static final Logger LOGGER = LoggerFactory.getLogger(BQAttributeFieldTranslator.class);
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
  public ValueDisplay parseValueDisplayFromResult(SqlRowResult sqlRowResult) {
    Literal valueField =
        sqlRowResult.get(getValueFieldAlias(), attributeField.getAttribute().getRuntimeDataType());

    if (attributeField.getAttribute().isSimple() || attributeField.isExcludeDisplay()) {
      return new ValueDisplay(valueField);
    } else {
      Literal displayField = sqlRowResult.get(getDisplayFieldAlias(), Literal.DataType.STRING);
      return new ValueDisplay(valueField, displayField.getStringVal());
    }
  }

  public ValueDisplay parseValueDisplayFromCountResult(
      SqlRowResult sqlRowResult, HintQueryResult entityLevelHints) {
    Literal valueField =
        sqlRowResult.get(getValueFieldAlias(), attributeField.getAttribute().getRuntimeDataType());

    if (attributeField.getAttribute().isSimple() || attributeField.isExcludeDisplay()) {
      return new ValueDisplay(valueField);
    } else {
      Optional<HintInstance> entityLevelHint =
          entityLevelHints.getHintInstance(attributeField.getAttribute());
      if (entityLevelHint.isEmpty()) {
        LOGGER.warn(
            "Entity-level hint not found for attribute: "
                + attributeField.getAttribute().getName());
        return new ValueDisplay(valueField);
      }
      Optional<String> displayField = entityLevelHint.get().getEnumDisplay(valueField);
      if (displayField.isEmpty()) {
        LOGGER.warn(
            "Entity-level hint enum display not found for attribute: "
                + attributeField.getAttribute().getName());
        return new ValueDisplay(valueField);
      }
      return new ValueDisplay(valueField, displayField.get());
    }
  }
}

package bio.terra.tanagra.query.bigquery.translator.field;

import bio.terra.tanagra.api.field.AttributeField;
import bio.terra.tanagra.api.query.hint.HintInstance;
import bio.terra.tanagra.api.query.hint.HintQueryResult;
import bio.terra.tanagra.api.shared.DataType;
import bio.terra.tanagra.api.shared.Literal;
import bio.terra.tanagra.api.shared.ValueDisplay;
import bio.terra.tanagra.query.sql.SqlField;
import bio.terra.tanagra.query.sql.SqlQueryField;
import bio.terra.tanagra.query.sql.SqlRowResult;
import bio.terra.tanagra.query.sql.translator.ApiFieldTranslator;
import bio.terra.tanagra.underlay.entitymodel.Attribute;
import bio.terra.tanagra.underlay.indextable.ITEntityMain;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BQAttributeFieldTranslator implements ApiFieldTranslator {
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
  public List<SqlQueryField> buildSqlFieldsForListSelect() {
    return buildSqlFields(true, true);
  }

  @Override
  public List<SqlQueryField> buildSqlFieldsForCountSelect() {
    return buildSqlFields(true, false);
  }

  @Override
  public List<SqlQueryField> buildSqlFieldsForOrderBy() {
    return buildSqlFields(false, true);
  }

  @Override
  public List<SqlQueryField> buildSqlFieldsForGroupBy() {
    return buildSqlFields(true, false);
  }

  private List<SqlQueryField> buildSqlFields(
      boolean includeValueField, boolean includeDisplayField) {
    Attribute attribute = attributeField.getAttribute();
    SqlField valueField = indexTable.getAttributeValueField(attribute.getName());
    if (attribute.hasRuntimeSqlFunctionWrapper()) {
      valueField = valueField.cloneWithFunctionWrapper(attribute.getRuntimeSqlFunctionWrapper());
    }

    SqlQueryField valueSqlQueryField = SqlQueryField.of(valueField, getValueFieldAlias());
    if (attribute.isSimple() || attributeField.isExcludeDisplay()) {
      return List.of(valueSqlQueryField);
    }

    SqlField displayField = indexTable.getAttributeDisplayField(attribute.getName());
    SqlQueryField displaySqlQueryField = SqlQueryField.of(displayField, getDisplayFieldAlias());
    List<SqlQueryField> sqlQueryFields = new ArrayList<>();
    if (includeValueField) {
      sqlQueryFields.add(valueSqlQueryField);
    }
    if (includeDisplayField) {
      sqlQueryFields.add(displaySqlQueryField);
    }
    return sqlQueryFields;
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
      Literal displayField = sqlRowResult.get(getDisplayFieldAlias(), DataType.STRING);
      return new ValueDisplay(valueField, displayField.getStringVal());
    }
  }

  public ValueDisplay parseValueDisplayFromCountResult(
      SqlRowResult sqlRowResult, HintQueryResult entityLevelHints) {
    Literal valueField =
        sqlRowResult.get(getValueFieldAlias(), attributeField.getAttribute().getRuntimeDataType());

    if (attributeField.getAttribute().isSimple() || attributeField.isExcludeDisplay()) {
      if (attributeField.isExcludeDisplay()) {
        LOGGER.debug(
            "Skipping the entity-level hint. isSimple={}, isExcludeDisplay={}",
            attributeField.getAttribute().isSimple(),
            attributeField.isExcludeDisplay());
      }
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

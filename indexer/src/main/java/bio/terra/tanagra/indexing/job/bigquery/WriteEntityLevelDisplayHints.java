package bio.terra.tanagra.indexing.job.bigquery;

import bio.terra.tanagra.api.shared.DataType;
import bio.terra.tanagra.api.shared.Literal;
import bio.terra.tanagra.api.shared.ValueDisplay;
import bio.terra.tanagra.exception.InvalidConfigException;
import bio.terra.tanagra.exception.SystemException;
import bio.terra.tanagra.indexing.job.BigQueryJob;
import bio.terra.tanagra.indexing.job.dataflow.beam.BigQueryBeamUtils;
import bio.terra.tanagra.query.bigquery.BQExecutor;
import bio.terra.tanagra.query.bigquery.BQExecutorInfrastructure;
import bio.terra.tanagra.query.bigquery.BQTable;
import bio.terra.tanagra.query.sql.SqlField;
import bio.terra.tanagra.query.sql.SqlParams;
import bio.terra.tanagra.query.sql.SqlQueryField;
import bio.terra.tanagra.query.sql.SqlQueryRequest;
import bio.terra.tanagra.underlay.entitymodel.Attribute;
import bio.terra.tanagra.underlay.entitymodel.Entity;
import bio.terra.tanagra.underlay.indextable.ITEntityLevelDisplayHints;
import bio.terra.tanagra.underlay.indextable.ITEntityMain;
import bio.terra.tanagra.underlay.serialization.SZIndexer;
import com.google.cloud.bigquery.Field;
import com.google.cloud.bigquery.FieldValue;
import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.Schema;
import com.google.cloud.bigquery.TableId;
import com.google.cloud.bigquery.TableResult;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WriteEntityLevelDisplayHints extends BigQueryJob {
  private static final Logger LOGGER = LoggerFactory.getLogger(WriteEntityLevelDisplayHints.class);
  private static final int MAX_ENUM_VALS_FOR_DISPLAY_HINT = 100;
  private static final String FLATTENED_ATTR_VAL_ALIAS = "flattenedAttrVal";
  private static final SqlField FLATTENED_ATTR_VAL_FIELD = SqlField.of(FLATTENED_ATTR_VAL_ALIAS);
  public static final String MIN_VAL_ALIAS = "minVal";
  public static final String MAX_VAL_ALIAS = "maxVal";
  public static final String ENUM_VAL_ALIAS = "enumVal";
  public static final String ENUM_DISP_ALIAS = "enumDisp";
  public static final String ENUM_COUNT_ALIAS = "enumCount";
  private final Entity entity;
  private final ITEntityMain indexAttributesTable;
  private final ITEntityLevelDisplayHints indexHintsTable;

  public WriteEntityLevelDisplayHints(
      SZIndexer indexerConfig,
      Entity entity,
      ITEntityMain indexAttributesTable,
      ITEntityLevelDisplayHints indexHintsTable) {
    super(indexerConfig);
    this.entity = entity;
    this.indexAttributesTable = indexAttributesTable;
    this.indexHintsTable = indexHintsTable;
  }

  @Override
  public String getEntity() {
    return entity.getName();
  }

  @Override
  protected String getOutputTableName() {
    return indexHintsTable.getTablePointer().getTableName();
  }

  @Override
  public JobStatus checkStatus() {
    return getOutputTable().isPresent() ? JobStatus.COMPLETE : JobStatus.NOT_STARTED;
  }

  @Override
  public void run(boolean isDryRun) {
    // Build the column schemas.
    List<Field> fields =
        indexHintsTable.getColumnSchemas().stream()
            .map(
                columnSchema ->
                    Field.newBuilder(
                            columnSchema.getColumnName(),
                            BigQueryBeamUtils.fromDataType(columnSchema.getDataType()))
                        .build())
            .collect(Collectors.toList());

    // Create an empty table with this schema.
    TableId destinationTable =
        TableId.of(
            indexerConfig.bigQuery.indexData.projectId,
            indexerConfig.bigQuery.indexData.datasetId,
            getOutputTableName());
    googleBigQuery.createTableFromSchema(destinationTable, Schema.of(fields), null, isDryRun);

    // Calculate a display hint for each attribute. Build a list of all the hints as JSON records.
    List<List<Literal>> insertRows = new ArrayList<>();
    entity
        .getAttributes()
        .forEach(
            attribute -> {
              if (!attribute.isComputeDisplayHint()) {
                LOGGER.info("Skip computing the hint for attribute {}", attribute.getName());
              } else if (isRangeHint(attribute)) {
                Pair<Literal, Literal> minMax = computeRangeHint(attribute, isDryRun);
                insertRows.add(buildRowOfLiterals(attribute, minMax));

              } else if (isEnumHintForValueDisplay(attribute)) {
                List<Pair<ValueDisplay, Long>> enumCounts =
                    computeEnumHintForValueDisplay(attribute, isDryRun);
                insertRows.addAll(buildRowOfLiteralsValueDisplay(attribute, enumCounts));

              } else if (isEnumHintForRepeatedStringValue(attribute)) {
                List<Pair<Literal, Long>> enumCounts =
                    computeEnumHintForRepeatedStringValue(attribute, isDryRun);
                insertRows.addAll(buildRowOfLiteralsRepeatedString(attribute, enumCounts));

              } else {
                LOGGER.info(
                    "Attribute {} data type {} not yet supported for computing hint",
                    attribute.getName(),
                    attribute.getDataType());
              }
            });

    if (insertRows.isEmpty()) {
      LOGGER.info("No display hints to insert.");
      return;
    }

    if (!isDryRun) {
      // Poll for table existence before we try to insert the hint rows. Maximum 18 x 10 sec = 3
      // min.
      googleBigQuery.pollForTableExistenceOrThrow(
          indexerConfig.bigQuery.indexData.projectId,
          indexerConfig.bigQuery.indexData.datasetId,
          getOutputTableName(),
          18,
          Duration.ofSeconds(10));

      // Sleep before inserting.
      // Sometimes even though the table is found in the existence check above, it still fails the
      // insert below.
      try {
        TimeUnit.SECONDS.sleep(30);
      } catch (InterruptedException intEx) {
        throw new SystemException(
            "Interrupted during sleep after creating entity-level hints table", intEx);
      }
    }

    // Do a single insert to BQ for all the hint rows.
    SqlParams sqlParams = new SqlParams();
    List<String> insertRowSqls = new ArrayList<>();
    for (List<Literal> insertRow : insertRows) {
      List<String> paramNames = new ArrayList<>();
      for (Literal literal : insertRow) {
        paramNames.add("@" + sqlParams.addParam("valr", literal));
      }
      insertRowSqls.add("(" + String.join(",", paramNames) + ")");
    }
    String rowsOfLiteralsSql = String.join(", ", insertRowSqls);
    String insertLiteralsSql =
        "INSERT INTO "
            + indexHintsTable.getTablePointer().render()
            + " ("
            + ITEntityLevelDisplayHints.Column.ATTRIBUTE_NAME.getSchema().getColumnName()
            + ", "
            + ITEntityLevelDisplayHints.Column.MIN.getSchema().getColumnName()
            + ", "
            + ITEntityLevelDisplayHints.Column.MAX.getSchema().getColumnName()
            + ", "
            + ITEntityLevelDisplayHints.Column.ENUM_VALUE.getSchema().getColumnName()
            + ", "
            + ITEntityLevelDisplayHints.Column.ENUM_DISPLAY.getSchema().getColumnName()
            + ", "
            + ITEntityLevelDisplayHints.Column.ENUM_COUNT.getSchema().getColumnName()
            + ") VALUES "
            + rowsOfLiteralsSql;
    if (isDryRun && getOutputTable().isEmpty()) {
      LOGGER.info("Skipping query dry run because output table does not exist yet.");
    } else {
      BQExecutor bqExecutor =
          new BQExecutor(BQExecutorInfrastructure.forQuery(indexerConfig.bigQuery.queryProjectId));
      SqlQueryRequest sqlQueryRequest =
          new SqlQueryRequest(insertLiteralsSql, sqlParams, null, null, isDryRun);
      bqExecutor.run(sqlQueryRequest);
    }
  }

  public static boolean isEnumHintForValueDisplay(Attribute attribute) {
    return (attribute.isValueDisplay() && DataType.INT64.equals(attribute.getRuntimeDataType()))
        || (attribute.isSimple()
            && !attribute.isDataTypeRepeated()
            && DataType.STRING.equals(attribute.getRuntimeDataType()));
  }

  public static boolean isEnumHintForRepeatedStringValue(Attribute attribute) {
    // TODO: Support not-repeated string simple attributes & repeated integer attributes.
    return attribute.isSimple()
        && attribute.isDataTypeRepeated()
        && (DataType.STRING.equals(attribute.getDataType()));
  }

  public static boolean isRangeHint(Attribute attribute) {
    if (attribute.isValueDisplay()) {
      return false;
    }
    return switch (attribute.getRuntimeDataType()) {
      case DOUBLE, INT64 -> !attribute.isDataTypeRepeated();
      default -> false;
    };
  }

  private List<Literal> buildRowOfLiterals(Attribute attribute, Pair<Literal, Literal> minMax) {
    List<Literal> rowOfLiterals = new ArrayList<>();
    rowOfLiterals.add(Literal.forString(attribute.getName()));
    rowOfLiterals.add(minMax.getKey());
    rowOfLiterals.add(minMax.getValue());
    rowOfLiterals.add(Literal.forInt64(null));
    rowOfLiterals.add(Literal.forString(null));
    rowOfLiterals.add(Literal.forInt64(null));
    LOGGER.info(
        "Numeric range hint: {}, {}, {}", attribute.getName(), minMax.getKey(), minMax.getValue());
    return rowOfLiterals;
  }

  private List<List<Literal>> buildRowOfLiteralsValueDisplay(
      Attribute attribute, List<Pair<ValueDisplay, Long>> enumCounts) {
    return enumCounts.stream()
        .map(
            enumCount -> {
              Literal int64Field =
                  attribute.isValueDisplay()
                      ? enumCount.getKey().getValue()
                      : Literal.forInt64(null);
              Literal stringField =
                  attribute.isValueDisplay()
                      ? Literal.forString(enumCount.getKey().getDisplay())
                      : enumCount.getKey().getValue();

              List<Literal> rowOfLiterals = new ArrayList<>();
              rowOfLiterals.add(Literal.forString(attribute.getName()));
              rowOfLiterals.add(Literal.forDouble(null));
              rowOfLiterals.add(Literal.forDouble(null));
              rowOfLiterals.add(int64Field);
              rowOfLiterals.add(stringField);
              rowOfLiterals.add(Literal.forInt64(enumCount.getValue()));
              LOGGER.info(
                  "Enum value-display or simple-string hint: {}, {}, {}, {}",
                  attribute.getName(),
                  int64Field,
                  stringField,
                  enumCount.getValue());
              return rowOfLiterals;
            })
        .toList();
  }

  private List<List<Literal>> buildRowOfLiteralsRepeatedString(
      Attribute attribute, List<Pair<Literal, Long>> enumCounts) {
    return enumCounts.stream()
        .map(
            enumCount -> {
              List<Literal> rowOfLiterals = new ArrayList<>();
              rowOfLiterals.add(Literal.forString(attribute.getName()));
              rowOfLiterals.add(Literal.forDouble(null));
              rowOfLiterals.add(Literal.forDouble(null));
              rowOfLiterals.add(Literal.forInt64(null));
              rowOfLiterals.add(enumCount.getKey());
              rowOfLiterals.add(Literal.forInt64(enumCount.getValue()));
              LOGGER.info(
                  "Enum repeated-string hint: {}, {}, {}, {}",
                  attribute.getName(),
                  null,
                  enumCount.getKey(),
                  enumCount.getValue());
              return rowOfLiterals;
            })
        .toList();
  }

  private Pair<Literal, Literal> computeRangeHint(Attribute attribute, boolean isDryRun) {
    String selectMinMaxSql = buildRangeHintSql(indexAttributesTable, attribute, null);

    // Execute the query.
    if (isDryRun) {
      if (getOutputTable().isEmpty()) {
        LOGGER.info("Skipping query dry run because output table does not exist yet.");
      } else {
        googleBigQuery.dryRunQuery(selectMinMaxSql);
      }
      return Pair.of(Literal.forDouble(0.0), Literal.forDouble(0.0));
    } else {
      // Parse the result row.
      TableResult tableResult = googleBigQuery.runQueryLongTimeout(selectMinMaxSql);
      Iterator<FieldValueList> rowResults = tableResult.getValues().iterator();
      if (rowResults.hasNext()) {
        FieldValueList rowResult = rowResults.next();
        FieldValue minFieldValue = rowResult.get(MIN_VAL_ALIAS);
        Literal minVal =
            Literal.forDouble(minFieldValue.isNull() ? null : minFieldValue.getDoubleValue());
        FieldValue maxFieldValue = rowResult.get(MAX_VAL_ALIAS);
        Literal maxVal =
            Literal.forDouble(maxFieldValue.isNull() ? null : maxFieldValue.getDoubleValue());
        return Pair.of(minVal, maxVal);
      } else {
        return Pair.of(Literal.forDouble(null), Literal.forDouble(null));
      }
    }
  }

  public static String buildRangeHintSql(
      ITEntityMain indexAttributesTable, Attribute attribute, String innerSqlSelector) {
    // Build the query.
    // SELECT MIN(attr) AS minVal, MAX(attr) AS maxVal FROM (SELECT * FROM indextable)
    SqlField attrField =
        SqlField.of(
            indexAttributesTable.getAttributeValueField(attribute.getName()).getColumnName(),
            attribute.hasRuntimeSqlFunctionWrapper()
                ? attribute.getRuntimeSqlFunctionWrapper()
                : null);

    String innerSuffix = StringUtils.EMPTY;
    if (innerSqlSelector != null) {
      innerSuffix = " WHERE " + innerSqlSelector;
    }

    BQTable innerQueryTable =
        new BQTable(
            "SELECT "
                + SqlQueryField.of(attrField, attribute.getName()).renderForSelect()
                + " FROM "
                + indexAttributesTable.getTablePointer().render()
                + innerSuffix);

    String selectMinMaxSql =
        "SELECT "
            + SqlQueryField.of(attrField.cloneWithFunctionWrapper("MIN"), MIN_VAL_ALIAS)
                .renderForSelect()
            + ", "
            + SqlQueryField.of(attrField.cloneWithFunctionWrapper("MAX"), MAX_VAL_ALIAS)
                .renderForSelect()
            + " FROM "
            + innerQueryTable.render();

    LOGGER.info("SQL numeric range: {}", selectMinMaxSql);
    return selectMinMaxSql;
  }

  private List<Pair<ValueDisplay, Long>> computeEnumHintForValueDisplay(
      Attribute attribute, boolean isDryRun) {
    String selectEnumCountSql =
        buildEnumHintForValueDisplaySql(indexAttributesTable, attribute, null);

    // Execute the query.
    List<Pair<ValueDisplay, Long>> enumCounts = new ArrayList<>();
    if (isDryRun) {
      if (getOutputTable().isEmpty()) {
        LOGGER.info("Skipping query dry run because output table does not exist yet.");
      } else {
        googleBigQuery.dryRunQuery(selectEnumCountSql);
      }
      enumCounts.add(Pair.of(new ValueDisplay(Literal.forInt64(0L), "ZERO"), 0L));
    } else {
      TableResult tableResult = googleBigQuery.runQueryLongTimeout(selectEnumCountSql);

      // Parse the result rows.
      for (FieldValueList rowResult : tableResult.getValues()) {
        FieldValue enumValFieldValue = rowResult.get(ENUM_VAL_ALIAS);
        Literal enumVal;
        String enumDisp;
        if (attribute.isValueDisplay()) {
          enumVal =
              Literal.forInt64(
                  enumValFieldValue.isNull() ? null : enumValFieldValue.getLongValue());
          FieldValue enumDispFieldValue = rowResult.get(ENUM_DISP_ALIAS);
          enumDisp = enumDispFieldValue.isNull() ? null : enumDispFieldValue.getStringValue();
        } else {
          enumVal =
              Literal.forString(
                  enumValFieldValue.isNull() ? null : enumValFieldValue.getStringValue());
          enumDisp = null;
        }
        FieldValue enumCountFieldValue = rowResult.get(ENUM_COUNT_ALIAS);
        long enumCount = enumCountFieldValue.getLongValue();
        enumCounts.add(Pair.of(new ValueDisplay(enumVal, enumDisp), enumCount));

        if (enumCounts.size() > MAX_ENUM_VALS_FOR_DISPLAY_HINT) {
          // If there are more than the max number of values, then skip the display hint.
          LOGGER.info(
              "Skipping enum values display hint because there are >{} possible values: {}",
              MAX_ENUM_VALS_FOR_DISPLAY_HINT,
              attribute.getName());
          return List.of();
        }
      }
    }

    if (attribute.isValueDisplay()) {
      // Check that there is exactly one display per value.
      Map<Literal, String> valDisplay = new HashMap<>();
      enumCounts.forEach(
          enumCount -> {
            if (valDisplay.containsKey(enumCount.getKey().getValue())) {
              throw new InvalidConfigException(
                  "Found >1 possible display for the enum value " + enumCount.getKey().getValue());
            } else {
              valDisplay.put(enumCount.getKey().getValue(), enumCount.getKey().getDisplay());
            }
          });
    }
    return enumCounts;
  }

  public static String buildEnumHintForValueDisplaySql(
      ITEntityMain indexAttributesTable, Attribute attribute, String innerSqlSelector) {
    // Build the query.
    // SELECT attrVal AS enumVal[, attrDisp AS enumDisp], COUNT(*) AS enumCount FROM indextable
    // GROUP BY enumVal[, enumDisp]
    SqlField attrValField = indexAttributesTable.getAttributeValueField(attribute.getName());
    SqlField attrDispField =
        attribute.isValueDisplay()
            ? indexAttributesTable.getAttributeDisplayField(attribute.getName())
            : null;

    String innerSuffix = StringUtils.EMPTY;
    if (innerSqlSelector != null) {
      innerSuffix = " WHERE " + innerSqlSelector;
    }

    String selectEnumCountSql =
        "SELECT " + SqlQueryField.of(attrValField, ENUM_VAL_ALIAS).renderForSelect();
    if (attribute.isValueDisplay()) {
      selectEnumCountSql +=
          ", " + SqlQueryField.of(attrDispField, ENUM_DISP_ALIAS).renderForSelect();
    }
    selectEnumCountSql +=
        ", COUNT(*) AS "
            + ENUM_COUNT_ALIAS
            + " FROM "
            + indexAttributesTable.getTablePointer().render()
            + innerSuffix
            + " GROUP BY "
            + SqlQueryField.of(attrValField, ENUM_VAL_ALIAS).renderForGroupBy(null, true);
    if (attribute.isValueDisplay()) {
      selectEnumCountSql +=
          ", " + SqlQueryField.of(attrDispField, ENUM_DISP_ALIAS).renderForGroupBy(null, true);
    }

    LOGGER.info("SQL enum count: {}", selectEnumCountSql);
    return selectEnumCountSql;
  }

  private List<Pair<Literal, Long>> computeEnumHintForRepeatedStringValue(
      Attribute attribute, boolean isDryRun) {
    String selectEnumCountSql =
        buildEnumHintForRepeatedStringValueSql(indexAttributesTable, attribute, null);

    // Execute the query.
    List<Pair<Literal, Long>> enumCounts = new ArrayList<>();
    if (isDryRun) {
      if (getOutputTable().isEmpty()) {
        LOGGER.info("Skipping query dry run because output table does not exist yet.");
      } else {
        googleBigQuery.dryRunQuery(selectEnumCountSql);
      }
      enumCounts.add(Pair.of(Literal.forString(""), 0L));
    } else {
      TableResult tableResult = googleBigQuery.runQueryLongTimeout(selectEnumCountSql);

      // Parse the result rows.
      for (FieldValueList rowResult : tableResult.getValues()) {
        FieldValue enumValFieldValue = rowResult.get(ENUM_VAL_ALIAS);
        Literal enumVal =
            Literal.forString(
                enumValFieldValue.isNull() ? null : enumValFieldValue.getStringValue());
        FieldValue enumCountFieldValue = rowResult.get(ENUM_COUNT_ALIAS);
        long enumCount = enumCountFieldValue.getLongValue();
        enumCounts.add(Pair.of(enumVal, enumCount));

        if (enumCounts.size() > MAX_ENUM_VALS_FOR_DISPLAY_HINT) {
          // if there are more than the max number of values, then skip the display hint
          LOGGER.info(
              "Skipping enum values display hint because there are >{} possible values: {}",
              MAX_ENUM_VALS_FOR_DISPLAY_HINT,
              attribute.getName());
          return List.of();
        }
      }
    }
    return enumCounts;
  }

  public static String buildEnumHintForRepeatedStringValueSql(
      ITEntityMain indexAttributesTable, Attribute attribute, String innerSqlSelector) {
    // TODO: Consolidate the logic here with the ValueDisplay enum hint method.
    // Build the query.
    // SELECT flattenedAttrVal AS enumVal, COUNT(*) AS enumCount FROM indextable
    // CROSS JOIN UNNEST(indextable.attrVal) AS flattenedAttrVal
    // GROUP BY enumVal
    SqlField attrValField = indexAttributesTable.getAttributeValueField(attribute.getName());

    String innerSuffix = StringUtils.EMPTY;
    if (innerSqlSelector != null) {
      innerSuffix = " WHERE " + innerSqlSelector;
    }

    String selectEnumCountSql =
        "SELECT "
            + SqlQueryField.of(FLATTENED_ATTR_VAL_FIELD, ENUM_VAL_ALIAS).renderForSelect()
            + ", COUNT(*) AS "
            + ENUM_COUNT_ALIAS
            + " FROM "
            + indexAttributesTable.getTablePointer().render()
            + " CROSS JOIN UNNEST("
            + SqlQueryField.of(attrValField).renderForSelect()
            + ") AS "
            + FLATTENED_ATTR_VAL_ALIAS
            + innerSuffix
            + " GROUP BY "
            + SqlQueryField.of(attrValField, ENUM_VAL_ALIAS).renderForGroupBy(null, true);

    LOGGER.info("SQL enum count: {}", selectEnumCountSql);
    return selectEnumCountSql;
  }
}

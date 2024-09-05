package bio.terra.tanagra.indexing.job.bigquery;

import bio.terra.tanagra.api.shared.DataType;
import bio.terra.tanagra.api.shared.Literal;
import bio.terra.tanagra.api.shared.ValueDisplay;
import bio.terra.tanagra.exception.InvalidConfigException;
import bio.terra.tanagra.exception.SystemException;
import bio.terra.tanagra.indexing.job.BigQueryJob;
import bio.terra.tanagra.indexing.job.dataflow.beam.BigQueryBeamUtils;
import bio.terra.tanagra.query.bigquery.*;
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
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WriteEntityLevelDisplayHints extends BigQueryJob {
  private static final Logger LOGGER = LoggerFactory.getLogger(WriteEntityLevelDisplayHints.class);
  private static final int MAX_ENUM_VALS_FOR_DISPLAY_HINT = 100;

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
                List<Literal> insertRow = new ArrayList<>();
                insertRow.add(Literal.forString(attribute.getName()));
                insertRow.add(minMax.getKey());
                insertRow.add(minMax.getValue());
                insertRow.add(Literal.forInt64(null));
                insertRow.add(Literal.forString(null));
                insertRow.add(Literal.forInt64(null));
                insertRows.add(insertRow);
                LOGGER.info(
                    "Numeric range hint: {}, {}, {}",
                    attribute.getName(),
                    minMax.getKey(),
                    minMax.getValue());
              } else if (isEnumHint(attribute)) {
                List<Pair<ValueDisplay, Long>> enumCounts = computeEnumHint(attribute, isDryRun);
                enumCounts.forEach(
                    enumCount -> {
                      List<Literal> rowOfLiterals = new ArrayList<>();
                      rowOfLiterals.add(Literal.forString(attribute.getName()));
                      rowOfLiterals.add(Literal.forDouble(null));
                      rowOfLiterals.add(Literal.forDouble(null));
                      rowOfLiterals.add(enumCount.getKey().getValue());
                      rowOfLiterals.add(Literal.forString(enumCount.getKey().getDisplay()));
                      rowOfLiterals.add(Literal.forInt64(enumCount.getValue()));
                      insertRows.add(rowOfLiterals);
                      LOGGER.info(
                          "Enum hint: {}, {}, {}, {}",
                          attribute.getName(),
                          enumCount.getKey().getValue(),
                          enumCount.getKey().getDisplay(),
                          enumCount.getValue());
                    });
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

  private static boolean isEnumHint(Attribute attribute) {
    return attribute.isValueDisplay() && DataType.INT64.equals(attribute.getRuntimeDataType());
  }

  private static boolean isRangeHint(Attribute attribute) {
    if (attribute.isValueDisplay()) {
      return false;
    }
    return switch (attribute.getRuntimeDataType()) {
      case DOUBLE, INT64 -> true;
      default -> false;
    };
  }

  private Pair<Literal, Literal> computeRangeHint(Attribute attribute, boolean isDryRun) {
    // Build the query.
    // SELECT MIN(attr) AS minVal, MAX(attr) AS maxVal FROM (SELECT * FROM indextable)

    SqlField attrField =
        SqlField.of(
            indexAttributesTable.getAttributeValueField(attribute.getName()).getColumnName(),
            attribute.hasRuntimeSqlFunctionWrapper()
                ? attribute.getRuntimeSqlFunctionWrapper()
                : null);
    BQTable innerQueryTable =
        new BQTable(
            "SELECT "
                + SqlQueryField.of(attrField, attribute.getName()).renderForSelect()
                + " FROM "
                + indexAttributesTable.getTablePointer().render());
    final String minValAlias = "minVal";
    final String maxValAlias = "maxVal";
    String selectMinMaxSql =
        "SELECT "
            + SqlQueryField.of(attrField.cloneWithFunctionWrapper("MIN"), minValAlias)
                .renderForSelect()
            + ", "
            + SqlQueryField.of(attrField.cloneWithFunctionWrapper("MAX"), maxValAlias)
                .renderForSelect()
            + " FROM "
            + innerQueryTable.render();
    LOGGER.info("SQL numeric range: {}", selectMinMaxSql);

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
        FieldValue minFieldValue = rowResult.get(minValAlias);
        Literal minVal =
            Literal.forDouble(minFieldValue.isNull() ? null : minFieldValue.getDoubleValue());
        FieldValue maxFieldValue = rowResult.get(maxValAlias);
        Literal maxVal =
            Literal.forDouble(maxFieldValue.isNull() ? null : maxFieldValue.getDoubleValue());
        return Pair.of(minVal, maxVal);
      } else {
        return Pair.of(Literal.forDouble(null), Literal.forDouble(null));
      }
    }
  }

  private List<Pair<ValueDisplay, Long>> computeEnumHint(Attribute attribute, boolean isDryRun) {
    // Build the query.
    // SELECT attrVal AS enumVal, attrDisp AS enumDisp, COUNT(*) AS enumCount FROM indextable GROUP
    // BY enumVal, enumDisp
    SqlField attrValField = indexAttributesTable.getAttributeValueField(attribute.getName());
    SqlField attrDispField = indexAttributesTable.getAttributeDisplayField(attribute.getName());
    final String enumValAlias = "enumVal";
    final String enumDispAlias = "enumDisp";
    final String enumCountAlias = "enumCount";

    String selectEnumCountSql =
        "SELECT "
            + SqlQueryField.of(attrValField, enumValAlias).renderForSelect()
            + ", "
            + SqlQueryField.of(attrDispField, enumDispAlias).renderForSelect()
            + ", COUNT(*) AS "
            + enumCountAlias
            + " FROM "
            + indexAttributesTable.getTablePointer().render()
            + " GROUP BY "
            + SqlQueryField.of(attrValField, enumValAlias).renderForGroupBy(null, true)
            + ", "
            + SqlQueryField.of(attrDispField, enumDispAlias).renderForGroupBy(null, true);
    LOGGER.info("SQL enum count: {}", selectEnumCountSql);

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
        FieldValue enumValFieldValue = rowResult.get(enumValAlias);
        Literal enumVal =
            Literal.forInt64(enumValFieldValue.isNull() ? null : enumValFieldValue.getLongValue());
        FieldValue enumDispFieldValue = rowResult.get(enumDispAlias);
        String enumDisp = enumDispFieldValue.isNull() ? null : enumDispFieldValue.getStringValue();
        FieldValue enumCountFieldValue = rowResult.get(enumCountAlias);
        long enumCount = enumCountFieldValue.getLongValue();
        enumCounts.add(Pair.of(new ValueDisplay(enumVal, enumDisp), enumCount));

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
    return enumCounts;
  }
}

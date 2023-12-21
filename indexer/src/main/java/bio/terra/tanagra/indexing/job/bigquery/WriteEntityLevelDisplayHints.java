package bio.terra.tanagra.indexing.job.bigquery;

import bio.terra.tanagra.api.query.ValueDisplay;
import bio.terra.tanagra.exception.InvalidConfigException;
import bio.terra.tanagra.exception.SystemException;
import bio.terra.tanagra.indexing.job.BigQueryJob;
import bio.terra.tanagra.indexing.job.dataflow.beam.BigQueryBeamUtils;
import bio.terra.tanagra.query.FieldPointer;
import bio.terra.tanagra.query.Literal;
import bio.terra.tanagra.query.TablePointer;
import bio.terra.tanagra.query2.bigquery.BQTranslator;
import bio.terra.tanagra.query2.sql.SqlField;
import bio.terra.tanagra.underlay.entitymodel.Attribute;
import bio.terra.tanagra.underlay.entitymodel.Entity;
import bio.terra.tanagra.underlay.indextable.ITEntityLevelDisplayHints;
import bio.terra.tanagra.underlay.indextable.ITEntityMain;
import bio.terra.tanagra.underlay.serialization.SZIndexer;
import com.google.cloud.bigquery.Field;
import com.google.cloud.bigquery.FieldValue;
import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.JobStatistics;
import com.google.cloud.bigquery.QueryJobConfiguration;
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
                            BigQueryBeamUtils.fromSqlDataType(columnSchema.getSqlDataType()))
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
    entity.getAttributes().stream()
        .forEach(
            attribute -> {
              if (!attribute.isComputeDisplayHint()) {
                LOGGER.info("Skip computing the hint for attribute {}", attribute.getName());
              } else if (isRangeHint(attribute)) {
                Pair<Literal, Literal> minMax = computeRangeHint(attribute, isDryRun);
                List<Literal> insertRow = new ArrayList<>();
                insertRow.add(new Literal(attribute.getName()));
                insertRow.add(minMax.getKey());
                insertRow.add(minMax.getValue());
                insertRow.add(new Literal(null));
                insertRow.add(new Literal(null));
                insertRow.add(new Literal(null));
                insertRows.add(insertRow);
                LOGGER.info(
                    "Numeric range hint: {}, {}, {}",
                    attribute.getName(),
                    minMax.getKey(),
                    minMax.getValue());
              } else if (isEnumHint(attribute)) {
                List<Pair<ValueDisplay, Long>> enumCounts = computeEnumHint(attribute, isDryRun);
                enumCounts.stream()
                    .forEach(
                        enumCount -> {
                          List<Literal> rowOfLiterals = new ArrayList<>();
                          rowOfLiterals.add(new Literal(attribute.getName()));
                          rowOfLiterals.add(new Literal(null));
                          rowOfLiterals.add(new Literal(null));
                          rowOfLiterals.add(enumCount.getKey().getValue());
                          rowOfLiterals.add(new Literal(enumCount.getKey().getDisplay()));
                          rowOfLiterals.add(new Literal(enumCount.getValue()));
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
    String rowsOfLiteralsSql =
        insertRows.stream()
            .map(
                row ->
                    "("
                        + row.stream().map(Literal::renderSQL).collect(Collectors.joining(","))
                        + ")")
            .collect(Collectors.joining(", "));
    String insertLiteralsSql =
        "INSERT INTO "
            + indexHintsTable.getTablePointer().renderSQL()
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
    QueryJobConfiguration queryConfig =
        QueryJobConfiguration.newBuilder(insertLiteralsSql)
            .setDryRun(isDryRun)
            .setUseLegacySql(false)
            .build();
    if (isDryRun) {
      JobStatistics.QueryStatistics queryStatistics = googleBigQuery.queryStatistics(queryConfig);
      LOGGER.info(
          "SQL dry run: statementType={}, cacheHit={}, totalBytesProcessed={}, totalSlotMs={}",
          queryStatistics.getStatementType(),
          queryStatistics.getCacheHit(),
          queryStatistics.getTotalBytesProcessed(),
          queryStatistics.getTotalSlotMs());
    } else {
      TableResult tableResult = googleBigQuery.queryBigQuery(queryConfig, null, null);
      LOGGER.info("SQL query returns {} rows across all pages", tableResult.getTotalRows());
    }
  }

  private static boolean isEnumHint(Attribute attribute) {
    return attribute.isValueDisplay()
        && Literal.DataType.INT64.equals(attribute.getRuntimeDataType());
  }

  private static boolean isRangeHint(Attribute attribute) {
    if (attribute.isValueDisplay()) {
      return false;
    }
    switch (attribute.getRuntimeDataType()) {
      case DOUBLE:
      case INT64:
        return true;
      default:
        return false;
    }
  }

  private Pair<Literal, Literal> computeRangeHint(Attribute attribute, boolean isDryRun) {
    // Build the query.
    // SELECT MIN(attr) AS minVal, MAX(attr) AS maxVal FROM (SELECT * FROM indextable)
    BQTranslator bqTranslator = new BQTranslator();

    TablePointer innerQueryTable =
        new TablePointer("SELECT * FROM " + indexAttributesTable.getTablePointer().renderSQL());
    FieldPointer attrField =
        new FieldPointer.Builder()
            .tablePointer(innerQueryTable)
            .columnName(
                indexAttributesTable.getAttributeValueField(attribute.getName()).getColumnName())
            .build();
    final String minValAlias = "minVal";
    final String maxValAlias = "maxVal";
    String selectMinMaxSql =
        "SELECT "
            + bqTranslator.selectSql(
                SqlField.of(attrField.toBuilder().sqlFunctionWrapper("MIN").build(), minValAlias),
                null)
            + ", "
            + bqTranslator.selectSql(
                SqlField.of(attrField.toBuilder().sqlFunctionWrapper("MAX").build(), maxValAlias),
                null)
            + " FROM "
            + innerQueryTable.renderSQL();

    // Execute the query.
    QueryJobConfiguration queryConfig =
        QueryJobConfiguration.newBuilder(selectMinMaxSql)
            .setDryRun(isDryRun)
            .setUseLegacySql(false)
            .build();
    if (isDryRun) {
      JobStatistics.QueryStatistics queryStatistics = googleBigQuery.queryStatistics(queryConfig);
      LOGGER.info(
          "SQL dry run: statementType={}, cacheHit={}, totalBytesProcessed={}, totalSlotMs={}",
          queryStatistics.getStatementType(),
          queryStatistics.getCacheHit(),
          queryStatistics.getTotalBytesProcessed(),
          queryStatistics.getTotalSlotMs());
      return Pair.of(new Literal(0.0), new Literal(0.0));
    } else {
      // Parse the result row.
      TableResult tableResult = googleBigQuery.queryBigQuery(queryConfig, null, null);
      LOGGER.info("SQL query returns {} rows across all pages", tableResult.getTotalRows());
      Iterator<FieldValueList> rowResults = tableResult.getValues().iterator();
      if (rowResults.hasNext()) {
        FieldValueList rowResult = rowResults.next();
        FieldValue minFieldValue = rowResult.get(minValAlias);
        Literal minVal =
            minFieldValue.isNull()
                ? new Literal(null)
                : new Literal(minFieldValue.getDoubleValue());
        FieldValue maxFieldValue = rowResult.get(maxValAlias);
        Literal maxVal =
            maxFieldValue.isNull()
                ? new Literal(null)
                : new Literal(maxFieldValue.getDoubleValue());
        return Pair.of(minVal, maxVal);
      } else {
        return Pair.of(new Literal(null), new Literal(null));
      }
    }
  }

  private List<Pair<ValueDisplay, Long>> computeEnumHint(Attribute attribute, boolean isDryRun) {
    // Build the query.
    // SELECT attrVal AS enumVal, attrDisp AS enumDisp, COUNT(*) AS enumCount FROM indextable GROUP
    // BY enumVal, enumDisp
    FieldPointer attrValField = indexAttributesTable.getAttributeValueField(attribute.getName());
    FieldPointer attrDispField = indexAttributesTable.getAttributeDisplayField(attribute.getName());
    final String enumValAlias = "enumVal";
    final String enumDispAlias = "enumDisp";
    final String enumCountAlias = "enumCount";

    BQTranslator bqTranslator = new BQTranslator();
    String selectEnumCountSql =
        "SELECT "
            + bqTranslator.selectSql(SqlField.of(attrValField, enumValAlias), null)
            + ", "
            + bqTranslator.selectSql(SqlField.of(attrDispField, enumDispAlias), null)
            + ", COUNT(*) AS "
            + enumCountAlias
            + " FROM "
            + indexAttributesTable.getTablePointer().renderSQL()
            + " GROUP BY "
            + bqTranslator.groupBySql(SqlField.of(attrValField, enumValAlias), null, true)
            + ", "
            + bqTranslator.groupBySql(SqlField.of(attrDispField, enumDispAlias), null, true);

    // Execute the query.
    QueryJobConfiguration queryConfig =
        QueryJobConfiguration.newBuilder(selectEnumCountSql)
            .setDryRun(isDryRun)
            .setUseLegacySql(false)
            .build();

    // Parse the result rows.
    List<Pair<ValueDisplay, Long>> enumCounts = new ArrayList<>();
    if (isDryRun) {
      JobStatistics.QueryStatistics queryStatistics = googleBigQuery.queryStatistics(queryConfig);
      LOGGER.info(
          "SQL dry run: statementType={}, cacheHit={}, totalBytesProcessed={}, totalSlotMs={}",
          queryStatistics.getStatementType(),
          queryStatistics.getCacheHit(),
          queryStatistics.getTotalBytesProcessed(),
          queryStatistics.getTotalSlotMs());
      enumCounts.add(Pair.of(new ValueDisplay(new Literal(0L), "ZERO"), 0L));
    } else {
      TableResult tableResult = googleBigQuery.queryBigQuery(queryConfig, null, null);
      LOGGER.info("SQL query returns {} rows across all pages", tableResult.getTotalRows());
      Iterator<FieldValueList> rowResults = tableResult.getValues().iterator();
      while (rowResults.hasNext()) {
        FieldValueList rowResult = rowResults.next();
        FieldValue enumValFieldValue = rowResult.get(enumValAlias);
        Literal enumVal =
            enumValFieldValue.isNull()
                ? new Literal(null)
                : new Literal(enumValFieldValue.getLongValue());
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
    enumCounts.stream()
        .forEach(
            enumCount -> {
              if (valDisplay.containsKey(enumCount.getKey().getValue())) {
                throw new InvalidConfigException(
                    "Found >1 possible display for the enum value "
                        + enumCount.getKey().getValue());
              } else {
                valDisplay.put(enumCount.getKey().getValue(), enumCount.getKey().getDisplay());
              }
            });
    return enumCounts;
  }
}

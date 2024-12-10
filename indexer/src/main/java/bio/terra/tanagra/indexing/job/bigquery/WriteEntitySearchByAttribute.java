package bio.terra.tanagra.indexing.job.bigquery;

import bio.terra.tanagra.indexing.job.BigQueryJob;
import bio.terra.tanagra.indexing.job.dataflow.beam.BigQueryBeamUtils;
import bio.terra.tanagra.query.sql.SqlField;
import bio.terra.tanagra.query.sql.SqlQueryField;
import bio.terra.tanagra.underlay.entitymodel.Entity;
import bio.terra.tanagra.underlay.indextable.ITEntityMain;
import bio.terra.tanagra.underlay.indextable.ITEntitySearchByAttribute;
import bio.terra.tanagra.underlay.serialization.SZIndexer;
import com.google.cloud.bigquery.Clustering;
import com.google.cloud.bigquery.Field;
import com.google.cloud.bigquery.Schema;
import com.google.cloud.bigquery.TableId;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WriteEntitySearchByAttribute extends BigQueryJob {
  private static final Logger LOGGER = LoggerFactory.getLogger(WriteEntitySearchByAttribute.class);
  private static final String FLATTENED_ATTR_VAL_ALIAS = "flattenedAttrVal";
  private static final SqlField FLATTENED_ATTR_VAL_FIELD = SqlField.of(FLATTENED_ATTR_VAL_ALIAS);
  private final Entity entity;
  private final ITEntityMain entityTable;
  private final ITEntitySearchByAttribute searchTable;

  public WriteEntitySearchByAttribute(
      SZIndexer indexerConfig,
      Entity entity,
      ITEntityMain entityTable,
      ITEntitySearchByAttribute searchTable) {
    super(indexerConfig);
    this.entity = entity;
    this.entityTable = entityTable;
    this.searchTable = searchTable;
  }

  @Override
  public String getEntity() {
    return entity.getName();
  }

  @Override
  protected String getOutputTableName() {
    return searchTable.getTablePointer().getTableName();
  }

  @Override
  public JobStatus checkStatus() {
    return outputTableHasAtLeastOneRow() ? JobStatus.COMPLETE : JobStatus.NOT_STARTED;
  }

  @Override
  public void run(boolean isDryRun) {
    List<Field> fields =
        searchTable.getColumnSchemas().stream()
            .map(
                columnSchema ->
                    Field.newBuilder(
                            columnSchema.getColumnName(),
                            BigQueryBeamUtils.fromDataType(columnSchema.getDataType()))
                        .build())
            .collect(Collectors.toList());

    // Build a clustering specification.
    Clustering clustering =
        Clustering.newBuilder().setFields(List.of(searchTable.getAttribute())).build();

    // Create an empty table with this schema.
    TableId destinationTable =
        TableId.of(
            indexerConfig.bigQuery.indexData.projectId,
            indexerConfig.bigQuery.indexData.datasetId,
            getOutputTableName());
    googleBigQuery.createTableFromSchema(destinationTable, Schema.of(fields), clustering, isDryRun);

    // Build the query to insert to the search table using a select from the main entity table.
    String idAttribute = entity.getIdAttribute().getName();
    String searchAttribute = searchTable.getAttribute();
    List<String> insertColumns = List.of(idAttribute, searchAttribute);
    String selectSql;

    if (entity.getAttribute(searchAttribute).isDataTypeRepeated()) {
      List<String> selectColumns =
          List.of(
              idAttribute,
              SqlQueryField.of(FLATTENED_ATTR_VAL_FIELD, searchAttribute).renderForSelect());
      selectSql =
          "SELECT "
              + String.join(", ", selectColumns)
              + " FROM "
              + entityTable.getTablePointer().render()
              + " CROSS JOIN UNNEST("
              + searchAttribute
              + ") AS "
              + FLATTENED_ATTR_VAL_ALIAS;
    } else {
      selectSql =
          "SELECT "
              + String.join(", ", insertColumns)
              + " FROM "
              + entityTable.getTablePointer().render();
    }

    String insertFromSelectSql =
        "INSERT INTO "
            + searchTable.getTablePointer().render()
            + " ("
            + String.join(", ", insertColumns)
            + ") "
            + selectSql
            + " WHERE "
            + searchAttribute
            + " IS NOT NULL";
    LOGGER.info("Generated insert SQL: {}", insertFromSelectSql);

    runQueryIfTableExists(searchTable.getTablePointer(), insertFromSelectSql, isDryRun);
  }
}

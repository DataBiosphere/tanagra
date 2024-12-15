package bio.terra.tanagra.indexing.job.bigquery;

import bio.terra.tanagra.indexing.job.BigQueryJob;
import bio.terra.tanagra.indexing.job.dataflow.beam.BigQueryBeamUtils;
import bio.terra.tanagra.underlay.entitymodel.Entity;
import bio.terra.tanagra.underlay.indextable.ITEntityMain;
import bio.terra.tanagra.underlay.indextable.ITEntitySearchByAttribute;
import bio.terra.tanagra.underlay.serialization.SZIndexer;
import com.google.cloud.bigquery.Clustering;
import com.google.cloud.bigquery.Field;
import com.google.cloud.bigquery.Schema;
import com.google.cloud.bigquery.TableId;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WriteEntitySearchByAttribute extends BigQueryJob {
  private static final Logger LOGGER = LoggerFactory.getLogger(WriteEntitySearchByAttribute.class);

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
            .toList();

    // Build a clustering specification.
    Clustering clustering =
        Clustering.newBuilder().setFields(searchTable.getAttributeNames()).build();

    // Create an empty table with this schema.
    TableId destinationTable =
        TableId.of(
            indexerConfig.bigQuery.indexData.projectId,
            indexerConfig.bigQuery.indexData.datasetId,
            getOutputTableName());
    googleBigQuery.createTableFromSchema(destinationTable, Schema.of(fields), clustering, isDryRun);

    // Build the query to insert to the search table using a select from the main entity table.
    List<String> insertColumns = new ArrayList<>();
    insertColumns.add(entity.getIdAttribute().getName());

    List<String> selectColumns = new ArrayList<>();
    selectColumns.add(entity.getIdAttribute().getName());

    List<String> crossJoins = new ArrayList<>();
    searchTable
        .getAttributeNames()
        .forEach(
            attribute -> {
              insertColumns.add(attribute);

              if (entity.getAttribute(attribute).isDataTypeRepeated()) {
                String alias = "flattened_" + attribute;
                selectColumns.add(alias);
                crossJoins.add(" CROSS JOIN UNNEST(" + attribute + ") AS " + alias);
              } else {
                selectColumns.add(attribute);
              }
            });

    // TODO-dex: check is select distinct is feasible
    String insertFromSelectSql =
        "INSERT INTO "
            + searchTable.getTablePointer().render()
            + " ("
            + String.join(", ", insertColumns)
            + ") SELECT "
            + String.join(", ", selectColumns)
            + " FROM "
            + entityTable.getTablePointer().render()
            + String.join(" ", crossJoins);
    LOGGER.info("Generated insert SQL: {}", insertFromSelectSql);

    runQueryIfTableExists(searchTable.getTablePointer(), insertFromSelectSql, isDryRun);
  }
}

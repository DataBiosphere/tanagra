package bio.terra.tanagra.indexing.job;

import bio.terra.tanagra.api.shared.UnaryOperator;
import bio.terra.tanagra.query.bigquery.BQTable;
import bio.terra.tanagra.query.bigquery.translator.BQApiTranslator;
import bio.terra.tanagra.query.sql.SqlField;
import bio.terra.tanagra.query.sql.SqlParams;
import bio.terra.tanagra.query.sql.SqlQueryField;
import bio.terra.tanagra.underlay.serialization.SZIndexer;
import bio.terra.tanagra.utils.GoogleBigQuery;
import com.google.cloud.bigquery.Table;
import com.google.cloud.bigquery.TableResult;
import java.math.BigInteger;
import java.util.Optional;

public abstract class BigQueryJob implements IndexingJob {
  protected final SZIndexer indexerConfig;
  protected final GoogleBigQuery googleBigQuery;

  protected BigQueryJob(SZIndexer indexerConfig) {
    this.indexerConfig = indexerConfig;
    this.googleBigQuery =
        GoogleBigQuery.forApplicationDefaultCredentials(indexerConfig.bigQuery.queryProjectId);
  }

  @Override
  public String getName() {
    return String.format("%s-%s", this.getClass().getSimpleName(), getOutputTableName());
  }

  @Override
  public void clean(boolean isDryRun) {
    Optional<Table> outputTable = getOutputTable();
    if (outputTable.isPresent()) {
      LOGGER.info("Deleting output table: {}", outputTable.get().getFriendlyName());
      if (!isDryRun) {
        googleBigQuery.deleteTable(
            indexerConfig.bigQuery.indexData.projectId,
            indexerConfig.bigQuery.indexData.datasetId,
            getOutputTableName());
      }
    } else {
      LOGGER.info("Output table not found. Nothing to delete.");
    }
  }

  protected abstract String getOutputTableName();

  protected Optional<Table> getOutputTable() {
    return googleBigQuery.getTable(
        indexerConfig.bigQuery.indexData.projectId,
        indexerConfig.bigQuery.indexData.datasetId,
        getOutputTableName());
  }

  protected boolean outputTableHasAtLeastOneRow() {
    Optional<Table> outputTable = getOutputTable();
    return outputTable.isPresent()
        && outputTable.get().getNumRows().compareTo(BigInteger.ZERO) == 1;
  }

  protected boolean outputTableHasAtLeastOneRowWithNotNullField(BQTable sqlTable, SqlField field) {
    // Check if the table has at least 1 row with a non-null field value.
    BQApiTranslator bqTranslator = new BQApiTranslator();
    String selectOneRowSql =
        "SELECT "
            + SqlQueryField.of(field).renderForSelect()
            + " FROM "
            + sqlTable.render()
            + " WHERE "
            + bqTranslator.unaryFilterSql(field, UnaryOperator.IS_NOT_NULL, null, new SqlParams())
            + " LIMIT 1";
    TableResult tableResult = googleBigQuery.runQuery(selectOneRowSql);
    return tableResult.getTotalRows() > 0;
  }

  protected void runQueryIfTableExists(BQTable bqTable, String sql, boolean isDryRun) {
    Optional<Table> table =
        googleBigQuery.getTable(
            indexerConfig.bigQuery.indexData.projectId,
            indexerConfig.bigQuery.indexData.datasetId,
            bqTable.getTableName());
    if (table.isEmpty()) {
      LOGGER.info("Output table has not been created yet, so skipping query");
    } else if (isDryRun) {
      googleBigQuery.dryRunQuery(sql);
    } else {
      googleBigQuery.runQuery(sql);
    }
  }
}

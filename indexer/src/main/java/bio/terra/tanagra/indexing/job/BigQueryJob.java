package bio.terra.tanagra.indexing.job;

import bio.terra.tanagra.query.FieldPointer;
import bio.terra.tanagra.query.FunctionTemplate;
import bio.terra.tanagra.query2.bigquery.BQTranslator;
import bio.terra.tanagra.query2.sql.SqlField;
import bio.terra.tanagra.query2.sql.SqlParams;
import bio.terra.tanagra.underlay.serialization.SZIndexer;
import bio.terra.tanagra.utils.GoogleBigQuery;
import com.google.cloud.bigquery.Table;
import com.google.cloud.bigquery.TableResult;
import java.math.BigInteger;
import java.util.List;
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

  protected boolean outputTableHasAtLeastOneRowWithNotNullField(FieldPointer field) {
    // Check if the table has at least 1 row with a non-null field value.
    BQTranslator bqTranslator = new BQTranslator();
    String selectOneRowSql =
        "SELECT "
            + bqTranslator.selectSql(SqlField.of(field, null), null)
            + " FROM "
            + field.getTablePointer().renderSQL()
            + " WHERE "
            + bqTranslator.functionFilterSql(
                field,
                bqTranslator.functionTemplateSql(FunctionTemplate.IS_NOT_NULL),
                List.of(),
                null,
                new SqlParams())
            + " LIMIT 1";
    TableResult tableResult = googleBigQuery.queryBigQuery(selectOneRowSql);
    return tableResult.getTotalRows() > 0;
  }
}

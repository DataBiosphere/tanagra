package bio.terra.tanagra.indexing.job;

import bio.terra.tanagra.query.ColumnHeaderSchema;
import bio.terra.tanagra.query.ColumnSchema;
import bio.terra.tanagra.query.FieldPointer;
import bio.terra.tanagra.query.FieldVariable;
import bio.terra.tanagra.query.FilterVariable;
import bio.terra.tanagra.query.Literal;
import bio.terra.tanagra.query.Query;
import bio.terra.tanagra.query.QueryRequest;
import bio.terra.tanagra.query.QueryResult;
import bio.terra.tanagra.query.TableVariable;
import bio.terra.tanagra.query.bigquery.BigQueryExecutor;
import bio.terra.tanagra.query.filtervariable.BinaryFilterVariable;
import bio.terra.tanagra.underlay2.serialization.SZIndexer;
import com.google.cloud.bigquery.Table;
import java.math.BigInteger;
import java.util.List;
import java.util.Optional;

public abstract class BigQueryJob implements IndexingJob {
  protected final SZIndexer indexerConfig;
  protected final BigQueryExecutor bigQueryExecutor;

  protected BigQueryJob(SZIndexer indexerConfig) {
    this.indexerConfig = indexerConfig;
    this.bigQueryExecutor =
        new BigQueryExecutor(
            indexerConfig.bigQuery.queryProjectId, indexerConfig.bigQuery.dataLocation);
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
        bigQueryExecutor
            .getBigQueryService()
            .deleteTable(
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
    return bigQueryExecutor
        .getBigQueryService()
        .getTable(
            indexerConfig.bigQuery.indexData.projectId,
            indexerConfig.bigQuery.indexData.datasetId,
            getOutputTableName());
  }

  protected boolean outputTableHasAtLeastOneRow() {
    Optional<Table> outputTable = getOutputTable();
    return outputTable.isPresent()
        && outputTable.get().getNumRows().compareTo(BigInteger.ZERO) == 1;
  }

  protected boolean outputTableHasAtLeastOneRowWithNotNullField(
      FieldPointer field, ColumnSchema columnSchema) {
    // Check if the table has at least 1 row with a non-null field value.
    TableVariable outputTableVar = TableVariable.forPrimary(field.getTablePointer());
    List<TableVariable> tableVars = List.of(outputTableVar);

    FieldVariable fieldVar = field.buildVariable(outputTableVar, tableVars);
    FilterVariable fieldNotNull =
        new BinaryFilterVariable(
            fieldVar, BinaryFilterVariable.BinaryOperator.IS_NOT, new Literal((String) null));
    Query query =
        new Query.Builder()
            .select(List.of(fieldVar))
            .tables(tableVars)
            .where(fieldNotNull)
            .limit(1)
            .build();

    ColumnHeaderSchema columnHeaderSchema = new ColumnHeaderSchema(List.of(columnSchema));
    QueryRequest queryRequest = new QueryRequest(query.renderSQL(), columnHeaderSchema);
    QueryResult queryResult = bigQueryExecutor.execute(queryRequest);
    return queryResult.getRowResults().iterator().hasNext();
  }
}

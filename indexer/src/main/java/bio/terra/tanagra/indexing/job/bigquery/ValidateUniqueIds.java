package bio.terra.tanagra.indexing.job.bigquery;

import bio.terra.tanagra.exception.InvalidConfigException;
import bio.terra.tanagra.indexing.job.BigQueryJob;
import bio.terra.tanagra.query.sql.SqlField;
import bio.terra.tanagra.query.sql.SqlQueryField;
import bio.terra.tanagra.underlay.entitymodel.Entity;
import bio.terra.tanagra.underlay.indextable.ITEntityMain;
import bio.terra.tanagra.underlay.serialization.SZIndexer;
import bio.terra.tanagra.underlay.sourcetable.STEntityAttributes;
import com.google.cloud.bigquery.TableResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ValidateUniqueIds extends BigQueryJob {
  private static final Logger LOGGER = LoggerFactory.getLogger(ValidateDataTypes.class);

  private final Entity entity;
  private final STEntityAttributes sourceTable;
  private final ITEntityMain indexTable;

  public ValidateUniqueIds(
      SZIndexer indexerConfig,
      Entity entity,
      STEntityAttributes sourceTable,
      ITEntityMain indexTable) {
    super(indexerConfig);
    this.entity = entity;
    this.sourceTable = sourceTable;
    this.indexTable = indexTable;
  }

  @Override
  public String getEntity() {
    return entity.getName();
  }

  @Override
  protected String getOutputTableName() {
    return indexTable.getTablePointer().getTableName();
  }

  @Override
  public JobStatus checkStatus() {
    return JobStatus.NOT_STARTED;
  }

  @Override
  public void run(boolean isDryRun) {
    // Build the query to select all duplicate ids from the source table.
    String sourceIdFieldName =
        sourceTable.getAttributeValueColumnSchema(entity.getIdAttribute()).getColumnName();
    SqlQueryField sourceIdField = SqlQueryField.of(SqlField.of(sourceIdFieldName));
    String selectDuplicateIdsSql =
        "SELECT "
            + sourceIdField.renderForSelect()
            + " FROM "
            + sourceTable.getTablePointer().render()
            + " GROUP BY "
            + sourceIdField.renderForGroupBy(null, true)
            + " HAVING COUNT(*) > 1";
    LOGGER.info("Generated select SQL: {}", selectDuplicateIdsSql);

    // Run the query and check that no rows were returned.
    TableResult tableResult = googleBigQuery.queryBigQuery(selectDuplicateIdsSql);
    if (tableResult.getTotalRows() > 0) {
      throw new InvalidConfigException(
          "Id attribute is not unique for entity: "
              + entity.getName()
              + ". "
              + tableResult.getTotalRows()
              + " duplicates found.");
    }
  }

  @Override
  public void clean(boolean isDryRun) {
    LOGGER.info(
        "Nothing to clean. This job only validates the entity id attribute. It does not write any index data.");
  }

  @Override
  public boolean checkStatusAfterRunMatchesExpected(
      RunType runType, boolean isDryRun, JobStatus status) {
    return JobStatus.NOT_STARTED.equals(status);
  }
}

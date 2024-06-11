package bio.terra.tanagra.indexing.job.bigquery;

import bio.terra.tanagra.exception.InvalidConfigException;
import bio.terra.tanagra.indexing.job.BigQueryJob;
import bio.terra.tanagra.query.sql.SqlField;
import bio.terra.tanagra.query.sql.SqlQueryField;
import bio.terra.tanagra.underlay.entitymodel.Entity;
import bio.terra.tanagra.underlay.indextable.ITEntityMain;
import bio.terra.tanagra.underlay.serialization.SZIndexer;
import com.google.cloud.bigquery.TableResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ValidateUniqueIds extends BigQueryJob {
  private static final Logger LOGGER = LoggerFactory.getLogger(ValidateDataTypes.class);

  private final Entity entity;
  private final ITEntityMain indexTable;

  public ValidateUniqueIds(SZIndexer indexerConfig, Entity entity, ITEntityMain indexTable) {
    super(indexerConfig);
    this.entity = entity;
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
    // Build the query to select all duplicate ids from the index table.
    String indexIdFieldName =
        indexTable.getAttributeValueColumnSchema(entity.getIdAttribute()).getColumnName();
    SqlQueryField indexIdField = SqlQueryField.of(SqlField.of(indexIdFieldName));
    String selectDuplicateIdsSql =
        "SELECT "
            + indexIdField.renderForSelect()
            + " FROM "
            + indexTable.getTablePointer().render()
            + " GROUP BY "
            + indexIdField.renderForGroupBy(null, true)
            + " HAVING COUNT(*) > 1";
    LOGGER.info("Generated select SQL: {}", selectDuplicateIdsSql);

    if (getOutputTable().isEmpty()) {
      LOGGER.info("Skipping unique id check because entity index table does not exist yet.");
    } else {
      // Run the query and check that no rows were returned.
      TableResult tableResult =
          googleBigQuery.runQuery(selectDuplicateIdsSql, null, null, null, null, null);
      if (tableResult.getTotalRows() > 0) {
        throw new InvalidConfigException(
            "Id attribute is not unique for entity: "
                + entity.getName()
                + ". "
                + tableResult.getTotalRows()
                + " duplicates found.");
      }
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

package bio.terra.tanagra.indexing.job.bigquery;

import bio.terra.tanagra.api.shared.BinaryOperator;
import bio.terra.tanagra.indexing.job.BigQueryJob;
import bio.terra.tanagra.query.bigquery.translator.BQApiTranslator;
import bio.terra.tanagra.query.sql.SqlField;
import bio.terra.tanagra.query.sql.SqlQueryField;
import bio.terra.tanagra.underlay.entitymodel.Hierarchy;
import bio.terra.tanagra.underlay.entitymodel.entitygroup.EntityGroup;
import bio.terra.tanagra.underlay.indextable.ITEntityMain;
import bio.terra.tanagra.underlay.serialization.SZIndexer;
import com.google.cloud.bigquery.Table;
import com.google.cloud.bigquery.TableResult;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CleanHierarchyNodesWithZeroCounts extends BigQueryJob {

  private static final Logger LOGGER = LoggerFactory.getLogger(CleanHierarchyNodesWithZeroCounts.class);

  private final EntityGroup entityGroup;
  private final ITEntityMain indexTable;
  private final Hierarchy hierarchy;

  public CleanHierarchyNodesWithZeroCounts(
      SZIndexer indexerConfig,
      EntityGroup entityGroup,
      ITEntityMain indexTable,
      Hierarchy hierarchy) {
    super(indexerConfig);
    this.entityGroup = entityGroup;
    this.indexTable = indexTable;
    this.hierarchy = hierarchy;
  }

  @Override
  public String getEntityGroup() {
    return entityGroup.getName();
  }

  @Override
  protected String getOutputTableName() {
    return indexTable.getTablePointer().getTableName();
  }

  @Override
  protected Optional<Table> getOutputTable() {
    return googleBigQuery.getTable(
        indexerConfig.bigQuery.indexData.projectId,
        indexerConfig.bigQuery.indexData.datasetId,
        getOutputTableName());
  }

  @Override
  public JobStatus checkStatus() {
    return getOutputTable().isPresent() && outputTableHasNoNodesWithZeroCounts()
        ? JobStatus.COMPLETE
        : JobStatus.NOT_STARTED;
  }

  @Override
  public void run(boolean isDryRun) {
    cleanHierarchyNodesWithZeroCounts(isDryRun);
  }

  private void cleanHierarchyNodesWithZeroCounts(boolean isDryRun) {
    /* Build a delete-from query for the index entity main table that have zero counts
    for both hierarchy and non hierarchy fields */
    String cleanHierarchyNodesWithZeroCounts = "DELETE FROM " + generateSqlBody();
    LOGGER.info("delete-from query: {}", cleanHierarchyNodesWithZeroCounts);

    // Run the update-from-select to write the count field in the index entity main table.
    googleBigQuery.runInsertUpdateQuery(cleanHierarchyNodesWithZeroCounts, isDryRun);
  }

  private boolean outputTableHasNoNodesWithZeroCounts() {
    // Check if the table has rows with zero counts for both hierarchy and non hierarchy fields
    String selectCountSql = "SELECT COUNT(*) AS count FROM " + generateSqlBody();
    TableResult tableResult = googleBigQuery.queryBigQuery(selectCountSql);
    return tableResult.iterateAll().iterator().next().get("count").getLongValue() == 0;
  }

  private String generateSqlBody() {
    // Get the hierarchy and non hierarchy count fields
    SqlField entityTableCountField =
        indexTable.getEntityGroupCountField(entityGroup.getName(), hierarchy.getName());
    SqlField entityTableNoHierCountField =
        indexTable.getEntityGroupCountField(entityGroup.getName(), null);

    BQApiTranslator bqTranslator = new BQApiTranslator();
    return indexTable.getTablePointer().render()
        + " WHERE "
        + SqlQueryField.of(entityTableCountField).renderForSelect()
        + bqTranslator.binaryOperatorSql(BinaryOperator.EQUALS)
        + "0"
        + " AND "
        + SqlQueryField.of(entityTableNoHierCountField).renderForSelect()
        + bqTranslator.binaryOperatorSql(BinaryOperator.EQUALS)
        + "0";
  }
}

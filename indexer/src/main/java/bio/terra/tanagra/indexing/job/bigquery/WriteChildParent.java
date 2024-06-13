package bio.terra.tanagra.indexing.job.bigquery;

import static bio.terra.tanagra.utils.GoogleBigQuery.LONG_QUERY_TIMEOUT;

import bio.terra.tanagra.indexing.job.BigQueryJob;
import bio.terra.tanagra.query.sql.SqlQueryField;
import bio.terra.tanagra.underlay.indextable.ITHierarchyChildParent;
import bio.terra.tanagra.underlay.serialization.SZIndexer;
import bio.terra.tanagra.underlay.sourcetable.STHierarchyChildParent;
import com.google.cloud.bigquery.Clustering;
import com.google.cloud.bigquery.TableId;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WriteChildParent extends BigQueryJob {
  private static final Logger LOGGER = LoggerFactory.getLogger(WriteChildParent.class);

  private final STHierarchyChildParent sourceTable;
  private final ITHierarchyChildParent indexTable;

  public WriteChildParent(
      SZIndexer indexerConfig,
      STHierarchyChildParent sourceTable,
      ITHierarchyChildParent indexTable) {
    super(indexerConfig);
    this.sourceTable = sourceTable;
    this.indexTable = indexTable;
  }

  @Override
  public String getEntity() {
    return sourceTable.getEntity();
  }

  @Override
  protected String getOutputTableName() {
    return indexTable.getTablePointer().getTableName();
  }

  @Override
  public JobStatus checkStatus() {
    return getOutputTable().isPresent() ? JobStatus.COMPLETE : JobStatus.NOT_STARTED;
  }

  @Override
  public void run(boolean isDryRun) {
    String sourceChildParentSql =
        "SELECT "
            + SqlQueryField.of(
                    sourceTable.getChildField(),
                    ITHierarchyChildParent.Column.CHILD.getSchema().getColumnName())
                .renderForSelect()
            + ", "
            + SqlQueryField.of(
                    sourceTable.getParentField(),
                    ITHierarchyChildParent.Column.PARENT.getSchema().getColumnName())
                .renderForSelect()
            + " FROM "
            + sourceTable.getTablePointer().render();
    LOGGER.info("source child-parent query: {}", sourceChildParentSql);

    // Create a new table directly from the select query.
    TableId outputTable =
        TableId.of(
            indexerConfig.bigQuery.indexData.projectId,
            indexerConfig.bigQuery.indexData.datasetId,
            indexTable.getTablePointer().getTableName());
    Clustering clustering =
        Clustering.newBuilder()
            .setFields(List.of(sourceTable.getParentField().getColumnName()))
            .build();

    if (isDryRun) {
      googleBigQuery.dryRunQuery(sourceChildParentSql, null, null, null, outputTable, clustering);
    } else {
      googleBigQuery.runQuery(
          sourceChildParentSql, null, null, null, outputTable, clustering, LONG_QUERY_TIMEOUT);
    }
  }
}

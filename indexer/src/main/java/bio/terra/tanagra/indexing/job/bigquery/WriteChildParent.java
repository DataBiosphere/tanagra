package bio.terra.tanagra.indexing.job.bigquery;

import bio.terra.tanagra.indexing.job.BigQueryJob;
import bio.terra.tanagra.query.Query;
import bio.terra.tanagra.underlay.indextable.ITHierarchyChildParent;
import bio.terra.tanagra.underlay.serialization.SZIndexer;
import bio.terra.tanagra.underlay.sourcetable.STHierarchyChildParent;
import com.google.cloud.bigquery.Clustering;
import com.google.cloud.bigquery.TableId;
import java.util.List;
import java.util.Map;
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
    Query sourceChildParentQuery =
        sourceTable.getQueryAll(
            Map.of(
                sourceTable.getChildColumnSchema(),
                    ITHierarchyChildParent.Column.CHILD.getSchema().getColumnName(),
                sourceTable.getParentColumnSchema(),
                    ITHierarchyChildParent.Column.PARENT.getSchema().getColumnName()));
    LOGGER.info("source child-parent query: {}", sourceChildParentQuery.renderSQL());

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
    bigQueryExecutor
        .getBigQueryService()
        .createTableFromQuery(
            outputTable, sourceChildParentQuery.renderSQL(), clustering, isDryRun);
  }
}

package bio.terra.tanagra.query.bigquery;

import com.google.common.collect.ImmutableList;
import jakarta.annotation.Nullable;
import java.util.List;

public class BQExecutorInfrastructure {
  private final String queryProjectId;
  private final @Nullable String datasetLocation;
  private final @Nullable ImmutableList<String> exportDatasetIds;
  private final @Nullable ImmutableList<String> exportBucketNames;

  private BQExecutorInfrastructure(
      String queryProjectId,
      @Nullable String datasetLocation,
      List<String> exportDatasetIds,
      List<String> exportBucketNames) {
    this.queryProjectId = queryProjectId;
    this.datasetLocation = datasetLocation;
    this.exportDatasetIds =
        exportDatasetIds == null ? null : ImmutableList.copyOf(exportDatasetIds);
    this.exportBucketNames =
        exportBucketNames == null ? null : ImmutableList.copyOf(exportBucketNames);
  }

  public static BQExecutorInfrastructure forQuery(String queryProjectId) {
    return new BQExecutorInfrastructure(queryProjectId, null, null, null);
  }

  public static BQExecutorInfrastructure forQueryAndExport(
      String queryProjectId,
      String datasetLocation,
      List<String> exportDatasetIds,
      List<String> exportBucketNames) {
    return new BQExecutorInfrastructure(
        queryProjectId, datasetLocation, exportDatasetIds, exportBucketNames);
  }

  public String getQueryProjectId() {
    return queryProjectId;
  }

  @Nullable
  public String getDatasetLocation() {
    return datasetLocation;
  }

  @Nullable
  public ImmutableList<String> getExportDatasetIds() {
    return exportDatasetIds;
  }

  @Nullable
  public ImmutableList<String> getExportBucketNames() {
    return exportBucketNames;
  }
}

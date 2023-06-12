package bio.terra.tanagra.query;

import java.util.List;

public interface QueryExecutor {
  /** Execute a query request, returning the results of the query. */
  QueryResult execute(QueryRequest queryRequest);

  /** @return GCS full path (e.g. gs://bucket/filename.csv) */
  String executeAndExportResultsToGcs(
      QueryRequest queryRequest, String fileName, String gcsProjectId, List<String> gcsBucketNames);
}

package bio.terra.tanagra.query.bigquery;

import static com.google.cloud.storage.Storage.BucketField.LOCATION;

import bio.terra.tanagra.exception.SystemException;
import bio.terra.tanagra.query.PageMarker;
import bio.terra.tanagra.query.QueryExecutor;
import bio.terra.tanagra.query.QueryRequest;
import bio.terra.tanagra.query.QueryResult;
import bio.terra.tanagra.query.RowResult;
import bio.terra.tanagra.utils.GoogleBigQuery;
import bio.terra.tanagra.utils.GoogleCloudStorage;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.TableResult;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.Storage;
import com.google.common.collect.Iterables;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** A {@link QueryExecutor} for Google BigQuery. */
public class BigQueryExecutor implements QueryExecutor {
  private static final Logger LOGGER = LoggerFactory.getLogger(BigQueryExecutor.class);

  private final String queryProjectId;
  private final String datasetLocation;
  private GoogleBigQuery bigQueryService;

  public BigQueryExecutor(String queryProjectId, String datasetLocation) {
    this.queryProjectId = queryProjectId;
    this.datasetLocation = datasetLocation;
  }

  @Override
  public QueryResult execute(QueryRequest queryRequest) {
    String sql = queryRequest.getSql();
    LOGGER.info("Running SQL against BigQuery: {}", sql);
    TableResult tableResult =
        getBigQueryService()
            .queryBigQuery(
                sql,
                queryRequest.getPageMarker() == null
                    ? null
                    : queryRequest.getPageMarker().getPageToken(),
                queryRequest.getPageSize());

    LOGGER.info("SQL query returns {} rows across all pages", tableResult.getTotalRows());
    Iterable<RowResult> rowResults =
        Iterables.transform(
            tableResult.getValues() /* Single page of results. */,
            (FieldValueList fieldValueList) ->
                new BigQueryRowResult(fieldValueList, queryRequest.getColumnHeaderSchema()));
    PageMarker nextPageMarker =
        tableResult.hasNextPage() ? PageMarker.forToken(tableResult.getNextPageToken()) : null;
    return new QueryResult(rowResults, queryRequest.getColumnHeaderSchema(), nextPageMarker);
  }

  @Override
  public String executeAndExportResultsToGcs(
      QueryRequest queryRequest,
      String fileName,
      String gcsProjectId,
      List<String> gcsBucketNames) {
    String bucketName = findCompatibleBucket(gcsProjectId, gcsBucketNames);

    // Validate the filename.
    // GCS file name must be in wildcard format.
    // https://cloud.google.com/bigquery/docs/reference/standard-sql/other-statements#export_option_list:~:text=The%20uri%20option%20must%20be%20a%20single%2Dwildcard%20URI
    String validatedFilename;
    if (fileName == null || !fileName.contains("*")) {
      validatedFilename = "tanagra_" + System.currentTimeMillis() + "_*.csv";
      LOGGER.warn(
          "No (or invalid) filename specified for GCS export ({}), using default: {}",
          fileName,
          validatedFilename);
    } else {
      validatedFilename = fileName;
    }

    // Prefix the SQL query with the export to GCS directive.
    String sql =
        String.format(
            "EXPORT DATA OPTIONS(uri='gs://%s/%s',format='CSV',overwrite=true,header=true) AS %n%s",
            bucketName, validatedFilename, queryRequest.getSql());
    LOGGER.info("Running SQL against BigQuery: {}", sql);
    getBigQueryService().queryBigQuery(sql);

    // Multiple files will be created only if export is very large (> 1GB). For now, just assume
    // only "000000000000" was created.
    // TODO: Detect and handle case where mulitple files are created.
    return String.format("gs://%s/%s", bucketName, validatedFilename.replace("*", "000000000000"));
  }

  @SuppressWarnings("PMD.AvoidLiteralsInIfCondition")
  private String findCompatibleBucket(String gcsProjectId, List<String> gcsBucketNames) {
    // Lookup the GCS bucket location. Return the first bucket with a compatible location for the
    // dataset.
    // https://cloud.google.com/bigquery/docs/exporting-data#data-locations
    GoogleCloudStorage storageService =
        GoogleCloudStorage.forApplicationDefaultCredentials(gcsProjectId);
    for (String bucketName : gcsBucketNames) {
      Optional<Bucket> bucket =
          storageService.getBucket(bucketName, Storage.BucketGetOption.fields(LOCATION));
      if (bucket.isEmpty()) {
        LOGGER.warn("Bucket not found: {}", bucketName);
        continue;
      }
      String bucketLocation = bucket.get().getLocation();
      if (datasetLocation.equals(bucketLocation)
          || "US".equalsIgnoreCase(datasetLocation)
          || (datasetLocation.startsWith("us") && "US".equalsIgnoreCase(bucketLocation))
          || ("EU".equalsIgnoreCase(datasetLocation) && bucketLocation.startsWith("europe"))) {
        return bucketName;
      }
    }
    throw new SystemException(
        "No compatible GCS bucket found for export from BQ dataset in location: "
            + datasetLocation);
  }

  public GoogleBigQuery getBigQueryService() {
    if (bigQueryService == null) {
      GoogleCredentials credentials;
      try {
        credentials = GoogleCredentials.getApplicationDefault();
      } catch (IOException ioEx) {
        throw new SystemException("Error loading application default credentials", ioEx);
      }
      bigQueryService = new GoogleBigQuery(credentials, queryProjectId);
    }
    return bigQueryService;
  }
}

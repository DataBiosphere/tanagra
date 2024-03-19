package bio.terra.tanagra.service.export;

import bio.terra.tanagra.api.field.AttributeField;
import bio.terra.tanagra.api.field.ValueDisplayField;
import bio.terra.tanagra.api.filter.EntityFilter;
import bio.terra.tanagra.api.query.export.ExportQueryRequest;
import bio.terra.tanagra.api.query.export.ExportQueryResult;
import bio.terra.tanagra.api.query.list.ListQueryRequest;
import bio.terra.tanagra.api.query.list.ListQueryResult;
import bio.terra.tanagra.app.configuration.ExportConfiguration;
import bio.terra.tanagra.exception.SystemException;
import bio.terra.tanagra.filterbuilder.EntityOutput;
import bio.terra.tanagra.service.artifact.ReviewService;
import bio.terra.tanagra.underlay.entitymodel.Attribute;
import bio.terra.tanagra.underlay.entitymodel.Entity;
import bio.terra.tanagra.utils.GoogleCloudStorage;
import bio.terra.tanagra.utils.NameUtils;
import bio.terra.tanagra.utils.RandomNumberGenerator;
import bio.terra.tanagra.utils.threadpool.Job;
import bio.terra.tanagra.utils.threadpool.JobResult;
import bio.terra.tanagra.utils.threadpool.ThreadPoolUtils;
import com.google.cloud.storage.BlobId;
import com.google.common.collect.ImmutableList;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.text.StringSubstitutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataExportHelper {
  private static final Logger LOGGER = LoggerFactory.getLogger(DataExportHelper.class);
  private final Integer maxChildThreads;
  private final ExportConfiguration.Shared sharedExportConfig;
  private final RandomNumberGenerator randomNumberGenerator;
  private final ReviewService reviewService;
  private final ExportRequest exportRequest;
  private final ImmutableList<EntityOutput> entityOutputs;
  private final EntityFilter primaryEntityFilter;
  private GoogleCloudStorage googleCloudStorage;

  public DataExportHelper(
      Integer maxChildThreads,
      ExportConfiguration.Shared sharedExportConfig,
      RandomNumberGenerator randomNumberGenerator,
      ReviewService reviewService,
      ExportRequest exportRequest,
      List<EntityOutput> entityOutputs,
      EntityFilter primaryEntityFilter) {
    this.maxChildThreads = maxChildThreads;
    this.sharedExportConfig = sharedExportConfig;
    this.randomNumberGenerator = randomNumberGenerator;
    this.reviewService = reviewService;
    this.exportRequest = exportRequest;
    this.entityOutputs = ImmutableList.copyOf(entityOutputs);
    this.primaryEntityFilter = primaryEntityFilter;
  }

  /**
   * @param isAgainstSourceDataset True to generate SQL queries against the source dataset.
   * @return Map of entity -> SQL query with all parameters substituted.
   */
  public Map<Entity, String> generateSqlPerExportEntity(
      List<String> entityNames, boolean isAgainstSourceDataset) {
    Map<Entity, String> sqlPerEntity = new HashMap<>();
    entityOutputs.stream()
        .filter(
            entityOutput ->
                entityNames.isEmpty() || entityNames.contains(entityOutput.getEntity().getName()))
        .forEach(
            entityOutput -> {
              List<ValueDisplayField> selectFields =
                  entityOutput.getAttributes().stream()
                      .map(
                          attribute ->
                              new AttributeField(
                                  exportRequest.getUnderlay(),
                                  entityOutput.getEntity(),
                                  attribute,
                                  false,
                                  isAgainstSourceDataset))
                      .collect(Collectors.toList());

              ListQueryRequest listQueryRequest =
                  new ListQueryRequest(
                      exportRequest.getUnderlay(),
                      entityOutput.getEntity(),
                      selectFields,
                      entityOutput.getDataFeatureFilter(),
                      null,
                      null,
                      null,
                      null,
                      true);
              ListQueryResult listQueryResult =
                  exportRequest
                      .getUnderlay()
                      .getQueryRunner()
                      .run(listQueryRequest.cloneAndSetDryRun());
              sqlPerEntity.put(entityOutput.getEntity(), listQueryResult.getSqlNoParams());
            });
    return sqlPerEntity;
  }

  /**
   * @param attributeNames List of attribute names to include in the generated SQL query. An empty
   *     list means to include all attributes.
   * @param isAgainstSourceDataset True to generate SQL queries against the source dataset.
   * @return SQL query with all parameters substituted.
   */
  public String generateSqlForPrimaryEntity(
      List<String> attributeNames, boolean isAgainstSourceDataset) {
    List<ValueDisplayField> selectedAttributeFields = new ArrayList<>();
    exportRequest.getUnderlay().getPrimaryEntity().getAttributes().stream()
        .filter(
            attribute -> attributeNames.isEmpty() || attributeNames.contains(attribute.getName()))
        .forEach(
            attribute ->
                selectedAttributeFields.add(
                    new AttributeField(
                        exportRequest.getUnderlay(),
                        exportRequest.getUnderlay().getPrimaryEntity(),
                        attribute,
                        false,
                        isAgainstSourceDataset)));
    ListQueryRequest listQueryRequest =
        new ListQueryRequest(
            exportRequest.getUnderlay(),
            exportRequest.getUnderlay().getPrimaryEntity(),
            selectedAttributeFields,
            primaryEntityFilter,
            null,
            null,
            null,
            null,
            true);
    ListQueryResult listQueryResult =
        exportRequest.getUnderlay().getQueryRunner().run(listQueryRequest);
    return listQueryResult.getSqlNoParams();
  }

  /**
   * @param fileNameTemplate String substitution template for the filename. Must include ${entity}
   *     and ${random} placeholders (e.g. ${entity}_cohort_${random}).
   * @return List of entity file outputs, including the full GCS path (e.g.
   *     gs://bucket/filename.csv.gzip).
   */
  public List<ExportFileResult> writeEntityDataToGcs(String fileNameTemplate) {
    // Build set of export query requests.
    List<ExportQueryRequest> exportQueryRequests =
        entityOutputs.stream()
            .map(
                entityOutput -> {
                  // Build the list query request.
                  List<ValueDisplayField> selectFields =
                      entityOutput.getAttributes().stream()
                          .sorted(Comparator.comparing(Attribute::getName))
                          .map(
                              attribute ->
                                  new AttributeField(
                                      exportRequest.getUnderlay(),
                                      entityOutput.getEntity(),
                                      attribute,
                                      false,
                                      false))
                          .collect(Collectors.toList());
                  ListQueryRequest listQueryRequest =
                      new ListQueryRequest(
                          exportRequest.getUnderlay(),
                          entityOutput.getEntity(),
                          selectFields,
                          entityOutput.getDataFeatureFilter(),
                          null,
                          null,
                          null,
                          null,
                          false);

                  // Build a map of substitution strings for the filename template.
                  Map<String, String> substitutions =
                      Map.of(
                          "entity",
                          entityOutput.getEntity().getName(),
                          "random",
                          Instant.now().getEpochSecond() + "_" + randomNumberGenerator.getNext());
                  String substitutedFilename =
                      StringSubstitutor.replace(fileNameTemplate, substitutions);
                  return new ExportQueryRequest(
                      listQueryRequest,
                      entityOutput.getEntity().getName(),
                      substitutedFilename,
                      sharedExportConfig.getGcpProjectId(),
                      sharedExportConfig.getBqDatasetIds(),
                      sharedExportConfig.getGcsBucketNames(),
                      true);
                })
            .collect(Collectors.toList());

    List<ExportFileResult> exportFileResults = new ArrayList<>();
    if (maxChildThreads == null || maxChildThreads > 1) {
      // Build set of export jobs.
      Set<Job<ExportQueryRequest, ExportQueryResult>> exportJobs = new HashSet<>();
      exportQueryRequests.stream()
          .forEach(
              exportQueryRequest ->
                  exportJobs.add(
                      new Job<>(
                          exportQueryRequest.getListQueryRequest().getEntity().getName()
                              + '_'
                              + Instant.now().toEpochMilli(),
                          exportQueryRequest,
                          () ->
                              exportQueryRequest
                                  .getListQueryRequest()
                                  .getUnderlay()
                                  .getQueryRunner()
                                  .run(exportQueryRequest))));
      // Kick off jobs in parallel.
      int threadPoolSize =
          maxChildThreads == null
              ? exportQueryRequests.size()
              : Math.min(exportQueryRequests.size(), maxChildThreads);
      LOGGER.info(
          "Running export requests in parallel, with a thread pool size of {}", threadPoolSize);
      Map<ExportQueryRequest, JobResult<ExportQueryResult>> exportJobResults =
          ThreadPoolUtils.runInParallel(threadPoolSize, exportJobs);
      exportJobResults.entrySet().stream()
          .forEach(
              exportJobResult -> {
                ExportQueryRequest exportQueryRequest = exportJobResult.getKey();
                JobResult<ExportQueryResult> jobResult = exportJobResult.getValue();
                if (jobResult == null) {
                  exportFileResults.add(
                      ExportFileResult.forEntityData(
                          null,
                          null,
                          exportQueryRequest.getListQueryRequest().getEntity(),
                          ExportError.forMessage(
                              "Export job did not complete or timed out", true)));
                } else {
                  exportFileResults.add(
                      ExportFileResult.forEntityData(
                          jobResult.getJobOutput() == null
                              ? null
                              : jobResult.getJobOutput().getFileDisplayName(),
                          jobResult.getJobOutput() == null
                              ? null
                              : jobResult.getJobOutput().getFilePath(),
                          exportQueryRequest.getListQueryRequest().getEntity(),
                          JobResult.Status.COMPLETED.equals(jobResult.getJobStatus())
                              ? null
                              : ExportError.forException(
                                  jobResult.getExceptionMessage(),
                                  jobResult.getExceptionStackTrace(),
                                  jobResult.isJobForceTerminated())));
                }
              });
    } else {
      // Kick off jobs in serial.
      LOGGER.info("Running export requests in serial");
      exportQueryRequests.stream()
          .forEach(
              exportQueryRequest -> {
                try {
                  ExportQueryResult exportQueryResult =
                      exportQueryRequest
                          .getListQueryRequest()
                          .getUnderlay()
                          .getQueryRunner()
                          .run(exportQueryRequest);
                  exportFileResults.add(
                      ExportFileResult.forEntityData(
                          exportQueryResult.getFileDisplayName(),
                          exportQueryResult.getFilePath(),
                          exportQueryRequest.getListQueryRequest().getEntity(),
                          null));
                } catch (Exception ex) {
                  exportFileResults.add(
                      ExportFileResult.forEntityData(
                          null,
                          null,
                          exportQueryRequest.getListQueryRequest().getEntity(),
                          ExportError.forException(ex)));
                }
              });
    }
    return exportFileResults;
  }

  /**
   * @param fileNameTemplate String substitution template for the filename. Must include ${cohort}
   *     and ${random} placeholders (e.g. annotations_cohort${cohort}_${random}).
   * @return List of annotation file outputs, including the full GCS path (e.g.
   *     gs://bucket/filename.csv.gzip).
   */
  public List<ExportFileResult> writeAnnotationDataToGcs(String fileNameTemplate) {
    // Just pick the first GCS bucket name.
    String bucketName = sharedExportConfig.getGcsBucketNames().get(0);

    // Write the annotations for each cohort to a separate file.
    List<ExportFileResult> exportFileResults = new ArrayList<>();
    exportRequest.getCohorts().stream()
        .forEach(
            cohort -> {
              try {
                String fileContents =
                    reviewService.buildCsvStringForAnnotationValues(
                        exportRequest.getStudy(), cohort);
                if (fileContents == null) {
                  exportFileResults.add(
                      ExportFileResult.forAnnotationData(
                          null,
                          null,
                          cohort,
                          ExportError.forMessage(
                              "There are no annotations for this cohort", false)));
                } else {
                  String fileName =
                      StringSubstitutor.replace(
                          fileNameTemplate,
                          Map.of(
                              "cohort",
                              NameUtils.simplifyStringForName(
                                  cohort.getDisplayName() + "_" + cohort.getId()),
                              "random",
                              Instant.now().getEpochSecond()
                                  + "_"
                                  + randomNumberGenerator.getNext()));
                  BlobId blobId = getStorageService().writeFile(bucketName, fileName, fileContents);
                  String gcsUrl = getStorageService().createSignedUrl(blobId.toGsUtilUri());
                  exportFileResults.add(
                      ExportFileResult.forAnnotationData(fileName, gcsUrl, cohort, null));
                }
              } catch (Exception ex) {
                exportFileResults.add(
                    ExportFileResult.forAnnotationData(
                        null, null, cohort, ExportError.forException(ex)));
              }
            });
    return exportFileResults;
  }

  /** Maintain a single reference to the GCS client object, so we don't keep recreating it. */
  public GoogleCloudStorage getStorageService() {
    if (googleCloudStorage == null) {
      googleCloudStorage =
          GoogleCloudStorage.forApplicationDefaultCredentials(sharedExportConfig.getGcpProjectId());
    }
    return googleCloudStorage;
  }

  public static String urlEncode(String param) {
    try {
      return URLEncoder.encode(param, StandardCharsets.UTF_8.toString());
    } catch (UnsupportedEncodingException ueEx) {
      throw new SystemException("Error encoding URL param: " + param, ueEx);
    }
  }
}

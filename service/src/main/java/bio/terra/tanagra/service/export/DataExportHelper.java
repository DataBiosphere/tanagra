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
import bio.terra.tanagra.utils.NameUtils;
import bio.terra.tanagra.utils.threadpool.Job;
import bio.terra.tanagra.utils.threadpool.JobResult;
import bio.terra.tanagra.utils.threadpool.ThreadPoolUtils;
import com.google.common.collect.ImmutableList;
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
  private final RandomNumberGenerator randomNumberGenerator;
  private final ReviewService reviewService;
  private final ExportRequest exportRequest;
  private final ImmutableList<EntityOutput> entityOutputs;
  private final EntityFilter primaryEntityFilter;
  private final boolean generateSignedUrl;

  public DataExportHelper(
      Integer maxChildThreads,
      ExportConfiguration.Shared sharedExportConfig,
      RandomNumberGenerator randomNumberGenerator,
      ReviewService reviewService,
      ExportRequest exportRequest,
      List<EntityOutput> entityOutputs,
      EntityFilter primaryEntityFilter,
      boolean generateSignedUrl) {
    this.maxChildThreads = maxChildThreads;
    this.randomNumberGenerator = randomNumberGenerator;
    this.reviewService = reviewService;
    this.exportRequest = exportRequest;
    this.entityOutputs = ImmutableList.copyOf(entityOutputs);
    this.primaryEntityFilter = primaryEntityFilter;
    this.generateSignedUrl = generateSignedUrl;
  }

  /**
   * @param isAgainstSourceDataset True to generate SQL queries against the source dataset.
   * @return Map of (entity,SQL query with all parameters substituted).
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
                                  false))
                      .collect(Collectors.toList());

              ListQueryRequest listQueryRequest =
                  isAgainstSourceDataset
                      ? ListQueryRequest.dryRunAgainstSourceData(
                          exportRequest.getUnderlay(),
                          entityOutput.getEntity(),
                          selectFields,
                          entityOutput.getDataFeatureFilter())
                      : ListQueryRequest.dryRunAgainstIndexData(
                          exportRequest.getUnderlay(),
                          entityOutput.getEntity(),
                          selectFields,
                          entityOutput.getDataFeatureFilter(),
                          null,
                          null);
              ListQueryResult listQueryResult =
                  exportRequest.getUnderlay().getQueryRunner().run(listQueryRequest);
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
            attribute ->
                (attributeNames.isEmpty() || attributeNames.contains(attribute.getName()))
                    && !attribute.isSuppressedForExport())
        .forEach(
            attribute ->
                selectedAttributeFields.add(
                    new AttributeField(
                        exportRequest.getUnderlay(),
                        exportRequest.getUnderlay().getPrimaryEntity(),
                        attribute,
                        false)));
    ListQueryRequest listQueryRequest =
        isAgainstSourceDataset
            ? ListQueryRequest.dryRunAgainstSourceData(
                exportRequest.getUnderlay(),
                exportRequest.getUnderlay().getPrimaryEntity(),
                selectedAttributeFields,
                primaryEntityFilter)
            : ListQueryRequest.dryRunAgainstIndexData(
                exportRequest.getUnderlay(),
                exportRequest.getUnderlay().getPrimaryEntity(),
                selectedAttributeFields,
                primaryEntityFilter,
                null,
                null);
    ListQueryResult listQueryResult =
        exportRequest.getUnderlay().getQueryRunner().run(listQueryRequest);
    return listQueryResult.getSqlNoParams();
  }

  /**
   * @return Map of (output entity name, total number of rows).
   */
  public Map<String, Long> getTotalNumRowsOfEntityData() {
    // Build set of list query requests with very small page size.
    List<ListQueryRequest> listQueryRequests =
        entityOutputs.stream()
            .map(
                entityOutput -> {
                  List<ValueDisplayField> selectFields =
                      entityOutput.getAttributes().stream()
                          .sorted(Comparator.comparing(Attribute::getName))
                          .map(
                              attribute ->
                                  new AttributeField(
                                      exportRequest.getUnderlay(),
                                      entityOutput.getEntity(),
                                      attribute,
                                      false))
                          .collect(Collectors.toList());
                  return ListQueryRequest.againstIndexData(
                      exportRequest.getUnderlay(),
                      entityOutput.getEntity(),
                      selectFields,
                      entityOutput.getDataFeatureFilter(),
                      null,
                      null,
                      null,
                      1);
                })
            .toList();

    Map<String, Long> totalNumRows = new HashMap<>();
    if (maxChildThreads == null || maxChildThreads > 1) {
      // Build set of list query jobs.
      Set<Job<ListQueryRequest, ListQueryResult>> listQueryJobs = new HashSet<>();
      listQueryRequests.forEach(
          listQueryRequest ->
              listQueryJobs.add(
                  new Job<>(
                      listQueryRequest.getEntity().getName() + '_' + Instant.now().toEpochMilli(),
                      listQueryRequest,
                      () ->
                          listQueryRequest.getUnderlay().getQueryRunner().run(listQueryRequest))));
      // Kick off jobs in parallel.
      int threadPoolSize =
          maxChildThreads == null
              ? listQueryRequests.size()
              : Math.min(listQueryRequests.size(), maxChildThreads);
      LOGGER.info(
          "Running list query requests in parallel, with a thread pool size of {}", threadPoolSize);
      Map<ListQueryRequest, JobResult<ListQueryResult>> listQueryJobResults =
          ThreadPoolUtils.runInParallel(threadPoolSize, listQueryJobs);
      listQueryJobResults.forEach(
          (listQueryRequest, jobResult) -> {
            if (jobResult == null) {
              LOGGER.error(
                  "List query did not complete or timed out: entity={}",
                  listQueryRequest.getEntity().getName());
              throw new SystemException(
                  "List query did not complete or timed out: entity="
                      + listQueryRequest.getEntity().getName());
            } else if (jobResult.getJobOutput() == null) {
              LOGGER.error(
                  "List query threw an exception: entity={}, {}",
                  listQueryRequest.getEntity().getName(),
                  jobResult.getExceptionMessage());
              throw new SystemException(
                  "List query threw an exception: entity="
                      + listQueryRequest.getEntity().getName()
                      + ", "
                      + jobResult.getExceptionMessage());
            } else {
              totalNumRows.put(
                  listQueryRequest.getEntity().getName(),
                  jobResult.getJobOutput().getNumRowsAcrossAllPages());
            }
          });
    } else {
      // Kick off jobs in serial.
      LOGGER.info("Running list query requests in serial");
      listQueryRequests.forEach(
          listQueryRequest -> {
            try {
              ListQueryResult listQueryResult =
                  listQueryRequest.getUnderlay().getQueryRunner().run(listQueryRequest);
              totalNumRows.put(
                  listQueryRequest.getEntity().getName(),
                  listQueryResult.getNumRowsAcrossAllPages());
            } catch (Exception ex) {
              LOGGER.error(
                  "List query threw an exception: entity={}",
                  listQueryRequest.getEntity().getName(),
                  ex);
              throw new SystemException(
                  "List query threw an exception: entity=" + listQueryRequest.getEntity().getName(),
                  ex);
            }
          });
    }
    return totalNumRows;
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
                                      false))
                          .collect(Collectors.toList());
                  ListQueryRequest listQueryRequest =
                      ListQueryRequest.againstIndexData(
                          exportRequest.getUnderlay(),
                          entityOutput.getEntity(),
                          selectFields,
                          entityOutput.getDataFeatureFilter(),
                          null,
                          null,
                          null,
                          null);

                  // Build a map of substitution strings for the filename template.
                  Map<String, String> substitutions =
                      Map.of(
                          "entity",
                          entityOutput.getEntity().getName(),
                          "random",
                          String.valueOf(randomNumberGenerator.getNext()));
                  String substitutedFilename =
                      StringSubstitutor.replace(fileNameTemplate, substitutions);
                  return ExportQueryRequest.forListQuery(
                      listQueryRequest,
                      entityOutput.getEntity().getName(),
                      substitutedFilename,
                      this.generateSignedUrl);
                })
            .toList();

    List<ExportFileResult> exportFileResults = new ArrayList<>();
    if (maxChildThreads == null || maxChildThreads > 1) {
      // Build set of export jobs.
      Set<Job<ExportQueryRequest, ExportQueryResult>> exportJobs = new HashSet<>();
      exportQueryRequests.forEach(
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
      exportJobResults.forEach(
          (exportQueryRequest, jobResult) -> {
            if (jobResult == null) {
              exportFileResults.add(
                  ExportFileResult.forEntityData(
                      null,
                      null,
                      exportQueryRequest.getListQueryRequest().getEntity(),
                      null,
                      ExportError.forMessage("Export job did not complete or timed out", false)));
            } else if (jobResult.getJobOutput() == null) {
              exportFileResults.add(
                  ExportFileResult.forEntityData(
                      null,
                      null,
                      exportQueryRequest.getListQueryRequest().getEntity(),
                      null,
                      ExportError.forException(
                          jobResult.getExceptionMessage(),
                          jobResult.getExceptionStackTrace(),
                          jobResult.isJobForceTerminated())));
            } else {
              exportFileResults.add(
                  ExportFileResult.forEntityData(
                      jobResult.getJobOutput().fileDisplayName(),
                      jobResult.getJobOutput().filePath(),
                      exportQueryRequest.getListQueryRequest().getEntity(),
                      jobResult.getJobOutput().filePath() == null
                          ? "Export query returned zero rows. No file generated."
                          : null,
                      null));
            }
          });
    } else {
      // Kick off jobs in serial.
      LOGGER.info("Running export requests in serial");
      exportQueryRequests.forEach(
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
                      exportQueryResult.fileDisplayName(),
                      exportQueryResult.filePath(),
                      exportQueryRequest.getListQueryRequest().getEntity(),
                      exportQueryResult.filePath() == null
                          ? "Export query returned zero rows. No file generated."
                          : null,
                      null));
            } catch (Exception ex) {
              exportFileResults.add(
                  ExportFileResult.forEntityData(
                      null,
                      null,
                      exportQueryRequest.getListQueryRequest().getEntity(),
                      null,
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
   *     gs://bucket/filename.csv).
   */
  public List<ExportFileResult> writeAnnotationDataToGcs(String fileNameTemplate) {
    // Write the annotations for each cohort to a separate file.
    List<ExportFileResult> exportFileResults = new ArrayList<>();
    exportRequest
        .getCohorts()
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
                          "Cohort has no annotation data. No file generated.",
                          null));
                } else {
                  String fileName =
                      StringSubstitutor.replace(
                          fileNameTemplate,
                          Map.of(
                              "cohort",
                              NameUtils.simplifyStringForName(cohort.getDisplayName()),
                              "random",
                              String.valueOf(randomNumberGenerator.getNext())));
                  if (!fileName.endsWith(".csv")) {
                    fileName += ".csv";
                  }

                  ExportQueryResult exportQueryResult = exportRawData(fileContents, fileName);
                  exportFileResults.add(
                      ExportFileResult.forAnnotationData(
                          fileName, exportQueryResult.filePath(), cohort, null, null));
                }
              } catch (Exception ex) {
                exportFileResults.add(
                    ExportFileResult.forAnnotationData(
                        null, null, cohort, null, ExportError.forException(ex)));
              }
            });
    return exportFileResults;
  }

  public ExportQueryResult exportRawData(String fileContents, String fileName) {
    ExportQueryRequest exportQueryRequest =
        ExportQueryRequest.forRawData(fileContents, fileName, this.generateSignedUrl);
    return exportRequest.getUnderlay().getQueryRunner().run(exportQueryRequest);
  }

  public static String urlEncode(String param) {
    return URLEncoder.encode(param, StandardCharsets.UTF_8);
  }

  public EntityFilter getPrimaryEntityFilter() {
    return primaryEntityFilter;
  }
}

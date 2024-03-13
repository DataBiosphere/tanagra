package bio.terra.tanagra.service.export;

import bio.terra.tanagra.api.filter.EntityFilter;
import bio.terra.tanagra.api.query.export.ExportQueryRequest;
import bio.terra.tanagra.api.query.export.ExportQueryResult;
import bio.terra.tanagra.api.query.list.ListQueryRequest;
import bio.terra.tanagra.app.configuration.ExportConfiguration;
import bio.terra.tanagra.app.configuration.ExportConfiguration.PerModel;
import bio.terra.tanagra.app.configuration.FeatureConfiguration;
import bio.terra.tanagra.app.controller.objmapping.ToApiUtils;
import bio.terra.tanagra.service.UnderlayService;
import bio.terra.tanagra.service.artifact.ActivityLogService;
import bio.terra.tanagra.service.artifact.CohortService;
import bio.terra.tanagra.service.artifact.ReviewService;
import bio.terra.tanagra.service.artifact.StudyService;
import bio.terra.tanagra.service.artifact.model.Cohort;
import bio.terra.tanagra.service.artifact.model.Study;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.utils.GoogleCloudStorage;
import bio.terra.tanagra.utils.NameUtils;
import bio.terra.tanagra.utils.RandomNumberGenerator;
import bio.terra.tanagra.utils.threadpool.Job;
import bio.terra.tanagra.utils.threadpool.JobResult;
import bio.terra.tanagra.utils.threadpool.ThreadPoolUtils;
import com.google.cloud.storage.BlobId;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.text.StringSubstitutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DataExportService {
  private static final Logger LOGGER = LoggerFactory.getLogger(DataExportService.class);

  private final FeatureConfiguration featureConfiguration;
  private final ExportConfiguration.Shared shared;
  private final Map<String, DataExport> modelToImpl = new HashMap<>();
  private final Map<String, ExportConfiguration.PerModel> modelToConfig = new HashMap<>();
  private final UnderlayService underlayService;
  private final StudyService studyService;
  private final CohortService cohortService;
  private final ReviewService reviewService;
  private final ActivityLogService activityLogService;
  private final RandomNumberGenerator randomNumberGenerator;
  private GoogleCloudStorage storageService;

  @Autowired
  @SuppressWarnings("checkstyle:ParameterNumber")
  public DataExportService(
      FeatureConfiguration featureConfiguration,
      ExportConfiguration exportConfiguration,
      UnderlayService underlayService,
      StudyService studyService,
      CohortService cohortService,
      ReviewService reviewService,
      ActivityLogService activityLogService,
      RandomNumberGenerator randomNumberGenerator) {
    this.featureConfiguration = featureConfiguration;
    this.shared = exportConfiguration.getShared();
    for (PerModel perModelConfig : exportConfiguration.getModels()) {
      DataExport dataExportImplInstance = perModelConfig.getType().createNewInstance();
      dataExportImplInstance.initialize(
          DeploymentConfig.fromApplicationConfig(shared, perModelConfig));

      // If no model name is defined, default it to the type name.
      String modelName = perModelConfig.getName();
      if (modelName == null || modelName.isEmpty()) {
        modelName = perModelConfig.getType().name();
      }
      this.modelToImpl.put(modelName, dataExportImplInstance);
      this.modelToConfig.put(modelName, perModelConfig);
    }
    this.underlayService = underlayService;
    this.studyService = studyService;
    this.cohortService = cohortService;
    this.reviewService = reviewService;
    this.activityLogService = activityLogService;
    this.randomNumberGenerator = randomNumberGenerator;
  }

  /** Return a map of model name -> (display name, implementation class instance). */
  public Map<String, Pair<String, DataExport>> getModels(String underlay) {
    // TODO: Allow configuring the list of implementations per underlay.
    return modelToImpl.keySet().stream()
        .collect(
            Collectors.toMap(
                Function.identity(),
                implName -> {
                  DataExport impl = modelToImpl.get(implName);
                  String displayName = modelToConfig.get(implName).getDisplayName();
                  if (displayName == null || displayName.isEmpty()) {
                    displayName = impl.getDefaultDisplayName();
                  }
                  return Pair.of(displayName, impl);
                }));
  }

  public ExportResult run(
      String studyId,
      List<String> cohortIds,
      ExportRequest.Builder request,
      List<ListQueryRequest> listQueryRequests,
      EntityFilter allCohortsEntityFilter,
      String userEmail) {
    // Make the current cohort revision un-editable, and create the next version.
    Map<String, String> cohortToRevisionIdMap = new HashMap<>();
    cohortIds.stream()
        .forEach(
            cohortId -> {
              String revisionId =
                  cohortService.createNextRevision(studyId, cohortId, userEmail, null);
              cohortToRevisionIdMap.put(cohortId, revisionId);
            });

    // Populate the study, cohort, and underlay API objects in the request.
    Study study = studyService.getStudy(studyId);
    List<Cohort> cohorts =
        cohortIds.stream()
            .map(cohortId -> cohortService.getCohort(studyId, cohortId))
            .collect(Collectors.toList());
    Underlay underlay = underlayService.getUnderlay(cohorts.get(0).getUnderlay());
    request
        .underlay(ToApiUtils.toApiObject(underlay))
        .study(ToApiUtils.toApiObject(study))
        .cohorts(
            cohorts.stream()
                .map(cohort -> ToApiUtils.toApiObject(cohort))
                .collect(Collectors.toList()));

    // Populate the function pointers for generating SQL query strings and writing GCS files.
    // Instead of executing these functions here and passing the outputs to the implementation
    // class, we just pass the function pointers to allow for lazy execution, or no execution at
    // all.
    // e.g. To export an ipynb file with the SQL queries embedded in it, there's no need to write
    // anything to GCS.
    if (!listQueryRequests.isEmpty()) {
      request.generateSqlQueriesFn(() -> generateSqlQueries(listQueryRequests));
      request.writeEntityDataToGcsFn(
          fileNameTemplate -> writeEntityDataToGcs(fileNameTemplate, listQueryRequests));
    }
    if (request.isIncludeAnnotations()) {
      request.writeAnnotationDataToGcsFn(
          fileNameTemplate -> writeAnnotationDataToGcs(fileNameTemplate, study, cohorts));
    }
    request.getGoogleCloudStorageFn(() -> getStorageService());

    // Get the implementation class instance for the requested data export model.
    DataExport impl = modelToImpl.get(request.getModel());
    ExportResult result = impl.run(request.build());

    // Calculate the number of primary entity instances that were included in this export request.
    // TODO: Remove the null handling here once the UI is passing the primary entity filter to the
    // export endpoint.
    long allCohortsCount;
    if (allCohortsEntityFilter == null) {
      allCohortsCount = -1;
      LOGGER.error(
          "allCohortsEntityFilter is null. This should only happen temporarily while the UI is not yet passing the filter to the API.");
    } else {
      allCohortsCount = cohortService.getRecordsCount(underlay.getName(), allCohortsEntityFilter);
    }
    activityLogService.logExport(
        request.getModel(), allCohortsCount, userEmail, studyId, cohortToRevisionIdMap);
    return result;
  }

  /** Generate the entity instance SQL queries. */
  private static Map<String, String> generateSqlQueries(List<ListQueryRequest> listQueryRequests) {
    return listQueryRequests.stream()
        .collect(
            Collectors.toMap(
                qr -> qr.getEntity().getName(),
                qr -> {
                  ListQueryRequest dryRunQueryRequest = qr.cloneAndSetDryRun();
                  return qr.getUnderlay().getQueryRunner().run(dryRunQueryRequest).getSqlNoParams();
                }));
  }

  /**
   * Execute the entity instance queries and write the results to a GCS file.
   *
   * @param fileNameTemplate Template string for the GCS filenames. It must contain a single
   *     wildcard character * and at least one reference to the entity name ${entity}.
   * @return map of entity name -> GCS full path (e.g. gs://bucket/filename.csv)
   */
  private Map<String, String> writeEntityDataToGcs(
      String fileNameTemplate, List<ListQueryRequest> listQueryRequests) {
    // Build set of export requests.
    List<ExportQueryRequest> exportQueryRequests =
        listQueryRequests.stream()
            .map(
                listQueryRequest -> {
                  // Build a map of substitution strings for the filename template.
                  Map<String, String> substitutions =
                      Map.of(
                          "entity",
                          listQueryRequest.getEntity().getName(),
                          "random",
                          Instant.now().getEpochSecond() + "_" + randomNumberGenerator.getNext());
                  String substitutedFilename =
                      StringSubstitutor.replace(fileNameTemplate, substitutions);
                  return new ExportQueryRequest(
                      listQueryRequest,
                      listQueryRequest.getEntity().getName(),
                      substitutedFilename,
                      shared.getGcpProjectId(),
                      shared.getBqDatasetIds(),
                      shared.getGcsBucketNames());
                })
            .collect(Collectors.toList());

    List<ExportQueryResult> exportQueryResults = new ArrayList<>();
    if (!featureConfiguration.hasMaxChildThreads()
        || featureConfiguration.getMaxChildThreads() > 1) {
      // Build set of export jobs.
      Set<Job<ExportQueryResult>> exportJobs = new HashSet<>();
      exportQueryRequests.stream()
          .forEach(
              exportQueryRequest ->
                  exportJobs.add(
                      new Job<>(
                          exportQueryRequest.getListQueryRequest().getEntity().getName()
                              + '_'
                              + Instant.now().toEpochMilli(),
                          () ->
                              exportQueryRequest
                                  .getListQueryRequest()
                                  .getUnderlay()
                                  .getQueryRunner()
                                  .run(exportQueryRequest))));
      // Kick off jobs in parallel.
      int threadPoolSize =
          featureConfiguration.hasMaxChildThreads()
              ? Math.min(listQueryRequests.size(), featureConfiguration.getMaxChildThreads())
              : listQueryRequests.size();
      LOGGER.info(
          "Running export requests in parallel, with a thread pool size of {}", threadPoolSize);
      Set<JobResult<ExportQueryResult>> exportJobResults =
          ThreadPoolUtils.runInParallel(threadPoolSize, exportJobs);
      exportJobResults.stream()
          .forEach(
              exportJobResult -> {
                if (exportJobResult != null
                    && JobResult.Status.COMPLETED.equals(exportJobResult.getJobStatus())
                    && exportJobResult.getJobOutput() != null) {
                  exportQueryResults.add(exportJobResult.getJobOutput());
                }
              });
    } else {
      // Kick off jobs in serial.
      LOGGER.info("Running export requests in serial");
      exportQueryRequests.stream()
          .forEach(
              exportQueryRequest -> {
                ExportQueryResult exportQueryResult =
                    exportQueryRequest
                        .getListQueryRequest()
                        .getUnderlay()
                        .getQueryRunner()
                        .run(exportQueryRequest);
                exportQueryResults.add(exportQueryResult);
              });
    }

    // Compile the results.
    Map<String, String> entityToUrlMap = new HashMap<>();
    exportQueryResults.stream()
        .forEach(
            // TODO: Pass out any error information also, once that's included in the OpenAPI spec.
            exportQueryResult -> {
              if (exportQueryResult.getFilePath() != null) {
                entityToUrlMap.put(
                    exportQueryResult.getFileDisplayName(), exportQueryResult.getFilePath());
              }
            });
    return entityToUrlMap;
  }

  /**
   * For each cohort, execute the annotation query and write the results to a GCS file.
   *
   * @param fileNameTemplate Template string for the GCS filenames. It must contain at least one
   *     reference to the cohort id ${cohort}.
   * @return map of cohort id -> GCS full path (e.g. gs://bucket/filename.csv)
   */
  private Map<Cohort, String> writeAnnotationDataToGcs(
      String fileNameTemplate, Study study, List<Cohort> cohorts) {
    // Just pick the first GCS bucket name.
    String bucketName = shared.getGcsBucketNames().get(0);
    Map<Cohort, String> cohortToGcsUrl = new HashMap<>();
    cohorts.stream()
        .forEach(
            cohort -> {
              String fileContents = reviewService.buildCsvStringForAnnotationValues(study, cohort);
              if (fileContents != null) {
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
                cohortToGcsUrl.put(cohort, blobId.toGsUtilUri());
              }
            });
    return cohortToGcsUrl;
  }

  private GoogleCloudStorage getStorageService() {
    if (storageService == null) {
      storageService =
          GoogleCloudStorage.forApplicationDefaultCredentials(shared.getGcpProjectId());
    }
    return storageService;
  }
}

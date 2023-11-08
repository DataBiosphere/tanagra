package bio.terra.tanagra.service.export;

import bio.terra.tanagra.api2.filter.EntityFilter;
import bio.terra.tanagra.api2.query.EntityQueryRunner;
import bio.terra.tanagra.api2.query.list.ListQueryRequest;
import bio.terra.tanagra.app.configuration.ExportConfiguration;
import bio.terra.tanagra.app.configuration.ExportConfiguration.PerModel;
import bio.terra.tanagra.app.controller.objmapping.ToApiUtils;
import bio.terra.tanagra.query.QueryExecutor;
import bio.terra.tanagra.service.artifact.ActivityLogService;
import bio.terra.tanagra.service.artifact.CohortService;
import bio.terra.tanagra.service.artifact.ReviewService;
import bio.terra.tanagra.service.artifact.StudyService;
import bio.terra.tanagra.service.artifact.model.Cohort;
import bio.terra.tanagra.service.artifact.model.Study;
import bio.terra.tanagra.service.query.UnderlayService;
import bio.terra.tanagra.underlay2.Underlay;
import bio.terra.tanagra.utils.GoogleCloudStorage;
import bio.terra.tanagra.utils.NameUtils;
import com.google.cloud.storage.BlobId;
import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
  private final ExportConfiguration.Shared shared;
  private final Map<String, DataExport> modelToImpl = new HashMap<>();
  private final Map<String, ExportConfiguration.PerModel> modelToConfig = new HashMap<>();
  private final UnderlayService underlayService;
  private final StudyService studyService;
  private final CohortService cohortService;
  private final ReviewService reviewService;
  private final ActivityLogService activityLogService;
  private GoogleCloudStorage storageService;

  @Autowired
  public DataExportService(
      ExportConfiguration exportConfiguration,
      UnderlayService underlayService,
      StudyService studyService,
      CohortService cohortService,
      ReviewService reviewService,
      ActivityLogService activityLogService) {
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
                qr -> EntityQueryRunner.buildQueryRequest(qr).getSql()));
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
    return listQueryRequests.stream()
        .collect(
            Collectors.toMap(
                qr -> qr.getEntity().getName(),
                qr -> {
                  // TODO: Make an export to GCS query part of the underlay/api, so we're not
                  // building Query objects outside the underlay sub-project.
                  QueryExecutor queryExecutor = qr.getUnderlay().getQueryExecutor();
                  String wildcardFilename =
                      getFileName(fileNameTemplate, "entity", qr.getEntity().getName());
                  return queryExecutor.executeAndExportResultsToGcs(
                      EntityQueryRunner.buildQueryRequest(qr),
                      wildcardFilename,
                      shared.getGcsProjectId(),
                      shared.getGcsBucketNames());
                }));
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
    return cohorts.stream()
        .collect(
            Collectors.toMap(
                Function.identity(),
                cohort -> {
                  String cohortIdAndName =
                      NameUtils.simplifyStringForName(
                          cohort.getDisplayName() + "_" + cohort.getId());
                  String fileName = getFileName(fileNameTemplate, "cohort", cohortIdAndName);
                  String fileContents =
                      reviewService.buildTsvStringForAnnotationValues(study, cohort);
                  BlobId blobId = getStorageService().writeFile(bucketName, fileName, fileContents);
                  return blobId.toGsUtilUri();
                }));
  }

  /** Return the rendered filename with substitutions. */
  private String getFileName(String template, String substituteFor, String substituteWith) {
    if (!template.contains("${" + substituteFor + "}")) {
      throw new IllegalArgumentException(
          "GCS filename template must contain a substitution for ${"
              + substituteFor
              + "}: "
              + template);
    }
    Map<String, String> params =
        ImmutableMap.<String, String>builder().put(substituteFor, substituteWith).build();
    return StringSubstitutor.replace(template, params);
  }

  private GoogleCloudStorage getStorageService() {
    if (storageService == null) {
      storageService =
          GoogleCloudStorage.forApplicationDefaultCredentials(shared.getGcsProjectId());
    }
    return storageService;
  }
}

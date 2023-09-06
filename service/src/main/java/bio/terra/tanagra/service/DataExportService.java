package bio.terra.tanagra.service;

import bio.terra.tanagra.app.configuration.ExportConfiguration;
import bio.terra.tanagra.app.configuration.ExportConfiguration.PerModel;
import bio.terra.tanagra.query.QueryExecutor;
import bio.terra.tanagra.service.artifact.Cohort;
import bio.terra.tanagra.service.artifact.Study;
import bio.terra.tanagra.service.export.DataExport;
import bio.terra.tanagra.service.export.DeploymentConfig;
import bio.terra.tanagra.service.export.ExportRequest;
import bio.terra.tanagra.service.export.ExportResult;
import bio.terra.tanagra.service.instances.EntityQueryRequest;
import bio.terra.tanagra.service.utils.ToApiConversionUtils;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.utils.GoogleCloudStorage;
import bio.terra.tanagra.utils.NameUtils;
import com.google.cloud.storage.BlobId;
import com.google.common.collect.ImmutableMap;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.text.StringSubstitutor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DataExportService {
  private final ExportConfiguration.Shared shared;
  private final Map<String, DataExport> modelToImpl = new HashMap<>();
  private final Map<String, ExportConfiguration.PerModel> modelToConfig = new HashMap<>();
  private final UnderlaysService underlaysService;
  private final StudyService studyService;
  private final CohortService cohortService;
  private final ReviewService reviewService;
  private final ActivityLogService activityLogService;
  private GoogleCloudStorage storageService;

  @Autowired
  public DataExportService(
      ExportConfiguration exportConfiguration,
      UnderlaysService underlaysService,
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
    this.underlaysService = underlaysService;
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
      List<EntityQueryRequest> entityQueryRequests,
      String userEmail) {
    // Make the current cohort revision un-editable, and create the next version.
    Map<String, String> cohortToRevisionIdMap = new HashMap<>();
    cohortIds.stream()
        .forEach(
            cohortId -> {
              String revisionId = cohortService.createNextRevision(studyId, cohortId, userEmail);
              cohortToRevisionIdMap.put(cohortId, revisionId);
            });

    // Populate the study, cohort, and underlay API objects in the request.
    Study study = studyService.getStudy(studyId);
    List<Cohort> cohorts =
        cohortIds.stream()
            .map(cohortId -> cohortService.getCohort(studyId, cohortId))
            .collect(Collectors.toList());
    Underlay underlay = underlaysService.getUnderlay(cohorts.get(0).getUnderlay());
    request
        .underlay(ToApiConversionUtils.toApiObject(underlay))
        .study(ToApiConversionUtils.toApiObject(study))
        .cohorts(
            cohorts.stream()
                .map(cohort -> ToApiConversionUtils.toApiObject(cohort))
                .collect(Collectors.toList()));

    // Populate the function pointers for generating SQL query strings and writing GCS files.
    // Instead of executing these functions here and passing the outputs to the implementation
    // class, we just pass the function pointers to allow for lazy execution, or no execution at
    // all.
    // e.g. To export an ipynb file with the SQL queries embedded in it, there's no need to write
    // anything to GCS.
    if (!entityQueryRequests.isEmpty()) {
      request.generateSqlQueriesFn(() -> generateSqlQueries(entityQueryRequests));
      request.writeEntityDataToGcsFn(
          fileNameTemplate -> writeEntityDataToGcs(fileNameTemplate, entityQueryRequests));
    }
    if (request.isIncludeAnnotations()) {
      request.writeAnnotationDataToGcsFn(
          fileNameTemplate -> writeAnnotationDataToGcs(fileNameTemplate, study, cohorts));
    }
    request.getGoogleCloudStorageFn(() -> getStorageService());

    // Get the implementation class instance for the requested data export model.
    DataExport impl = modelToImpl.get(request.getModel());
    ExportResult result = impl.run(request.build());
    activityLogService.logExport(request.getModel(), userEmail, studyId, cohortToRevisionIdMap);
    return result;
  }

  /** Generate the entity instance SQL queries. */
  private static Map<String, String> generateSqlQueries(
      List<EntityQueryRequest> entityQueryRequests) {
    return entityQueryRequests.stream()
        .collect(
            Collectors.toMap(
                eqr -> eqr.getEntity().getName(), eqr -> eqr.buildInstancesQuery().getSql()));
  }

  /**
   * Execute the entity instance queries and write the results to a GCS file.
   *
   * @param fileNameTemplate Template string for the GCS filenames. It must contain a single
   *     wildcard character * and at least one reference to the entity name ${entity}.
   * @return map of entity name -> GCS full path (e.g. gs://bucket/filename.csv)
   */
  private Map<String, String> writeEntityDataToGcs(
      String fileNameTemplate, List<EntityQueryRequest> entityQueryRequests) {
    return entityQueryRequests.stream()
        .collect(
            Collectors.toMap(
                eqr -> eqr.getEntity().getName(),
                eqr -> {
                  QueryExecutor queryExecutor =
                      eqr.getEntity()
                          .getMapping(eqr.getMappingType())
                          .getTablePointer()
                          .getDataPointer()
                          .getQueryExecutor();
                  String wildcardFilename =
                      getFileName(fileNameTemplate, "entity", eqr.getEntity().getName());
                  return queryExecutor.executeAndExportResultsToGcs(
                      eqr.buildInstancesQuery(),
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

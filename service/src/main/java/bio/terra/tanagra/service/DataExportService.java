package bio.terra.tanagra.service;

import bio.terra.tanagra.app.configuration.ExportConfiguration;
import bio.terra.tanagra.app.configuration.ExportConfiguration.ExportModelConfiguration;
import bio.terra.tanagra.exception.SystemException;
import bio.terra.tanagra.query.QueryExecutor;
import bio.terra.tanagra.service.export.DataExport;
import bio.terra.tanagra.service.export.DataExport.Model;
import bio.terra.tanagra.service.export.ExportRequest;
import bio.terra.tanagra.service.export.ExportResult;
import bio.terra.tanagra.service.export.impl.GcsTransferServiceFile;
import bio.terra.tanagra.service.export.impl.ListOfSignedUrls;
import bio.terra.tanagra.service.instances.EntityQueryRequest;
import bio.terra.tanagra.utils.GoogleCloudStorage;
import com.google.cloud.storage.BlobId;
import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.text.StringSubstitutor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DataExportService {
  private final ExportConfiguration.ExportInfraConfiguration exportInfraConfiguration;
  private final Map<Model, DataExport> dataExportModelToImpl = new HashMap<>();

  private final ReviewService reviewService;
  private final GoogleCloudStorage storageService;

  @Autowired
  public DataExportService(ExportConfiguration exportConfiguration, ReviewService reviewService) {
    this.exportInfraConfiguration = exportConfiguration.getCommonInfra();
    for (ExportModelConfiguration exportModelConfig : exportConfiguration.getModels()) {
      DataExport dataExportImplInstance;
      switch (exportModelConfig.getModel()) {
        case LIST_OF_SIGNED_URLS:
          dataExportImplInstance = new ListOfSignedUrls();
          break;
        case GCS_TRANSFER_SERVICE_FILE:
          dataExportImplInstance = new GcsTransferServiceFile();
          break;
        default:
          throw new SystemException("Unknown data export model: " + exportModelConfig.getModel());
      }
      dataExportImplInstance.initialize(
          DataExport.CommonInfrastructure.fromApplicationConfig(exportInfraConfiguration),
          exportModelConfig.getParams());
      this.dataExportModelToImpl.put(exportModelConfig.getModel(), dataExportImplInstance);
    }
    this.reviewService = reviewService;
    this.storageService =
        GoogleCloudStorage.forApplicationDefaultCredentials(
            exportInfraConfiguration.getGcsProjectId());
  }

  public ExportResult run(
      ExportRequest.Builder request, List<EntityQueryRequest> entityQueryRequests) {
    // Get the implementation class instance for the requested data export model.
    DataExport impl = dataExportModelToImpl.get(request.getModel());

    // Populate the function pointers for generating SQL query strings and writing GCS files.
    // Instead of executing these functions here and passing the outputs to the implementation
    // class,
    // we just pass the function pointers to allow for lazy execution, or no execution at all.
    // e.g. To export an ipynb file with the SQL queries embedded in it, there's no need to write
    // anything to GCS.
    if (!entityQueryRequests.isEmpty()) {
      request.generateSqlQueriesFn(() -> generateSqlQueries(entityQueryRequests));
      request.writeEntityDataToGcsFn(
          fileNameTemplate -> writeEntityDataToGcs(fileNameTemplate, entityQueryRequests));
    }
    if (request.isIncludeAnnotations()) {
      request.writeAnnotationDataToGcsFn(
          fileNameTemplate ->
              writeAnnotationDataToGcs(fileNameTemplate, request.getStudy(), request.getCohorts()));
    }

    return impl.run(request.build());
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
                      exportInfraConfiguration.getGcsProjectId(),
                      exportInfraConfiguration.getGcsBucketNames());
                }));
  }

  /**
   * For each cohort, execute the annotation query and write the results to a GCS file.
   *
   * @param fileNameTemplate Template string for the GCS filenames. It must contain at least one
   *     reference to the cohort id ${cohort}.
   * @return map of cohort id -> GCS full path (e.g. gs://bucket/filename.csv)
   */
  private Map<String, String> writeAnnotationDataToGcs(
      String fileNameTemplate, String studyId, List<String> cohorts) {
    // Just pick the first GCS bucket name.
    String bucketName = exportInfraConfiguration.getGcsBucketNames().get(0);
    return cohorts.stream()
        .collect(
            Collectors.toMap(
                Function.identity(),
                cohortId -> {
                  String fileName = getFileName(fileNameTemplate, "cohort", cohortId);
                  String fileContents =
                      reviewService.buildTsvStringForAnnotationValues(studyId, cohortId);
                  BlobId blobId = storageService.writeFile(bucketName, fileName, fileContents);
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
}

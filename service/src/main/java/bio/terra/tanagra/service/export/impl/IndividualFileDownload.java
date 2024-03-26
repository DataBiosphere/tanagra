package bio.terra.tanagra.service.export.impl;

import static bio.terra.tanagra.utils.NameUtils.simplifyStringForName;

import bio.terra.tanagra.service.export.DataExport;
import bio.terra.tanagra.service.export.DataExportHelper;
import bio.terra.tanagra.service.export.DeploymentConfig;
import bio.terra.tanagra.service.export.ExportFileResult;
import bio.terra.tanagra.service.export.ExportRequest;
import bio.terra.tanagra.service.export.ExportResult;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IndividualFileDownload implements DataExport {

  private static final String ENTITY_OUTPUT_KEY_PREFIX = "Data:";
  private static final String COHORT_OUTPUT_KEY_PREFIX = "Annotations:";

  @Override
  public Type getType() {
    return Type.INDIVIDUAL_FILE_DOWNLOAD;
  }

  @Override
  public String getDefaultDisplayName() {
    return "Download individual files";
  }

  @Override
  public String getDescription() {
    return "List of file URLs. Each URL points to a single file with either query results or annotation data.";
  }

  @Override
  public Map<String, String> describeOutputs() {
    return Map.of(
        ENTITY_OUTPUT_KEY_PREFIX + "*", "URL to query results",
        COHORT_OUTPUT_KEY_PREFIX + "*", "URL to annotation data for a cohort");
  }

  @Override
  public void initialize(DeploymentConfig deploymentConfig) {
    // Model-specific deployment config is currently ignored. Possible future uses: expiry time for
    // signed url, filename template, csv/tsv/etc. format
  }

  @Override
  public ExportResult run(ExportRequest request, DataExportHelper helper) {
    // Export the entity and annotation data to GCS.
    String studyUnderlayRef =
        simplifyStringForName(request.getStudy().getId() + "_" + request.getUnderlay().getName());
    String cohortRef =
        simplifyStringForName(
                request.getCohorts().get(0).getDisplayName()
                    + "_"
                    + request.getCohorts().get(0).getId())
            + (request.getCohorts().size() > 1
                ? "_plus" + (request.getCohorts().size() - 1) + "more"
                : "");
    List<ExportFileResult> entityExportFileResults =
        helper.writeEntityDataToGcs(
            "${entity}_cohort" + cohortRef + "_" + studyUnderlayRef + "_${random}");
    List<ExportFileResult> annotationExportFileResults =
        helper.writeAnnotationDataToGcs(
            "annotations_cohort${cohort}" + "_" + studyUnderlayRef + "_${random}");

    // Build a combined list of all output files.
    List<ExportFileResult> allExportFileResults = new ArrayList<>();
    // Set the tags for each file result, and suppress empty files.
    entityExportFileResults.stream()
        .filter(ExportFileResult::hasFileUrl)
        .forEach(
            exportFileResult -> {
              exportFileResult.addTags(List.of("Data", exportFileResult.getEntity().getName()));
              allExportFileResults.add(exportFileResult);
            });
    annotationExportFileResults.stream()
        .filter(ExportFileResult::hasFileUrl)
        .forEach(
            exportFileResult -> {
              exportFileResult.addTags(
                  List.of("Annotations", exportFileResult.getCohort().getDisplayName()));
              allExportFileResults.add(exportFileResult);
            });

    // TODO: Skip populating these output parameters once the UI is processing the file results
    // directly.
    Map<String, String> outputParams = new HashMap<>();
    entityExportFileResults.stream()
        .filter(
            exportFileResult -> exportFileResult.isSuccessful() && exportFileResult.hasFileUrl())
        .forEach(
            exportFileResult ->
                outputParams.put(
                    ENTITY_OUTPUT_KEY_PREFIX + exportFileResult.getEntity().getName(),
                    exportFileResult.getFileUrl()));
    annotationExportFileResults.stream()
        .filter(
            annotationExportFileResult ->
                annotationExportFileResult.isSuccessful()
                    && annotationExportFileResult.hasFileUrl())
        .forEach(
            exportFileResult -> {
              String cohortName = exportFileResult.getCohort().getDisplayName();
              if (cohortName == null || cohortName.isEmpty()) {
                cohortName = exportFileResult.getCohort().getId();
              }
              outputParams.put(
                  COHORT_OUTPUT_KEY_PREFIX + cohortName, exportFileResult.getFileUrl());
            });

    return ExportResult.forOutputParams(outputParams, allExportFileResults);
  }
}

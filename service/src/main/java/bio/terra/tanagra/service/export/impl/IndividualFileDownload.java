package bio.terra.tanagra.service.export.impl;

import static bio.terra.tanagra.utils.NameUtils.simplifyStringForName;

import bio.terra.tanagra.service.export.DataExport;
import bio.terra.tanagra.service.export.DataExportHelper;
import bio.terra.tanagra.service.export.DeploymentConfig;
import bio.terra.tanagra.service.export.ExportFileResult;
import bio.terra.tanagra.service.export.ExportRequest;
import bio.terra.tanagra.service.export.ExportResult;
import java.util.ArrayList;
import java.util.List;

public class IndividualFileDownload implements DataExport {

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
        .filter(
            exportFileResult -> exportFileResult.hasFileUrl() || !exportFileResult.isSuccessful())
        .forEach(
            exportFileResult -> {
              exportFileResult.addTags(List.of("Data", exportFileResult.getEntity().getName()));
              allExportFileResults.add(exportFileResult);
            });
    annotationExportFileResults.stream()
        .filter(
            exportFileResult -> exportFileResult.hasFileUrl() || !exportFileResult.isSuccessful())
        .forEach(
            exportFileResult -> {
              exportFileResult.addTags(
                  List.of("Annotations", exportFileResult.getCohort().getDisplayName()));
              allExportFileResults.add(exportFileResult);
            });

    return ExportResult.forFileResults(allExportFileResults);
  }
}

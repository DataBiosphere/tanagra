package bio.terra.tanagra.service.export.impl;

import bio.terra.tanagra.service.export.DataExport;
import bio.terra.tanagra.service.export.DataExportHelper;
import bio.terra.tanagra.service.export.DeploymentConfig;
import bio.terra.tanagra.service.export.ExportFileResult;
import bio.terra.tanagra.service.export.ExportRequest;
import bio.terra.tanagra.service.export.ExportResult;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class IndividualFileDownload implements DataExport {

  @Override
  public Type getType() {
    return Type.INDIVIDUAL_FILE_DOWNLOAD;
  }

  @Override
  public String getDefaultDisplayName() {
    return "Download CSV";
  }

  @Override
  public String getDescription() {
    return "Get links to one or more CSV files";
  }

  @Override
  public void initialize(DeploymentConfig deploymentConfig) {
    // Model-specific deployment config is currently ignored. Possible future uses: expiry time for
    // signed url, filename template, csv/tsv/etc. format
  }

  @Override
  public ExportResult run(ExportRequest request, DataExportHelper helper) {
    // Export the entity and annotation data to GCS.
    // Filename template: YYYYMMDD_HHMMSS_{random}_{type}_{name}
    String timestamp =
        DateTimeFormatter.ofPattern("yyyyMMdd_hhmmss")
            .withZone(ZoneId.of("UTC"))
            .format(Instant.now());
    // e.g. 20240422_132237_1234_data_person
    List<ExportFileResult> entityExportFileResults =
        helper.writeEntityDataToGcs(timestamp + "_${random}_data_${entity}");
    // e.g. 202040422_132237_1234_annotation_MyCohort
    List<ExportFileResult> annotationExportFileResults =
        helper.writeAnnotationDataToGcs(timestamp + "_${random}_annotation_${cohort}");

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

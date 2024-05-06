package bio.terra.tanagra.service.export.impl;

import static bio.terra.tanagra.service.export.DataExportHelper.urlEncode;

import bio.terra.tanagra.service.export.DataExport;
import bio.terra.tanagra.service.export.DataExportHelper;
import bio.terra.tanagra.service.export.DeploymentConfig;
import bio.terra.tanagra.service.export.ExportFileResult;
import bio.terra.tanagra.service.export.ExportRequest;
import bio.terra.tanagra.service.export.ExportResult;
import com.google.cloud.storage.BlobId;
import com.google.common.collect.ImmutableMap;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.commons.text.StringSubstitutor;

public class VwbFileImport implements DataExport {
  private static final String FILE_FORMAT_SPECIFIER = "TsvHttpData-1.0";
  private List<String> gcsBucketNames;
  private String redirectAwayUrl;

  @Override
  public Type getType() {
    return Type.VWB_FILE_IMPORT;
  }

  @Override
  public String getDefaultDisplayName() {
    return "Import to VWB";
  }

  @Override
  public String getDescription() {
    return "Redirect URL to VWB that includes a signed URL to a file that contains a list of signed URLs. Each URL in the file points to a single file with either query results or annotation data.";
  }

  @Override
  public void initialize(DeploymentConfig deploymentConfig) {
    gcsBucketNames = deploymentConfig.getShared().getGcsBucketNames();
    redirectAwayUrl = deploymentConfig.getRedirectAwayUrl();
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

    // Build a list of the signed URLs, sorted lexicographically.
    // Build a TSV-string from the sorted list of rows, prefixed with the format header.
    StringBuilder fileContents = new StringBuilder(FILE_FORMAT_SPECIFIER + "\n");
    allExportFileResults.stream()
        .filter(
            exportFileResult -> exportFileResult.isSuccessful() && exportFileResult.hasFileUrl())
        .map(ExportFileResult::getFileUrl)
        .sorted()
        .forEach(tsvRow -> fileContents.append(tsvRow).append("\n"));

    // Write the TSV file to GCS. Just pick the first bucket name.
    String fileName = "tanagra_vwb_export_" + Instant.now() + ".tsv";
    BlobId blobId =
        helper
            .getStorageService()
            .writeFile(gcsBucketNames.get(0), fileName, fileContents.toString());

    // Generate a signed URL for the TSV file.
    String tsvSignedUrl = helper.getStorageService().createSignedUrl(blobId.toGsUtilUri());
    ExportFileResult tsvExportFileResult =
        ExportFileResult.forFile(fileName, tsvSignedUrl, null, null);
    tsvExportFileResult.addTags(List.of("URL List"));
    allExportFileResults.add(tsvExportFileResult);

    // Generate the redirect URL to VWB.
    Map<String, String> urlParams =
        ImmutableMap.<String, String>builder()
            .put("tsvFileUrl", urlEncode(tsvSignedUrl))
            .put("redirectBackUrl", urlEncode(request.getRedirectBackUrl()))
            .build();
    String expandedRedirectAwayUrl = StringSubstitutor.replace(redirectAwayUrl, urlParams);
    return ExportResult.forRedirectUrl(expandedRedirectAwayUrl, allExportFileResults);
  }
}

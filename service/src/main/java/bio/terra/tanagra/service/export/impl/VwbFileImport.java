package bio.terra.tanagra.service.export.impl;

import static bio.terra.tanagra.service.export.DataExportHelper.urlEncode;
import static bio.terra.tanagra.utils.NameUtils.simplifyStringForName;

import bio.terra.tanagra.service.export.DataExport;
import bio.terra.tanagra.service.export.DataExportHelper;
import bio.terra.tanagra.service.export.DeploymentConfig;
import bio.terra.tanagra.service.export.ExportFileResult;
import bio.terra.tanagra.service.export.ExportRequest;
import bio.terra.tanagra.service.export.ExportResult;
import com.google.cloud.storage.BlobId;
import com.google.common.collect.ImmutableMap;
import java.time.Instant;
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
    List<ExportFileResult> allExportFileResults = new ArrayList<>();
    allExportFileResults.addAll(entityExportFileResults);
    allExportFileResults.addAll(annotationExportFileResults);

    // Build a list of the signed URLs, sorted lexicographically.
    // Build a TSV-string from the sorted list of rows, prefixed with the format header.
    StringBuilder fileContents = new StringBuilder(FILE_FORMAT_SPECIFIER + "\n");
    allExportFileResults.stream()
        .map(ExportFileResult::getFileUrl)
        .sorted()
        .forEach(tsvRow -> fileContents.append(tsvRow + "\n"));

    // Write the TSV file to GCS. Just pick the first bucket name.
    String fileName = "tanagra_vwb_export_" + Instant.now() + ".tsv";
    BlobId blobId =
        helper
            .getStorageService()
            .writeFile(gcsBucketNames.get(0), fileName, fileContents.toString());

    // Generate a signed URL for the TSV file.
    String tsvSignedUrl = helper.getStorageService().createSignedUrl(blobId.toGsUtilUri());
    allExportFileResults.add(ExportFileResult.forFile(fileName, tsvSignedUrl, null));

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

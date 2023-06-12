package bio.terra.tanagra.service.export.impl;

import bio.terra.tanagra.exception.SystemException;
import bio.terra.tanagra.service.export.DataExport;
import bio.terra.tanagra.service.export.DeploymentConfig;
import bio.terra.tanagra.service.export.ExportRequest;
import bio.terra.tanagra.service.export.ExportResult;
import bio.terra.tanagra.utils.GoogleCloudStorage;
import com.google.cloud.storage.BlobId;
import com.google.common.collect.ImmutableMap;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.text.StringSubstitutor;

public class VwbFileImport implements DataExport {
  private static final String FILE_FORMAT_SPECIFIER = "TsvHttpData-1.0";
  private GoogleCloudStorage storageService;
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
    storageService =
        GoogleCloudStorage.forApplicationDefaultCredentials(
            deploymentConfig.getShared().getGcpProjectId());
    gcsBucketNames = deploymentConfig.getShared().getGcsBucketNames();
    redirectAwayUrl = deploymentConfig.getRedirectAwayUrl();
  }

  @Override
  public ExportResult run(ExportRequest request) {
    // Write the data export files to GCS.
    Map<String, String> entityToGcsUrl =
        request.writeEntityDataToGcs("tanagra_${entity}_" + Instant.now() + "_*.csv");
    Map<String, String> cohortToGcsUrl =
        request.writeAnnotationDataToGcs("tanagra_${cohort}_" + Instant.now() + "_*.tsv");
    List<String> unsignedUrls = new ArrayList<>();
    unsignedUrls.addAll(entityToGcsUrl.values());
    unsignedUrls.addAll(cohortToGcsUrl.values());

    // Build a list of the TSV rows: signed url
    List<String> tsvRows =
        unsignedUrls.stream()
            .map(unsignedUrl -> storageService.createSignedUrl(unsignedUrl))
            .collect(Collectors.toList());

    // Sort the TSV rows lexicographically by signed URL.
    // Since the signed URL is the first column in each row, we can just sort the full TSV row
    // string.
    // Build a TSV-string from the sorted list of rows, prefixed with the format header.
    StringBuilder fileContents = new StringBuilder(FILE_FORMAT_SPECIFIER + "\n");
    tsvRows.stream().sorted().forEach(tsvRow -> fileContents.append(tsvRow + "\n"));

    // Write the TSV file to GCS. Just pick the first bucket name.
    BlobId blobId =
        storageService.writeFile(
            gcsBucketNames.get(0),
            "tanagra_export_" + Instant.now() + ".tsv",
            fileContents.toString());

    // Generate a signed URL for the TSV file.
    String tsvSignedUrl = storageService.createSignedUrl(blobId.toGsUtilUri());

    // Generate the redirect URL to VWB.
    Map<String, String> urlParams =
        ImmutableMap.<String, String>builder()
            .put("tsvFileUrl", urlEncode(tsvSignedUrl))
            .put("redirectBackUrl", urlEncode(request.getRedirectBackUrl()))
            .build();
    String expandedRedirectAwayUrl = StringSubstitutor.replace(redirectAwayUrl, urlParams);
    return ExportResult.forRedirectUrl(expandedRedirectAwayUrl, ExportResult.Status.COMPLETE);
  }

  private static String urlEncode(String param) {
    try {
      return URLEncoder.encode(param, StandardCharsets.UTF_8.toString());
    } catch (UnsupportedEncodingException ueEx) {
      throw new SystemException("Error encoding URL param: " + param, ueEx);
    }
  }
}

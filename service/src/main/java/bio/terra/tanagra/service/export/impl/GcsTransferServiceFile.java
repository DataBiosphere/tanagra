package bio.terra.tanagra.service.export.impl;

import bio.terra.tanagra.exception.SystemException;
import bio.terra.tanagra.service.export.DataExport;
import bio.terra.tanagra.service.export.ExportRequest;
import bio.terra.tanagra.service.export.ExportResult;
import bio.terra.tanagra.utils.GoogleCloudStorage;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class GcsTransferServiceFile implements DataExport {
  private static final String FILE_FORMAT_SPECIFIER = "TsvHttpData-1.0";
  private static final long DEFAULT_SIGNED_URL_DURATION = 30;
  private static final TimeUnit DEFAULT_SIGNED_URL_UNIT = TimeUnit.MINUTES;
  private GoogleCloudStorage storageService;
  private List<String> gcsBucketNames;

  @Override
  public String getDescription() {
    return "Signed URL to a file that contains a list of signed URLs. Each URL in the file points to a single file with either query results or annotation data.";
  }

  @Override
  public void initialize(CommonInfrastructure commonInfrastructure, List<String> params) {
    storageService =
        GoogleCloudStorage.forApplicationDefaultCredentials(commonInfrastructure.getGcpProjectId());
    gcsBucketNames = commonInfrastructure.getGcsBucketNames();
    // Parameters are currently ignored. Possible future uses: expiry time for signed url, filename
    // template, csv/tsv/etc. format
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

    // Build a list of the TSV rows: signed url -> "signedUrl \t size \t checksum"
    List<String> tsvRows =
        unsignedUrls.stream()
            .map(
                unsignedUrl -> {
                  String signedUrl =
                      storageService.createSignedUrl(
                          unsignedUrl, DEFAULT_SIGNED_URL_DURATION, DEFAULT_SIGNED_URL_UNIT);

                  Optional<Blob> blob = storageService.getBlob(unsignedUrl);
                  if (blob.isEmpty()) {
                    throw new SystemException("Blob not found");
                  }
                  long fileSizeBytes = blob.get().getSize();
                  String base64MD5Checksum = blob.get().getMd5();

                  return signedUrl + "\t" + fileSizeBytes + "\t" + base64MD5Checksum;
                })
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

    Map<String, String> outputParams = Map.of("tsvFile", blobId.toGsUtilUri());
    return new ExportResult(outputParams, ExportResult.Status.COMPLETE);
  }
}

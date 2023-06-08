package bio.terra.tanagra.service.export.impl;

import bio.terra.tanagra.service.export.DataExport;
import bio.terra.tanagra.service.export.ExportRequest;
import bio.terra.tanagra.service.export.ExportResult;
import bio.terra.tanagra.utils.GoogleCloudStorage;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class ListOfSignedUrls implements DataExport {
  private static final long DEFAULT_SIGNED_URL_DURATION = 30;
  private static final TimeUnit DEFAULT_SIGNED_URL_UNIT = TimeUnit.MINUTES;
  private GoogleCloudStorage storageService;

  @Override
  public String getDescription() {
    return "List of signed URLs. Each URL points to a single file with either query results or annotation data.";
  }

  @Override
  public void initialize(CommonInfrastructure commonInfrastructure, List<String> params) {
    storageService =
        GoogleCloudStorage.forApplicationDefaultCredentials(commonInfrastructure.getGcpProjectId());
    // Parameters are currently ignored. Possible future uses: expiry time for signed url, filename
    // template, csv/tsv/etc. format
  }

  @Override
  public ExportResult run(ExportRequest request) {
    Map<String, String> entityToGcsUrl =
        request.writeEntityDataToGcs("tanagra_${entity}_" + Instant.now() + "_*.csv");
    Map<String, String> cohortToGcsUrl =
        request.writeAnnotationDataToGcs("tanagra_${cohort}_" + Instant.now() + "_*.tsv");

    Map<String, String> outputParams = new HashMap<>();
    entityToGcsUrl.entrySet().stream()
        .forEach(
            entry ->
                outputParams.put(
                    "entity:" + entry.getKey(),
                    storageService.createSignedUrl(
                        entry.getValue(), DEFAULT_SIGNED_URL_DURATION, DEFAULT_SIGNED_URL_UNIT)));
    cohortToGcsUrl.entrySet().stream()
        .forEach(
            entry ->
                outputParams.put(
                    "cohort:" + entry.getKey(),
                    storageService.createSignedUrl(
                        entry.getValue(), DEFAULT_SIGNED_URL_DURATION, DEFAULT_SIGNED_URL_UNIT)));
    return new ExportResult(outputParams, ExportResult.Status.COMPLETE);
  }
}

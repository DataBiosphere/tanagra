package bio.terra.tanagra.service.export.impl;

import bio.terra.tanagra.service.artifact.model.Cohort;
import bio.terra.tanagra.service.export.DataExport;
import bio.terra.tanagra.service.export.DeploymentConfig;
import bio.terra.tanagra.service.export.ExportRequest;
import bio.terra.tanagra.service.export.ExportResult;
import java.time.Instant;
import java.util.*;

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
  public ExportResult run(ExportRequest request) {
    Map<String, String> entityToGcsUrl =
        request.writeEntityDataToGcs("tanagra_${entity}_" + Instant.now() + "_*.csv");
    Map<Cohort, String> cohortToGcsUrl =
        request.writeAnnotationDataToGcs("tanagra_${cohort}_" + Instant.now() + "_*.tsv");

    Map<String, String> outputParams = new HashMap<>();
    entityToGcsUrl.entrySet().stream()
        .forEach(
            entry ->
                outputParams.put(
                    ENTITY_OUTPUT_KEY_PREFIX + entry.getKey(),
                    request.getGoogleCloudStorage().createSignedUrl(entry.getValue())));
    cohortToGcsUrl.entrySet().stream()
        .forEach(
            entry -> {
              String cohortName = entry.getKey().getDisplayName();
              if (cohortName == null || cohortName.isEmpty()) {
                cohortName = entry.getKey().getId();
              }
              outputParams.put(
                  COHORT_OUTPUT_KEY_PREFIX + cohortName,
                  request.getGoogleCloudStorage().createSignedUrl(entry.getValue()));
            });
    return ExportResult.forOutputParams(outputParams, ExportResult.Status.COMPLETE);
  }
}

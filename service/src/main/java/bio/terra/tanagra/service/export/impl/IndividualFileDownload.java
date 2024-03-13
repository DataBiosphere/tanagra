package bio.terra.tanagra.service.export.impl;

import static bio.terra.tanagra.utils.NameUtils.simplifyStringForName;

import bio.terra.tanagra.service.artifact.model.Cohort;
import bio.terra.tanagra.service.export.DataExport;
import bio.terra.tanagra.service.export.DeploymentConfig;
import bio.terra.tanagra.service.export.ExportRequest;
import bio.terra.tanagra.service.export.ExportResult;
import java.util.HashMap;
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
  public ExportResult run(ExportRequest request) {
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
    Map<String, String> entityToGcsUrl =
        request.writeEntityDataToGcs(
            "${entity}_cohort" + cohortRef + "_" + studyUnderlayRef + "_${random}");
    Map<Cohort, String> cohortToGcsUrl =
        request.writeAnnotationDataToGcs(
            "annotations_cohort${cohort}" + "_" + studyUnderlayRef + "_${random}");

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

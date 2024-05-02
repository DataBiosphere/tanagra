package bio.terra.tanagra.service.export.impl;

import static bio.terra.tanagra.utils.NameUtils.simplifyStringForName;

import bio.terra.tanagra.proto.regressiontest.RTCohort;
import bio.terra.tanagra.proto.regressiontest.RTCriteria;
import bio.terra.tanagra.proto.regressiontest.RTDataFeatureSet;
import bio.terra.tanagra.proto.regressiontest.RTExportCounts;
import bio.terra.tanagra.service.artifact.model.Cohort;
import bio.terra.tanagra.service.artifact.model.CohortRevision;
import bio.terra.tanagra.service.artifact.model.ConceptSet;
import bio.terra.tanagra.service.artifact.model.Criteria;
import bio.terra.tanagra.service.export.DataExport;
import bio.terra.tanagra.service.export.DataExportHelper;
import bio.terra.tanagra.service.export.DeploymentConfig;
import bio.terra.tanagra.service.export.ExportFileResult;
import bio.terra.tanagra.service.export.ExportRequest;
import bio.terra.tanagra.service.export.ExportResult;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.entitymodel.Attribute;
import bio.terra.tanagra.underlay.entitymodel.Entity;
import bio.terra.tanagra.utils.ProtobufUtils;
import com.google.cloud.storage.BlobId;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@SuppressWarnings("PMD.TestClassWithoutTestCases")
public class RegressionTest implements DataExport {
  private List<String> gcsBucketNames;

  @Override
  public Type getType() {
    return Type.REGRESSION_TEST;
  }

  @Override
  public String getDefaultDisplayName() {
    return "Regression test file";
  }

  @Override
  public String getDescription() {
    return "Signed URL to a regression test file that includes the total number of rows returned for each output entity.";
  }

  @Override
  public void initialize(DeploymentConfig deploymentConfig) {
    gcsBucketNames = deploymentConfig.getShared().getGcsBucketNames();
  }

  @Override
  public ExportResult run(ExportRequest request, DataExportHelper helper) {
    // Build the export counts proto object.
    RTExportCounts.ExportCounts.Builder exportCounts =
        RTExportCounts.ExportCounts.newBuilder().setUnderlay(request.getUnderlay().getName());
    request.getCohorts().forEach(cohort -> exportCounts.addCohorts(toRegressionTestObj(cohort)));
    request
        .getConceptSets()
        .forEach(
            conceptSet ->
                exportCounts.addDataFeatureSets(
                    toRegressionTestObj(conceptSet, request.getUnderlay())));

    // Get the counts for each output entity.
    Map<String, Long> totalNumRowsPerEntity = helper.getTotalNumRowsOfEntityData();
    totalNumRowsPerEntity.forEach(
        (key, value) -> exportCounts.addEntityOutputCounts(toRegressionTestObj(key, value)));

    // Write the proto object in JSON format to a GCS file. Just pick the first bucket name.
    String cohortRef =
        simplifyStringForName(request.getCohorts().get(0).getDisplayName())
            + (request.getCohorts().size() > 1
                ? "_plus" + (request.getCohorts().size() - 1) + "more"
                : "");
    String dataFeatureSetRef =
        request.getConceptSets().isEmpty()
            ? ""
            : simplifyStringForName(request.getConceptSets().get(0).getDisplayName())
                + (request.getConceptSets().size() > 1
                    ? "_plus" + (request.getConceptSets().size() - 1) + "more"
                    : "");
    String fileName = "cohort" + cohortRef + "_datafeatureset" + dataFeatureSetRef + ".json";
    String fileContents = ProtobufUtils.serializeToPrettyJson(exportCounts.build());
    BlobId blobId =
        helper.getStorageService().writeFile(gcsBucketNames.get(0), fileName, fileContents);

    // Generate a signed URL for the JSON file.
    String jsonSignedUrl = helper.getStorageService().createSignedUrl(blobId.toGsUtilUri());

    ExportFileResult exportFileResult =
        ExportFileResult.forFile(fileName, jsonSignedUrl, null, null);
    exportFileResult.addTags(List.of("Regression Test File"));
    return ExportResult.forFileResults(List.of(exportFileResult));
  }

  private static RTExportCounts.EntityOutputCount toRegressionTestObj(
      String entityName, Long numRows) {
    return RTExportCounts.EntityOutputCount.newBuilder()
        .setEntity(entityName)
        .setNumRows(numRows)
        .build();
  }

  private static RTDataFeatureSet.DataFeatureSet toRegressionTestObj(
      ConceptSet conceptSet, Underlay underlay) {
    return RTDataFeatureSet.DataFeatureSet.newBuilder()
        .setDisplayName(conceptSet.getDisplayName())
        .addAllCriteria(
            conceptSet.getCriteria().stream()
                .map(RegressionTest::toRegressionTestObj)
                .collect(Collectors.toList()))
        .addAllEntityOutputs(
            conceptSet.getExcludeOutputAttributesPerEntity().entrySet().stream()
                .map(
                    entry ->
                        toRegressionTestObj(underlay.getEntity(entry.getKey()), entry.getValue()))
                .collect(Collectors.toList()))
        .build();
  }

  private static RTDataFeatureSet.EntityOutput toRegressionTestObj(
      Entity entity, List<String> excludedAttributes) {
    List<String> includedAttributes =
        entity.getAttributes().stream()
            .map(Attribute::getName)
            .filter(name -> !excludedAttributes.contains(name))
            .collect(Collectors.toList());
    return RTDataFeatureSet.EntityOutput.newBuilder()
        .setEntity(entity.getName())
        .addAllIncludedAttributes(includedAttributes)
        .build();
  }

  private static RTCohort.Cohort toRegressionTestObj(Cohort cohort) {
    return RTCohort.Cohort.newBuilder()
        .setDisplayName(cohort.getDisplayName())
        .addAllCriteriaGroupSections(
            cohort.getMostRecentRevision().getSections().stream()
                .map(RegressionTest::toRegressionTestObj)
                .collect(Collectors.toList()))
        .build();
  }

  private static RTCriteria.CriteriaGroupSection toRegressionTestObj(
      CohortRevision.CriteriaGroupSection criteriaGroupSection) {
    return RTCriteria.CriteriaGroupSection.newBuilder()
        .addAllCriteriaGroups(
            criteriaGroupSection.getCriteriaGroups().stream()
                .map(RegressionTest::toRegressionTestObj)
                .collect(Collectors.toList()))
        .setOperator(
            RTCriteria.BooleanLogicOperator.valueOf(criteriaGroupSection.getOperator().name()))
        .setIsExcluded(criteriaGroupSection.isExcluded())
        .build();
  }

  private static RTCriteria.CriteriaGroup toRegressionTestObj(
      CohortRevision.CriteriaGroup criteriaGroup) {
    return RTCriteria.CriteriaGroup.newBuilder()
        .addAllCriteria(
            criteriaGroup.getCriteria().stream()
                .map(RegressionTest::toRegressionTestObj)
                .collect(Collectors.toList()))
        .build();
  }

  private static RTCriteria.Criteria toRegressionTestObj(Criteria criteria) {
    RTCriteria.Criteria.Builder builder =
        RTCriteria.Criteria.newBuilder().setPluginName(criteria.getPluginName());
    if (criteria.getPredefinedId() != null) {
      builder.setPredefinedId(criteria.getPredefinedId());
    } else {
      builder
          .setSelectorOrModifierName(criteria.getSelectorOrModifierName())
          .setPluginVersion(criteria.getPluginVersion())
          .setPluginConfig(criteria.getUiConfig());
    }

    if (criteria.getSelectionData() != null) {
      builder.setSelectionData(criteria.getSelectionData());
    }
    return builder.build();
  }
}

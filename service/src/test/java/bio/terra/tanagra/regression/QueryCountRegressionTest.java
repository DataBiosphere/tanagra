package bio.terra.tanagra.regression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import bio.terra.tanagra.api.filter.BooleanAndOrFilter;
import bio.terra.tanagra.proto.regressiontest.RTCriteria;
import bio.terra.tanagra.proto.regressiontest.RTDataFeatureSet;
import bio.terra.tanagra.proto.regressiontest.RTExportCounts;
import bio.terra.tanagra.service.UnderlayService;
import bio.terra.tanagra.service.artifact.CohortService;
import bio.terra.tanagra.service.artifact.FeatureSetService;
import bio.terra.tanagra.service.artifact.StudyService;
import bio.terra.tanagra.service.artifact.model.Cohort;
import bio.terra.tanagra.service.artifact.model.CohortRevision;
import bio.terra.tanagra.service.artifact.model.Criteria;
import bio.terra.tanagra.service.artifact.model.FeatureSet;
import bio.terra.tanagra.service.artifact.model.Study;
import bio.terra.tanagra.service.export.DataExportHelper;
import bio.terra.tanagra.service.export.DataExportService;
import bio.terra.tanagra.service.export.ExportRequest;
import bio.terra.tanagra.testing.BaseSpringUnitTest;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.entitymodel.Attribute;
import bio.terra.tanagra.underlay.entitymodel.Entity;
import bio.terra.tanagra.utils.FileUtils;
import bio.terra.tanagra.utils.ProtobufUtils;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

@Tag("requires-cloud-access")
@Tag("regression-test")
public class QueryCountRegressionTest extends BaseSpringUnitTest {
  private static final Logger LOGGER = LoggerFactory.getLogger(QueryCountRegressionTest.class);
  private static final String USER_EMAIL_1 = "abc@123.com";

  @Autowired private UnderlayService underlayService;
  @Autowired private StudyService studyService;
  @Autowired private FeatureSetService featureSetService;
  @Autowired private CohortService cohortService;
  @Autowired private DataExportService dataExportService;

  private Study study1;

  @BeforeEach
  void createStudy() {
    study1 = studyService.createStudy(Study.builder().displayName("study 1"), USER_EMAIL_1);
    assertNotNull(study1);
    LOGGER.info("Created study1 {} at {}", study1.getId(), study1.getCreated());
  }

  @AfterEach
  void deleteStudy() {
    try {
      studyService.deleteStudy(study1.getId(), USER_EMAIL_1);
      LOGGER.info("Deleted study1 {}", study1.getId());
    } catch (Exception ex) {
      LOGGER.error("Error deleting study1", ex);
    }
  }

  @ParameterizedTest(name = "{index}: {0} counts match")
  @MethodSource("getTestFilePaths")
  void countsMatch(Path filePath) throws IOException {
    // Read in the exported regression file and deserialize from JSON.
    LOGGER.info("Testing regression file: {}", filePath.getFileName());
    String fileContents = FileUtils.readStringFromFile(FileUtils.getFileStream(filePath));
    RTExportCounts.ExportCounts rtExportCounts =
        ProtobufUtils.deserializeFromJsonOrProtoBytes(
                fileContents, RTExportCounts.ExportCounts.newBuilder())
            .build();

    // Create cohorts and data feature sets.
    LOGGER.info("Testing against underlay: {}", rtExportCounts.getUnderlay());
    Underlay underlay = underlayService.getUnderlay(rtExportCounts.getUnderlay());
    List<Cohort> cohorts = new ArrayList<>();
    rtExportCounts
        .getCohortsList()
        .forEach(
            rtCohort -> {
              Cohort cohort =
                  cohortService.createCohort(
                      study1.getId(),
                      Cohort.builder()
                          .underlay(rtExportCounts.getUnderlay())
                          .displayName(
                              rtCohort
                                  .getDisplayName()
                                  .substring(0, Math.min(rtCohort.getDisplayName().length(), 50)))
                          .description(rtCohort.getDisplayName())
                          .createdBy(USER_EMAIL_1),
                      rtCohort.getCriteriaGroupSectionsList().stream()
                          .map(QueryCountRegressionTest::fromRegressionTestObj)
                          .toList());
              assertNotNull(cohort);
              LOGGER.info("Created cohort {} at {}", cohort.getId(), cohort.getCreated());
              cohorts.add(cohort);
            });
    List<FeatureSet> featureSets = new ArrayList<>();
    rtExportCounts
        .getDataFeatureSetsList()
        .forEach(
            rtDataFeatureSet -> {
              FeatureSet featureSet =
                  featureSetService.createFeatureSet(
                      study1.getId(),
                      FeatureSet.builder()
                          .underlay(rtExportCounts.getUnderlay())
                          .displayName(
                              rtDataFeatureSet
                                  .getDisplayName()
                                  .substring(
                                      0, Math.min(rtDataFeatureSet.getDisplayName().length(), 50)))
                          .description(rtDataFeatureSet.getDisplayName())
                          .criteria(
                              rtDataFeatureSet.getCriteriaList().stream()
                                  .map(QueryCountRegressionTest::fromRegressionTestObj)
                                  .toList())
                          .excludeOutputAttributesPerEntity(
                              fromRegressionTestObj(
                                  underlay, rtDataFeatureSet.getEntityOutputsList())),
                      USER_EMAIL_1);
              assertNotNull(featureSet);
              LOGGER.info(
                  "Created data feature set {} at {}", featureSet.getId(), featureSet.getCreated());
              featureSets.add(featureSet);
            });

    // Call the regression test export model to compute the counts for all output entities.
    ExportRequest exportRequest =
        new ExportRequest(
            "REGRESSION_TEST",
            Map.of(),
            null,
            false,
            USER_EMAIL_1,
            underlay,
            study1,
            cohorts,
            featureSets);
    DataExportHelper dataExportHelper = dataExportService.buildHelper(exportRequest);
    Map<String, Long> totalNumRowsPerEntity = dataExportHelper.getTotalNumRowsOfEntityData();

    // Compare the counts with those saved in the exported regression file.
    Map<String, Long> expectedTotalNumRowsPerEntity =
        fromRegressionTestObj(rtExportCounts.getEntityOutputCountsList());
    totalNumRowsPerEntity.forEach(
        (entityName, totalNumRows) -> {
          LOGGER.info("entity={}, actual count={}", entityName, totalNumRows);
          assertEquals(expectedTotalNumRowsPerEntity.get(entityName), totalNumRows);
        });
  }

  @SuppressWarnings("PMD.UnusedPrivateMethod")
  private static Stream<String> getTestFilePaths() throws FileNotFoundException {
    String regressionTestDirsParam = System.getProperty("REGRESSION_TEST_DIRS");
    String regressionTestUnderlaysParam = System.getProperty("REGRESSION_TEST_UNDERLAYS");
    String regressionTestFilesParam = System.getProperty("REGRESSION_TEST_FILES");
    String gradleProjectDir = System.getProperty("GRADLE_PROJECT_DIR");
    LOGGER.info("REGRESSION_TEST_DIRS = {}", regressionTestDirsParam);
    LOGGER.info("REGRESSION_TEST_UNDERLAYS = {}", regressionTestUnderlaysParam);
    LOGGER.info("REGRESSION_TEST_FILES = {}", regressionTestFilesParam);
    LOGGER.info("GRADLE_PROJECT_DIR = {}", gradleProjectDir);

    List<Path> regressionTestDirs = new ArrayList<>();
    if (regressionTestDirsParam != null && !regressionTestDirsParam.isEmpty()) {
      List.of(regressionTestDirsParam.split(","))
          .forEach(dirName -> regressionTestDirs.add(Path.of(dirName)));
    } else if (regressionTestUnderlaysParam != null && !regressionTestUnderlaysParam.isEmpty()) {
      List<String> underlaySubDirs = List.of(regressionTestUnderlaysParam.split(","));
      Path regressionParentDir =
          Path.of(gradleProjectDir).resolve("src/test/resources/regression/");
      underlaySubDirs.forEach(
          underlayName -> regressionTestDirs.add(regressionParentDir.resolve(underlayName)));
    } else {
      throw new IllegalArgumentException(
          "No test directories or underlays specified. Use Gradle properties: -PregressionTestDirs for a directory, -PregressionTestUnderlays for an underlay-specific directory in the service/test/resources/regression sub-directory.");
    }

    List<String> regressionTestFileNameFilters = new ArrayList<>();
    if (regressionTestFilesParam != null && !regressionTestFilesParam.isEmpty()) {
      regressionTestFileNameFilters.addAll(List.of(regressionTestFilesParam.split(",")));
    }

    List<String> selectedRegressionTestFiles = new ArrayList<>();
    regressionTestDirs.forEach(
        regressionTestDir -> {
          LOGGER.info(
              "Searching directory for regression test files: {}",
              regressionTestDir.toAbsolutePath());
          File[] testFiles = regressionTestDir.toFile().listFiles();
          if (testFiles != null && testFiles.length > 0) {
            Stream.of(testFiles)
                .filter(
                    testFile ->
                        regressionTestFileNameFilters.isEmpty()
                            || regressionTestFileNameFilters.contains(
                                testFile.toPath().getFileName().toString()))
                .forEach(
                    testFile ->
                        selectedRegressionTestFiles.add(
                            testFile.toPath().toAbsolutePath().toString()));
          }
        });
    if (selectedRegressionTestFiles.isEmpty()) {
      throw new FileNotFoundException(
          "No regression test files found: "
              + regressionTestDirs.stream()
                  .map(path -> path.toAbsolutePath().toString())
                  .collect(Collectors.joining(",")));
    }
    return selectedRegressionTestFiles.stream();
  }

  private static Map<String, Long> fromRegressionTestObj(
      List<RTExportCounts.EntityOutputCount> rtObj) {
    Map<String, Long> totalNumRowsPerEntity = new HashMap<>();
    rtObj.forEach(
        rtEntityOutputCount ->
            totalNumRowsPerEntity.put(
                rtEntityOutputCount.getEntity(), rtEntityOutputCount.getNumRows()));
    return totalNumRowsPerEntity;
  }

  private static Map<String, List<String>> fromRegressionTestObj(
      Underlay underlay, List<RTDataFeatureSet.EntityOutput> rtObj) {
    Map<String, List<String>> excludedAttributesPerEntity = new HashMap<>();
    rtObj.forEach(
        rtEntityOutput -> {
          Entity entity = underlay.getEntity(rtEntityOutput.getEntity());
          List<String> excludedAttributes =
              entity.getAttributes().stream()
                  .map(Attribute::getName)
                  .filter(name -> !rtEntityOutput.getIncludedAttributesList().contains(name))
                  .toList();
          excludedAttributesPerEntity.put(entity.getName(), excludedAttributes);
        });
    return excludedAttributesPerEntity;
  }

  private static CohortRevision.CriteriaGroupSection fromRegressionTestObj(
      RTCriteria.CriteriaGroupSection rtObj) {
    return CohortRevision.CriteriaGroupSection.builder()
        .criteriaGroups(
            rtObj.getCriteriaGroupsList().stream()
                .map(QueryCountRegressionTest::fromRegressionTestObj)
                .toList())
        .operator(BooleanAndOrFilter.LogicalOperator.valueOf(rtObj.getOperator().name()))
        .setIsExcluded(rtObj.getIsExcluded())
        .build();
  }

  private static CohortRevision.CriteriaGroup fromRegressionTestObj(
      RTCriteria.CriteriaGroup rtObj) {
    return CohortRevision.CriteriaGroup.builder()
        .criteria(
            rtObj.getCriteriaList().stream()
                .map(QueryCountRegressionTest::fromRegressionTestObj)
                .toList())
        .build();
  }

  private static Criteria fromRegressionTestObj(RTCriteria.Criteria rtObj) {
    return Criteria.builder()
        .predefinedId(rtObj.getPredefinedId())
        .selectorOrModifierName(rtObj.getSelectorOrModifierName())
        .selectionData(rtObj.getSelectionData())
        .pluginVersion(rtObj.getPluginVersion())
        .uiConfig(rtObj.getPluginConfig())
        .pluginName(rtObj.getPluginName())
        .build();
  }
}

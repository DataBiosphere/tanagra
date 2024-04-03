package bio.terra.tanagra.regression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bio.terra.tanagra.api.filter.BooleanAndOrFilter;
import bio.terra.tanagra.app.Main;
import bio.terra.tanagra.app.configuration.FeatureConfiguration;
import bio.terra.tanagra.proto.regressiontest.RTCriteria;
import bio.terra.tanagra.proto.regressiontest.RTDataFeatureSet;
import bio.terra.tanagra.proto.regressiontest.RTExportCounts;
import bio.terra.tanagra.service.UnderlayService;
import bio.terra.tanagra.service.artifact.CohortService;
import bio.terra.tanagra.service.artifact.ConceptSetService;
import bio.terra.tanagra.service.artifact.StudyService;
import bio.terra.tanagra.service.artifact.model.Cohort;
import bio.terra.tanagra.service.artifact.model.CohortRevision;
import bio.terra.tanagra.service.artifact.model.ConceptSet;
import bio.terra.tanagra.service.artifact.model.Criteria;
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
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = Main.class)
@SpringBootTest
@ActiveProfiles("test")
@Tag("requires-cloud-access")
@Tag("regression-test")
public class QueryCountRegressionTest extends BaseSpringUnitTest {
  private static final Logger LOGGER = LoggerFactory.getLogger(QueryCountRegressionTest.class);
  private static final List<String> UNDERLAYS_TESTED_BY_DEFAULT =
      List.of("cmssynpuf", "aouSR2019q4r4");

  @Autowired private FeatureConfiguration featureConfiguration;
  @Autowired private UnderlayService underlayService;
  @Autowired private StudyService studyService;
  @Autowired private ConceptSetService conceptSetService;
  @Autowired private CohortService cohortService;
  @Autowired private DataExportService dataExportService;

  private Study study1;

  @BeforeEach
  void createStudy() {
    study1 = studyService.createStudy(Study.builder().displayName("study 1"), "abc@123.com");
    assertNotNull(study1);
    LOGGER.info("Created study1 {} at {}", study1.getId(), study1.getCreated());
  }

  @AfterEach
  void deleteStudy() {
    try {
      studyService.deleteStudy(study1.getId(), "abc@123.com");
      LOGGER.info("Deleted study1 {}", study1.getId());
    } catch (Exception ex) {
      LOGGER.error("Error deleting study1", ex);
    }
  }

  @ParameterizedTest(name = "{index}: {0} counts match")
  @MethodSource("getTestFilePaths")
  void countsMatch(Path filePath) throws IOException {
    // Read in the exported regression file and deserialize from JSON.
    String fileContents = FileUtils.readStringFromFile(FileUtils.getFileStream(filePath));
    RTExportCounts.ExportCounts rtExportCounts =
        ProtobufUtils.deserializeFromJsonOrProtoBytes(
                fileContents, RTExportCounts.ExportCounts.newBuilder())
            .build();

    // Create cohorts and data feature sets.
    Underlay underlay = underlayService.getUnderlay(rtExportCounts.getUnderlay());
    List<Cohort> cohorts = new ArrayList<>();
    rtExportCounts.getCohortsList().stream()
        .forEach(
            rtCohort -> {
              Cohort cohort =
                  cohortService.createCohort(
                      study1.getId(),
                      Cohort.builder()
                          .underlay(rtExportCounts.getUnderlay())
                          .displayName(rtCohort.getDisplayName()),
                      "abc@123.com",
                      rtCohort.getCriteriaGroupSectionsList().stream()
                          .map(QueryCountRegressionTest::fromRegressionTestObj)
                          .collect(Collectors.toList()));
              assertNotNull(cohort);
              LOGGER.info("Created cohort {} at {}", cohort.getId(), cohort.getCreated());
              cohorts.add(cohort);
            });
    List<ConceptSet> conceptSets = new ArrayList<>();
    rtExportCounts.getDataFeatureSetsList().stream()
        .forEach(
            rtDataFeatureSet -> {
              ConceptSet conceptSet =
                  conceptSetService.createConceptSet(
                      study1.getId(),
                      ConceptSet.builder()
                          .underlay(rtExportCounts.getUnderlay())
                          .displayName(rtDataFeatureSet.getDisplayName())
                          .criteria(
                              rtDataFeatureSet.getCriteriaList().stream()
                                  .map(QueryCountRegressionTest::fromRegressionTestObj)
                                  .collect(Collectors.toList()))
                          .excludeOutputAttributesPerEntity(
                              fromRegressionTestObj(
                                  underlay, rtDataFeatureSet.getEntityOutputsList())),
                      "abc@123.com");
              assertNotNull(conceptSet);
              LOGGER.info(
                  "Created data feature set {} at {}", conceptSet.getId(), conceptSet.getCreated());
              conceptSets.add(conceptSet);
            });

    // Call the regression test export model to compute the counts for all output entities.
    ExportRequest exportRequest =
        new ExportRequest(
            "REGRESSION_TEST",
            Map.of(),
            null,
            false,
            "abc@123.com",
            underlay,
            study1,
            cohorts,
            conceptSets);
    assertTrue(featureConfiguration.isBackendFiltersEnabled());
    DataExportHelper dataExportHelper = dataExportService.buildHelper(exportRequest, null, null);
    Map<String, Long> totalNumRowsPerEntity = dataExportHelper.getTotalNumRowsOfEntityData();

    // Compare the counts with those saved in the exported regression file.
    Map<String, Long> expectedTotalNumRowsPerEntity =
        fromRegressionTestObj(rtExportCounts.getEntityOutputCountsList());
    totalNumRowsPerEntity.entrySet().stream()
        .forEach(
            entry -> {
              String entityName = entry.getKey();
              Long totalNumRows = entry.getValue();
              assertEquals(expectedTotalNumRowsPerEntity.get(entityName), totalNumRows);
            });
  }

  private static Map<String, Long> fromRegressionTestObj(
      List<RTExportCounts.EntityOutputCount> rtObj) {
    Map<String, Long> totalNumRowsPerEntity = new HashMap<>();
    rtObj.stream()
        .forEach(
            rtEntityOutputCount ->
                totalNumRowsPerEntity.put(
                    rtEntityOutputCount.getEntity(), rtEntityOutputCount.getNumRows()));
    return totalNumRowsPerEntity;
  }

  private static Map<String, List<String>> fromRegressionTestObj(
      Underlay underlay, List<RTDataFeatureSet.EntityOutput> rtObj) {
    Map<String, List<String>> excludedAttributesPerEntity = new HashMap<>();
    rtObj.stream()
        .forEach(
            rtEntityOutput -> {
              Entity entity = underlay.getEntity(rtEntityOutput.getEntity());
              List<String> excludedAttributes =
                  entity.getAttributes().stream()
                      .filter(
                          attribute ->
                              !rtEntityOutput
                                  .getIncludedAttributesList()
                                  .contains(attribute.getName()))
                      .map(Attribute::getName)
                      .collect(Collectors.toList());
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
                .collect(Collectors.toList()))
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
                .collect(Collectors.toList()))
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

  @SuppressWarnings("PMD.UnusedPrivateMethod")
  private static Stream<Path> getTestFilePaths() {
    List<Path> regressionTestDirs = new ArrayList<>();
    String specifiedDirs = System.getProperty("QUERY_COUNT_REGRESSION_TEST_DIRS");
    if (specifiedDirs != null && !specifiedDirs.isEmpty()) {
      List.of(specifiedDirs.split(",")).stream()
          .forEach(dirName -> regressionTestDirs.add(Path.of(dirName)));
    } else {
      String gradleProjectDir = System.getProperty("GRADLE_PROJECT_DIR");
      Path regressionParentDir =
          Path.of(gradleProjectDir).resolve("src/test/resources/regression/");
      UNDERLAYS_TESTED_BY_DEFAULT.stream()
          .forEach(
              underlayName -> regressionTestDirs.add(regressionParentDir.resolve(underlayName)));
    }

    List<Path> regressionTestFiles = new ArrayList<>();
    regressionTestDirs.stream()
        .forEach(
            regressionTestDir -> {
              File[] testFiles = regressionTestDir.toFile().listFiles();
              if (testFiles.length > 0) {
                List.of(testFiles).stream()
                    .forEach(testFile -> regressionTestFiles.add(testFile.toPath()));
              }
            });
    return regressionTestFiles.stream();
  }
}

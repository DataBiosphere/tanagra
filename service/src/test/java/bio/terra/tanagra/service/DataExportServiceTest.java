package bio.terra.tanagra.service;

import static bio.terra.tanagra.service.criteriaconstants.cmssynpuf.Criteria.DEMOGRAPHICS_PREPACKAGED_DATA_FEATURE;
import static bio.terra.tanagra.service.criteriaconstants.cmssynpuf.Criteria.ICD9CM_EQ_DIABETES;
import static bio.terra.tanagra.service.criteriaconstants.cmssynpuf.CriteriaGroupSection.CRITERIA_GROUP_SECTION_AGE;
import static bio.terra.tanagra.service.criteriaconstants.cmssynpuf.CriteriaGroupSection.CRITERIA_GROUP_SECTION_GENDER;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bio.terra.tanagra.api.field.AttributeField;
import bio.terra.tanagra.api.field.ValueDisplayField;
import bio.terra.tanagra.api.filter.AttributeFilter;
import bio.terra.tanagra.api.filter.EntityFilter;
import bio.terra.tanagra.api.query.list.ListQueryRequest;
import bio.terra.tanagra.api.shared.BinaryOperator;
import bio.terra.tanagra.api.shared.DataType;
import bio.terra.tanagra.api.shared.Literal;
import bio.terra.tanagra.api.shared.OrderByDirection;
import bio.terra.tanagra.app.Main;
import bio.terra.tanagra.service.artifact.AnnotationService;
import bio.terra.tanagra.service.artifact.CohortService;
import bio.terra.tanagra.service.artifact.FeatureSetService;
import bio.terra.tanagra.service.artifact.ReviewService;
import bio.terra.tanagra.service.artifact.StudyService;
import bio.terra.tanagra.service.artifact.model.AnnotationKey;
import bio.terra.tanagra.service.artifact.model.Cohort;
import bio.terra.tanagra.service.artifact.model.FeatureSet;
import bio.terra.tanagra.service.artifact.model.Review;
import bio.terra.tanagra.service.artifact.model.Study;
import bio.terra.tanagra.service.artifact.reviewquery.ReviewQueryOrderBy;
import bio.terra.tanagra.service.artifact.reviewquery.ReviewQueryRequest;
import bio.terra.tanagra.service.artifact.reviewquery.ReviewQueryResult;
import bio.terra.tanagra.service.export.DataExport;
import bio.terra.tanagra.service.export.DataExportModel;
import bio.terra.tanagra.service.export.DataExportService;
import bio.terra.tanagra.service.export.ExportFileResult;
import bio.terra.tanagra.service.export.ExportRequest;
import bio.terra.tanagra.service.export.ExportResult;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.entitymodel.Attribute;
import bio.terra.tanagra.underlay.entitymodel.Entity;
import bio.terra.tanagra.utils.GoogleCloudStorage;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
public class DataExportServiceTest {
  private static final Logger LOGGER = LoggerFactory.getLogger(DataExportServiceTest.class);
  private static final String UNDERLAY_NAME = "cmssynpuf";
  @Autowired private UnderlayService underlayService;

  @Autowired private StudyService studyService;
  @Autowired private CohortService cohortService;
  @Autowired private FeatureSetService featureSetService;
  @Autowired private AnnotationService annotationService;
  @Autowired private ReviewService reviewService;
  @Autowired private DataExportService dataExportService;

  private Study study1;
  private Cohort cohort1;
  private Cohort cohort2;
  private FeatureSet featureSet1;
  private FeatureSet featureSet2;

  @BeforeEach
  void createAnnotationValues() {
    // Build a study, feature set, cohort, review, and annotation data.
    String userEmail = "abc@123.com";

    study1 = studyService.createStudy(Study.builder(), userEmail);
    assertNotNull(study1);
    LOGGER.info("Created study1 {} at {}", study1.getId(), study1.getCreated());

    featureSet1 =
        featureSetService.createFeatureSet(
            study1.getId(),
            FeatureSet.builder()
                .underlay(UNDERLAY_NAME)
                .displayName("First Concept Set")
                .criteria(List.of(DEMOGRAPHICS_PREPACKAGED_DATA_FEATURE.getRight())),
            userEmail);
    assertNotNull(featureSet1);
    LOGGER.info("Created feature set {} at {}", featureSet1.getId(), featureSet1.getCreated());

    featureSet2 =
        featureSetService.createFeatureSet(
            study1.getId(),
            FeatureSet.builder()
                .underlay(UNDERLAY_NAME)
                .displayName("Second Concept Set")
                .criteria(List.of(ICD9CM_EQ_DIABETES.getRight())),
            userEmail);
    assertNotNull(featureSet2);
    LOGGER.info("Created feature set {} at {}", featureSet2.getId(), featureSet2.getCreated());

    cohort1 =
        cohortService.createCohort(
            study1.getId(),
            Cohort.builder().underlay(UNDERLAY_NAME).displayName("First Cohort"),
            userEmail,
            List.of(CRITERIA_GROUP_SECTION_GENDER));
    assertNotNull(cohort1);
    LOGGER.info("Created cohort {} at {}", cohort1.getId(), cohort1.getCreated());

    cohort2 =
        cohortService.createCohort(
            study1.getId(),
            Cohort.builder().underlay(UNDERLAY_NAME).displayName("Second Cohort"),
            userEmail,
            List.of(CRITERIA_GROUP_SECTION_GENDER, CRITERIA_GROUP_SECTION_AGE));
    assertNotNull(cohort2);
    LOGGER.info("Created cohort {} at {}", cohort2.getId(), cohort2.getCreated());

    AnnotationKey annotationKey1 =
        annotationService.createAnnotationKey(
            study1.getId(),
            cohort1.getId(),
            AnnotationKey.builder()
                .displayName("annotation key 1")
                .description("first annotation key")
                .dataType(DataType.INT64));
    assertNotNull(annotationKey1);
    LOGGER.info("Created annotation key {}", annotationKey1.getId());

    Underlay underlay = underlayService.getUnderlay(UNDERLAY_NAME);
    Entity primaryEntity = underlay.getPrimaryEntity();
    Review review1 =
        reviewService.createReview(
            study1.getId(),
            cohort1.getId(),
            Review.builder().size(10),
            userEmail,
            new AttributeFilter(
                underlay,
                primaryEntity,
                primaryEntity.getAttribute("gender"),
                BinaryOperator.EQUALS,
                Literal.forInt64(8_532L)));
    assertNotNull(review1);
    LOGGER.info("Created review {} at {}", review1.getId(), review1.getCreated());

    ReviewQueryRequest reviewQueryRequest =
        ReviewQueryRequest.builder()
            .attributes(primaryEntity.getAttributes())
            .orderBys(
                List.of(
                    new ReviewQueryOrderBy(
                        primaryEntity.getIdAttribute(), OrderByDirection.DESCENDING)))
            .build();
    ReviewQueryResult reviewQueryResult =
        reviewService.listReviewInstances(
            study1.getId(), cohort1.getId(), review1.getId(), reviewQueryRequest);
    Long instanceId =
        reviewQueryResult
            .getReviewInstances()
            .get(0)
            .getAttributeValues()
            .get(primaryEntity.getIdAttribute())
            .getValue()
            .getInt64Val();
    Literal intVal = Literal.forInt64(16L);
    annotationService.updateAnnotationValues(
        study1.getId(),
        cohort1.getId(),
        annotationKey1.getId(),
        review1.getId(),
        String.valueOf(instanceId),
        List.of(intVal));
    LOGGER.info("Created annotation value {}, {}", instanceId, intVal.getInt64Val());
  }

  @AfterEach
  void deleteReview() {
    try {
      studyService.deleteStudy(study1.getId(), "abc@123.com");
      LOGGER.info("Deleted study1 {}", study1.getId());
    } catch (Exception ex) {
      LOGGER.error("Error deleting study1", ex);
    }
  }

  @Test
  void listEntityModels() {
    List<DataExportModel> models = dataExportService.getModels(UNDERLAY_NAME);
    assertEquals(4, models.size());

    // Check the INDIVIDUAL_FILE_DOWNLOAD export option.
    Optional<DataExportModel> individualFileDownload =
        models.stream()
            .filter(
                m ->
                    DataExport.Type.INDIVIDUAL_FILE_DOWNLOAD.equals(m.getImpl().getType())
                        && !m.getConfig().hasNumPrimaryEntityCap())
            .findFirst();
    assertTrue(individualFileDownload.isPresent());
    String modelName = individualFileDownload.get().getName();
    String displayName = individualFileDownload.get().getDisplayName();
    DataExport impl = individualFileDownload.get().getImpl();
    assertEquals(
        DataExport.Type.INDIVIDUAL_FILE_DOWNLOAD.name(),
        modelName); // Default model name is type enum value.
    assertEquals(impl.getDefaultDisplayName(), displayName);
    assertNotNull(impl);

    // Check the IPYNB_FILE_DOWNLOAD export option.
    Optional<DataExportModel> ipynbFileDownload =
        models.stream()
            .filter(m -> DataExport.Type.IPYNB_FILE_DOWNLOAD.equals(m.getImpl().getType()))
            .findFirst();
    assertTrue(ipynbFileDownload.isPresent());
    modelName = ipynbFileDownload.get().getName();
    displayName = ipynbFileDownload.get().getDisplayName();
    impl = ipynbFileDownload.get().getImpl();
    assertEquals(
        DataExport.Type.IPYNB_FILE_DOWNLOAD.name(),
        modelName); // Default model name is type enum value.
    assertFalse(ipynbFileDownload.get().getConfig().hasNumPrimaryEntityCap());
    assertEquals(impl.getDefaultDisplayName(), displayName);
    assertNotNull(impl);

    // Check the VWB_FILE_IMPORT export option.
    Optional<DataExportModel> vwbFileImport =
        models.stream()
            .filter(m -> DataExport.Type.VWB_FILE_IMPORT.equals(m.getImpl().getType()))
            .findFirst();
    assertTrue(vwbFileImport.isPresent());
    modelName = vwbFileImport.get().getName();
    displayName = vwbFileImport.get().getDisplayName();
    impl = vwbFileImport.get().getImpl();
    assertEquals("VWB_FILE_IMPORT_DEVEL", modelName); // Overridden model name.
    assertEquals("Import to VWB (devel)", displayName); // Overridden display name.
    assertFalse(vwbFileImport.get().getConfig().hasNumPrimaryEntityCap());
    assertNotNull(impl);

    // Check the INDIVIDUAL_FILE_DOWNLOAD_WITH_CAP export option.
    Optional<DataExportModel> individualFileDownloadWithCap =
        models.stream()
            .filter(
                m ->
                    DataExport.Type.INDIVIDUAL_FILE_DOWNLOAD.equals(m.getImpl().getType())
                        && m.getConfig().hasNumPrimaryEntityCap())
            .findFirst();
    assertTrue(individualFileDownloadWithCap.isPresent());
    modelName = individualFileDownloadWithCap.get().getName();
    displayName = individualFileDownloadWithCap.get().getDisplayName();
    impl = individualFileDownloadWithCap.get().getImpl();
    assertEquals("INDIVIDUAL_FILE_DOWNLOAD_WITH_CAP", modelName); // Overridden model name.
    assertEquals("Individual file download with cap=5", displayName); // Overridden display name.
    assertEquals(5, individualFileDownloadWithCap.get().getConfig().getNumPrimaryEntityCap());
    assertNotNull(impl);
  }

  @Test
  void numPrimaryEntityInstancesCap() {
    ExportRequest exportRequest =
        new ExportRequest(
            "INDIVIDUAL_FILE_DOWNLOAD_WITH_CAP",
            Map.of(),
            null,
            true,
            "abc@123.com",
            underlayService.getUnderlay(UNDERLAY_NAME),
            study1,
            List.of(cohort1),
            List.of(featureSet1));
    ExportResult exportResult = dataExportService.run(exportRequest);
    assertNotNull(exportResult);
    assertFalse(exportResult.isSuccessful());
    assertNotNull(exportResult.getError());
    assertFalse(exportResult.getError().isTimeout());
    assertTrue(
        exportResult
            .getError()
            .getMessage()
            .startsWith("Maximum number of primary entity instances"));
  }

  @Test
  void individualFileDownload() throws IOException {
    ExportRequest exportRequest =
        new ExportRequest(
            "INDIVIDUAL_FILE_DOWNLOAD",
            Map.of(),
            null,
            true,
            "abc@123.com",
            underlayService.getUnderlay(UNDERLAY_NAME),
            study1,
            List.of(cohort1),
            List.of(featureSet1));
    ExportResult exportResult = dataExportService.run(exportRequest);
    assertNotNull(exportResult);
    assertTrue(exportResult.isSuccessful());
    assertNull(exportResult.getRedirectAwayUrl());
    assertEquals(2, exportResult.getFileResults().size());

    // Validate the entity instances file + tags.
    Optional<ExportFileResult> entityInstancesFileResult =
        exportResult.getFileResults().stream().filter(ExportFileResult::isEntityData).findFirst();
    assertTrue(entityInstancesFileResult.isPresent());
    assertTrue(entityInstancesFileResult.get().isSuccessful());
    assertTrue(entityInstancesFileResult.get().getFileDisplayName().endsWith(".csv.gzip"));
    assertEquals(
        List.of("Data", underlayService.getUnderlay(UNDERLAY_NAME).getPrimaryEntity().getName()),
        entityInstancesFileResult.get().getTags());
    String signedUrl = entityInstancesFileResult.get().getFileUrl();
    assertNotNull(signedUrl);
    LOGGER.info("Entity instances signed URL: {}", signedUrl);
    String fileContents = GoogleCloudStorage.readGzipFileContentsFromUrl(signedUrl, 6);
    LOGGER.info("Entity instances fileContents: {}", fileContents);
    assertFalse(fileContents.isEmpty());
    String fileContentsFirstLine = fileContents.split(System.lineSeparator())[0];
    assertEquals(
        "ethnicity,T_DISP_ethnicity,gender,T_DISP_gender,id,person_source_value,race,T_DISP_race,year_of_birth",
        fileContentsFirstLine);
    assertEquals(6, fileContents.split("\n").length); // 5 instances + header row

    // Validate the annotations file + tags.
    Optional<ExportFileResult> annotationsFileResult =
        exportResult.getFileResults().stream()
            .filter(ExportFileResult::isAnnotationData)
            .findFirst();
    assertTrue(annotationsFileResult.isPresent());
    assertTrue(annotationsFileResult.get().isSuccessful());
    assertTrue(annotationsFileResult.get().getFileDisplayName().endsWith(".csv"));
    assertEquals(
        List.of("Annotations", annotationsFileResult.get().getCohort().getDisplayName()),
        annotationsFileResult.get().getTags());
    signedUrl = annotationsFileResult.get().getFileUrl();
    assertNotNull(signedUrl);
    LOGGER.info("Annotations signed URL: {}", signedUrl);
    fileContents = GoogleCloudStorage.readFileContentsFromUrl(signedUrl);
    assertFalse(fileContents.isEmpty());
    LOGGER.info("Annotations fileContents: {}", fileContents);
    assertTrue(fileContents.startsWith("person_id,annotation key"));
    assertEquals(2, fileContents.split("\n").length); // 1 annotation + header row
  }

  @Test
  void vwbFileImport() throws IOException, URISyntaxException {
    String redirectBackUrl = "https://tanagra-test.api.verily.com";
    ExportRequest exportRequest =
        new ExportRequest(
            "VWB_FILE_IMPORT_DEVEL",
            Map.of(),
            redirectBackUrl,
            true,
            "abc@123.com",
            underlayService.getUnderlay(UNDERLAY_NAME),
            study1,
            List.of(cohort1),
            List.of(featureSet1));
    ExportResult exportResult = dataExportService.run(exportRequest);
    assertNotNull(exportResult);
    assertTrue(exportResult.isSuccessful());
    LOGGER.info("redirect away url: {}", exportResult.getRedirectAwayUrl());

    // Validate the file result objects.
    assertEquals(3, exportResult.getFileResults().size());
    Optional<ExportFileResult> entityDataFileResult =
        exportResult.getFileResults().stream().filter(ExportFileResult::isEntityData).findFirst();
    assertTrue(entityDataFileResult.isPresent());
    assertTrue(entityDataFileResult.get().isSuccessful());
    assertTrue(entityDataFileResult.get().getFileDisplayName().endsWith(".csv.gzip"));
    assertEquals(
        List.of("Data", underlayService.getUnderlay(UNDERLAY_NAME).getPrimaryEntity().getName()),
        entityDataFileResult.get().getTags());

    Optional<ExportFileResult> annotationsFileResult =
        exportResult.getFileResults().stream()
            .filter(ExportFileResult::isAnnotationData)
            .findFirst();
    assertTrue(annotationsFileResult.isPresent());
    assertTrue(annotationsFileResult.get().isSuccessful());
    assertTrue(annotationsFileResult.get().getFileDisplayName().endsWith(".csv"));
    assertEquals(
        List.of("Annotations", annotationsFileResult.get().getCohort().getDisplayName()),
        annotationsFileResult.get().getTags());

    Optional<ExportFileResult> tsvFileResult =
        exportResult.getFileResults().stream()
            .filter(
                exportFileResult ->
                    !exportFileResult.isEntityData() && !exportFileResult.isAnnotationData())
            .findFirst();
    assertTrue(tsvFileResult.isPresent());
    assertTrue(tsvFileResult.get().isSuccessful());
    assertTrue(tsvFileResult.get().getFileDisplayName().endsWith(".tsv"));
    assertEquals(List.of("URL List"), tsvFileResult.get().getTags());

    // Parse the redirect away URL into component parts.
    URL url = new URL(exportResult.getRedirectAwayUrl());
    assertEquals("terra-devel-ui-terra.api.verily.com", url.getHost());
    assertEquals("/import", url.getPath());
    List<NameValuePair> params = URLEncodedUtils.parse(url.toURI(), StandardCharsets.UTF_8);
    assertEquals("urlList", params.get(0).getName());
    assertTrue(params.get(0).getValue().startsWith("https://storage.googleapis.com/"));
    assertEquals("returnUrl", params.get(1).getName());
    assertEquals("https://tanagra-test.api.verily.com", params.get(1).getValue());
    assertEquals("returnApp", params.get(2).getName());
    assertEquals("Tanagra", params.get(2).getValue());

    // Validate the VWB input file.
    String signedUrl = params.get(0).getValue();
    LOGGER.info("VWB input file signed URL: {}", signedUrl);
    String fileContents = GoogleCloudStorage.readFileContentsFromUrl(signedUrl);
    assertFalse(fileContents.isEmpty());
    LOGGER.info("VWB input fileContents: {}", fileContents);
    assertTrue(fileContents.startsWith("TsvHttpData-1.0"));
    String[] fileLines = fileContents.split("\n");
    assertEquals(
        3, fileLines.length); // 1 url for entity instances, 1 url for annotations, 1 header row

    // The URLs are in lexicographical order.
    String signedUrl1 = fileLines[1].split("\t")[0];
    String fileContents1 = GoogleCloudStorage.readFileContentsFromUrl(signedUrl1);
    String signedUrl2 = fileLines[2].split("\t")[0];
    String fileContents2 = GoogleCloudStorage.readFileContentsFromUrl(signedUrl2);

    // Validate the entity instances and annotations files.
    String annotationsFileContents =
        fileContents1.startsWith("person_id,annotation key") ? fileContents1 : fileContents2;
    LOGGER.info("Annotations fileContents: {}", annotationsFileContents);
    assertTrue(annotationsFileContents.startsWith("person_id,annotation key"));
    assertEquals(2, annotationsFileContents.split("\n").length); // 1 annotation + header row

    String entityInstancesFileContents =
        annotationsFileContents.equals(fileContents1)
            ? GoogleCloudStorage.readGzipFileContentsFromUrl(signedUrl2, 6)
            : GoogleCloudStorage.readGzipFileContentsFromUrl(signedUrl1, 6);
    LOGGER.info("Entity instances fileContents: {}", entityInstancesFileContents);
    String fileContentsFirstLine = entityInstancesFileContents.split(System.lineSeparator())[0];
    assertEquals(
        "ethnicity,T_DISP_ethnicity,gender,T_DISP_gender,id,person_source_value,race,T_DISP_race,year_of_birth",
        fileContentsFirstLine);
    assertEquals(6, entityInstancesFileContents.split("\n").length); // 5 instances + header row
  }

  @Test
  void ipynbFileDownload() throws IOException {
    ExportRequest exportRequest =
        new ExportRequest(
            "IPYNB_FILE_DOWNLOAD",
            Map.of(),
            null,
            false,
            "abc@123.com",
            underlayService.getUnderlay(UNDERLAY_NAME),
            study1,
            List.of(cohort1),
            List.of(featureSet1));
    ExportResult exportResult = dataExportService.run(exportRequest);
    assertNotNull(exportResult);
    assertTrue(exportResult.isSuccessful());
    assertNull(exportResult.getRedirectAwayUrl());

    // Validate the file result objects.
    assertEquals(1, exportResult.getFileResults().size());
    ExportFileResult ipynbFileResult = exportResult.getFileResults().get(0);
    assertTrue(ipynbFileResult.isSuccessful());
    assertFalse(ipynbFileResult.isEntityData());
    assertFalse(ipynbFileResult.isAnnotationData());
    assertTrue(ipynbFileResult.getFileDisplayName().endsWith(".ipynb"));
    assertEquals(List.of("Notebook File"), ipynbFileResult.getTags());

    // Validate the ipynb file.
    String signedUrl = ipynbFileResult.getFileUrl();
    assertNotNull(signedUrl);
    LOGGER.info("ipynb file signed URL: {}", signedUrl);
    String fileContents = GoogleCloudStorage.readFileContentsFromUrl(signedUrl);
    assertFalse(fileContents.isEmpty());
    LOGGER.info("ipynb fileContents: {}", fileContents);
    assertTrue(fileContents.contains("# This query was generated for"));
    assertTrue(
        fileContents.contains(
            "bigquery-public-data.cms_synthetic_patient_data_omop")); // Source BQ dataset.
    assertTrue(fileContents.split("\n").length >= 36); // Length of notebook template file = 36.
    assertDoesNotThrow(
        () -> new ObjectMapper().readTree(fileContents)); // Notebook file is valid json.
  }

  @Test
  void exportLevelError() {
    ExportRequest exportRequest =
        new ExportRequest(
            "IPYNB_FILE_DOWNLOAD",
            Map.of("IPYNB_TEMPLATE_FILE_GCS_URL", "gs://invalid-bucket/invalid-file.ipynb"),
            null,
            false,
            "abc@123.com",
            underlayService.getUnderlay(UNDERLAY_NAME),
            study1,
            List.of(cohort1),
            List.of(featureSet1));
    ExportResult exportResult = dataExportService.run(exportRequest);
    assertNotNull(exportResult);
    assertFalse(exportResult.isSuccessful());
    assertNotNull(exportResult.getError());
    assertFalse(exportResult.getError().isTimeout());
    assertTrue(exportResult.getError().getMessage().contains("Template ipynb file not found"));
  }

  @Test
  void fileLevelMessageForNoData() {
    ExportRequest exportRequest =
        new ExportRequest(
            "INDIVIDUAL_FILE_DOWNLOAD",
            Map.of(),
            null,
            true,
            "abc@123.com",
            underlayService.getUnderlay(UNDERLAY_NAME),
            study1,
            List.of(cohort2),
            List.of(featureSet2));
    ExportResult exportResult = dataExportService.run(exportRequest);
    assertNotNull(exportResult);
    assertTrue(exportResult.isSuccessful());
    assertEquals(1, exportResult.getFileResults().size());

    Optional<ExportFileResult> conditionOccurrenceFileResult =
        exportResult.getFileResults().stream()
            .filter(
                exportFileResult ->
                    exportFileResult.getEntity() != null
                        && "conditionOccurrence"
                            .equalsIgnoreCase(exportFileResult.getEntity().getName()))
            .findFirst();
    assertTrue(conditionOccurrenceFileResult.isPresent());
    assertTrue(conditionOccurrenceFileResult.get().isSuccessful());
    assertNotNull(conditionOccurrenceFileResult.get().getFileUrl());
    assertTrue(conditionOccurrenceFileResult.get().getFileDisplayName().endsWith(".csv.gzip"));

    Optional<ExportFileResult> observationOccurrenceFileResult =
        exportResult.getFileResults().stream()
            .filter(
                exportFileResult ->
                    exportFileResult.getEntity() != null
                        && "observationOccurrence"
                            .equalsIgnoreCase(exportFileResult.getEntity().getName()))
            .findFirst();
    assertFalse(observationOccurrenceFileResult.isPresent());

    Optional<ExportFileResult> procedureOccurrenceFileResult =
        exportResult.getFileResults().stream()
            .filter(
                exportFileResult ->
                    exportFileResult.getEntity() != null
                        && "procedureOccurrence"
                            .equalsIgnoreCase(exportFileResult.getEntity().getName()))
            .findFirst();
    assertFalse(procedureOccurrenceFileResult.isPresent());

    Optional<ExportFileResult> annotationsFileResult =
        exportResult.getFileResults().stream()
            .filter(
                exportFileResult ->
                    exportFileResult.getCohort() != null
                        && cohort2.equals(exportFileResult.getCohort()))
            .findFirst();
    assertFalse(annotationsFileResult.isPresent());
  }

  private ListQueryRequest buildListQueryRequest() {
    Underlay underlay = underlayService.getUnderlay(UNDERLAY_NAME);
    Entity primaryEntity = underlay.getPrimaryEntity();

    // Select all attributes.
    List<ValueDisplayField> selectFields =
        primaryEntity.getAttributes().stream()
            .sorted(Comparator.comparing(Attribute::getName))
            .map(attribute -> new AttributeField(underlay, primaryEntity, attribute, false))
            .collect(Collectors.toList());
    return ListQueryRequest.againstIndexData(
        underlay, primaryEntity, selectFields, null, null, 5, null, null);
  }

  private EntityFilter buildPrimaryEntityFilter() {
    Underlay underlay = underlayService.getUnderlay(UNDERLAY_NAME);
    Entity primaryEntity = underlay.getPrimaryEntity();
    return new AttributeFilter(
        underlay,
        primaryEntity,
        primaryEntity.getAttribute("gender"),
        BinaryOperator.EQUALS,
        Literal.forInt64(8_532L));
  }
}

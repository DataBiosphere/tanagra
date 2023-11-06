package bio.terra.tanagra.service;

import static bio.terra.tanagra.service.CriteriaGroupSectionValues.CRITERIA_GROUP_SECTION_3;
import static bio.terra.tanagra.service.export.impl.IpynbFileDownload.IPYNB_FILE_KEY;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bio.terra.tanagra.api.query.EntityQueryRequest;
import bio.terra.tanagra.api.query.filter.AttributeFilter;
import bio.terra.tanagra.api.query.filter.EntityFilter;
import bio.terra.tanagra.app.Main;
import bio.terra.tanagra.query.Literal;
import bio.terra.tanagra.query.OrderByDirection;
import bio.terra.tanagra.query.filtervariable.BinaryFilterVariable;
import bio.terra.tanagra.service.artifact.AnnotationService;
import bio.terra.tanagra.service.artifact.CohortService;
import bio.terra.tanagra.service.artifact.ReviewService;
import bio.terra.tanagra.service.artifact.StudyService;
import bio.terra.tanagra.service.artifact.model.AnnotationKey;
import bio.terra.tanagra.service.artifact.model.Cohort;
import bio.terra.tanagra.service.artifact.model.Review;
import bio.terra.tanagra.service.artifact.model.Study;
import bio.terra.tanagra.service.export.DataExport;
import bio.terra.tanagra.service.export.DataExportService;
import bio.terra.tanagra.service.export.ExportRequest;
import bio.terra.tanagra.service.export.ExportResult;
import bio.terra.tanagra.service.query.ReviewQueryOrderBy;
import bio.terra.tanagra.service.query.ReviewQueryRequest;
import bio.terra.tanagra.service.query.ReviewQueryResult;
import bio.terra.tanagra.service.query.UnderlayService;
import bio.terra.tanagra.underlay.Entity;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.utils.GoogleCloudStorage;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.lang3.tuple.Pair;
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
  private static final String UNDERLAY_NAME = "cms_synpuf";
  @Autowired private UnderlayService underlayService;

  @Autowired private StudyService studyService;
  @Autowired private CohortService cohortService;
  @Autowired private AnnotationService annotationService;
  @Autowired private ReviewService reviewService;
  @Autowired private DataExportService dataExportService;

  private Study study1;
  private Cohort cohort1;

  @BeforeEach
  void createAnnotationValues() {
    // Build a study, cohort, review, and annotation data.
    String userEmail = "abc@123.com";

    study1 = studyService.createStudy(Study.builder(), userEmail);
    assertNotNull(study1);
    LOGGER.info("Created study1 {} at {}", study1.getId(), study1.getCreated());

    cohort1 =
        cohortService.createCohort(
            study1.getId(),
            Cohort.builder().underlay(UNDERLAY_NAME).displayName("First Cohort"),
            userEmail,
            List.of(CRITERIA_GROUP_SECTION_3));
    assertNotNull(cohort1);
    LOGGER.info("Created cohort {} at {}", cohort1.getId(), cohort1.getCreated());

    AnnotationKey annotationKey1 =
        annotationService.createAnnotationKey(
            study1.getId(),
            cohort1.getId(),
            AnnotationKey.builder()
                .displayName("annotation key 1")
                .description("first annotation key")
                .dataType(Literal.DataType.INT64));
    assertNotNull(annotationKey1);
    LOGGER.info("Created annotation key {}", annotationKey1.getId());

    Entity primaryEntity = underlayService.getUnderlay(UNDERLAY_NAME).getPrimaryEntity();
    Review review1 =
        reviewService.createReview(
            study1.getId(),
            cohort1.getId(),
            Review.builder().size(10),
            userEmail,
            new AttributeFilter(
                primaryEntity.getAttribute("gender"),
                BinaryFilterVariable.BinaryOperator.EQUALS,
                new Literal(8532)));
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
    Literal intVal = new Literal(16L);
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
    Map<String, Pair<String, DataExport>> models = dataExportService.getModels(UNDERLAY_NAME);
    assertEquals(3, models.size());

    // Check the INDIVIDUAL_FILE_DOWNLOAD export option.
    Optional<Map.Entry<String, Pair<String, DataExport>>> individualFileDownload =
        models.entrySet().stream()
            .filter(
                m ->
                    DataExport.Type.INDIVIDUAL_FILE_DOWNLOAD.equals(
                        m.getValue().getValue().getType()))
            .findFirst();
    assertTrue(individualFileDownload.isPresent());
    String modelName = individualFileDownload.get().getKey();
    String displayName = individualFileDownload.get().getValue().getKey();
    DataExport impl = individualFileDownload.get().getValue().getValue();
    assertEquals(
        DataExport.Type.INDIVIDUAL_FILE_DOWNLOAD.name(),
        modelName); // Default model name is type enum value.
    assertEquals(impl.getDefaultDisplayName(), displayName);
    assertNotNull(impl);

    // Check the IPYNB_FILE_DOWNLOAD export option.
    Optional<Map.Entry<String, Pair<String, DataExport>>> ipynbFileDownload =
        models.entrySet().stream()
            .filter(
                m -> DataExport.Type.IPYNB_FILE_DOWNLOAD.equals(m.getValue().getValue().getType()))
            .findFirst();
    assertTrue(ipynbFileDownload.isPresent());
    modelName = ipynbFileDownload.get().getKey();
    displayName = ipynbFileDownload.get().getValue().getKey();
    impl = ipynbFileDownload.get().getValue().getValue();
    assertEquals(
        DataExport.Type.IPYNB_FILE_DOWNLOAD.name(),
        modelName); // Default model name is type enum value.
    assertEquals(impl.getDefaultDisplayName(), displayName);
    assertNotNull(impl);

    // Check the VWB_FILE_IMPORT export option.
    Optional<Map.Entry<String, Pair<String, DataExport>>> vwbFileImport =
        models.entrySet().stream()
            .filter(m -> DataExport.Type.VWB_FILE_IMPORT.equals(m.getValue().getValue().getType()))
            .findFirst();
    assertTrue(vwbFileImport.isPresent());
    modelName = vwbFileImport.get().getKey();
    displayName = vwbFileImport.get().getValue().getKey();
    impl = vwbFileImport.get().getValue().getValue();
    assertEquals("VWB_FILE_IMPORT_DEVEL", modelName); // Overridden model name.
    assertEquals("Import to VWB (devel)", displayName); // Overridden display name.
    assertNotNull(impl);
  }

  @Test
  void individualFileDownload() throws IOException {
    ExportRequest.Builder exportRequest =
        ExportRequest.builder().model("INDIVIDUAL_FILE_DOWNLOAD").includeAnnotations(true);
    ExportResult exportResult =
        dataExportService.run(
            study1.getId(),
            List.of(cohort1.getId()),
            exportRequest,
            List.of(buildEntityQueryRequest()),
            buildPrimaryEntityFilter(),
            "abc@123.com");
    assertNotNull(exportResult);
    assertEquals(ExportResult.Status.COMPLETE, exportResult.getStatus());
    assertNull(exportResult.getRedirectAwayUrl());
    assertEquals(2, exportResult.getOutputs().size());

    // Validate the entity instances file.
    String signedUrl =
        exportResult
            .getOutputs()
            .get("Data:" + underlayService.getUnderlay(UNDERLAY_NAME).getPrimaryEntity().getName());
    assertNotNull(signedUrl);
    LOGGER.info("Entity instances signed URL: {}", signedUrl);
    String fileContents = GoogleCloudStorage.readFileContentsFromUrl(signedUrl);
    assertFalse(fileContents.isEmpty());
    LOGGER.info("Entity instances fileContents: {}", fileContents);
    assertTrue(
        fileContents.startsWith(
            "age,ethnicity,gender,id,race,t_display_ethnicity,t_display_gender,t_display_race"));
    assertEquals(6, fileContents.split("\n").length); // 5 instances + header row

    // Validate the annotations file.
    signedUrl = exportResult.getOutputs().get("Annotations:" + cohort1.getDisplayName());
    assertNotNull(signedUrl);
    LOGGER.info("Annotations signed URL: {}", signedUrl);
    fileContents = GoogleCloudStorage.readFileContentsFromUrl(signedUrl);
    assertFalse(fileContents.isEmpty());
    LOGGER.info("Annotations fileContents: {}", fileContents);
    assertTrue(fileContents.startsWith("person_id\tannotation key"));
    assertEquals(2, fileContents.split("\n").length); // 1 annotation + header row
  }

  @Test
  void vwbFileImport() throws IOException, URISyntaxException {
    String redirectBackUrl = "https://tanagra-test.api.verily.com";
    ExportRequest.Builder exportRequest =
        ExportRequest.builder()
            .model("VWB_FILE_IMPORT_DEVEL")
            .redirectBackUrl(redirectBackUrl)
            .includeAnnotations(true);
    ExportResult exportResult =
        dataExportService.run(
            study1.getId(),
            List.of(cohort1.getId()),
            exportRequest,
            List.of(buildEntityQueryRequest()),
            buildPrimaryEntityFilter(),
            "abc@123.com");
    assertNotNull(exportResult);
    assertEquals(ExportResult.Status.COMPLETE, exportResult.getStatus());
    assertTrue(exportResult.getOutputs().isEmpty());
    LOGGER.info("redirect away url: {}", exportResult.getRedirectAwayUrl());

    // Parse the redirect away URL into component parts.
    URL url = new URL(exportResult.getRedirectAwayUrl());
    assertEquals("terra-devel-ui-terra.api.verily.com", url.getHost());
    assertEquals("/import", url.getPath());
    List<NameValuePair> params = URLEncodedUtils.parse(url.toURI(), Charset.forName("UTF-8"));
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
        fileContents1.startsWith("person_id\tannotation key") ? fileContents1 : fileContents2;
    LOGGER.info("Annotations fileContents: {}", annotationsFileContents);
    assertTrue(annotationsFileContents.startsWith("person_id\tannotation key"));
    assertEquals(2, annotationsFileContents.split("\n").length); // 1 annotation + header row

    String entityInstancesFileContents =
        annotationsFileContents.equals(fileContents1) ? fileContents2 : fileContents1;
    LOGGER.info("Entity instances fileContents: {}", entityInstancesFileContents);
    assertTrue(
        entityInstancesFileContents.startsWith(
            "age,ethnicity,gender,id,race,t_display_ethnicity,t_display_gender,t_display_race"));
    assertEquals(6, entityInstancesFileContents.split("\n").length); // 5 instances + header row
  }

  @Test
  void ipynbFileDownload() throws IOException {
    ExportRequest.Builder exportRequest = ExportRequest.builder().model("IPYNB_FILE_DOWNLOAD");
    ExportResult exportResult =
        dataExportService.run(
            study1.getId(),
            List.of(cohort1.getId()),
            exportRequest,
            List.of(buildEntityQueryRequest()),
            buildPrimaryEntityFilter(),
            "abc@123.com");
    assertNotNull(exportResult);
    assertEquals(ExportResult.Status.COMPLETE, exportResult.getStatus());
    assertNull(exportResult.getRedirectAwayUrl());
    assertEquals(1, exportResult.getOutputs().size());

    // Validate the ipynb file.
    String signedUrl = exportResult.getOutputs().get(IPYNB_FILE_KEY);
    assertNotNull(signedUrl);
    LOGGER.info("ipynb file signed URL: {}", signedUrl);
    String fileContents = GoogleCloudStorage.readFileContentsFromUrl(signedUrl);
    assertFalse(fileContents.isEmpty());
    LOGGER.info("ipynb fileContents: {}", fileContents);
    assertTrue(fileContents.contains("# This query was generated for"));
    assertTrue(fileContents.split("\n").length >= 36); // Length of notebook template file = 36.
    assertDoesNotThrow(
        () -> new ObjectMapper().readTree(fileContents)); // Notebook file is valid json.
  }

  private EntityQueryRequest buildEntityQueryRequest() {
    Entity primaryEntity = underlayService.getUnderlay(UNDERLAY_NAME).getPrimaryEntity();
    return new EntityQueryRequest.Builder()
        .entity(primaryEntity)
        .mappingType(Underlay.MappingType.INDEX)
        .selectAttributes(primaryEntity.getAttributes())
        .limit(5)
        .build();
  }

  private EntityFilter buildPrimaryEntityFilter() {
    Entity primaryEntity = underlayService.getUnderlay(UNDERLAY_NAME).getPrimaryEntity();
    return new AttributeFilter(
        primaryEntity.getAttribute("year_of_birth"),
        BinaryFilterVariable.BinaryOperator.EQUALS,
        new Literal(11L));
  }
}

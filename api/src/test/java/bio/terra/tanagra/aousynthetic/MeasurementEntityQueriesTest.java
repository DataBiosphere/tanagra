package bio.terra.tanagra.aousynthetic;

import static bio.terra.tanagra.aousynthetic.UnderlayUtils.ALL_MEASUREMENT_ATTRIBUTES;
import static bio.terra.tanagra.aousynthetic.UnderlayUtils.MEASUREMENT_ENTITY;
import static bio.terra.tanagra.aousynthetic.UnderlayUtils.UNDERLAY_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;

import bio.terra.tanagra.app.controller.EntityInstancesApiController;
import bio.terra.tanagra.generated.model.ApiAttributeValue;
import bio.terra.tanagra.generated.model.ApiAttributeVariable;
import bio.terra.tanagra.generated.model.ApiBinaryFilter;
import bio.terra.tanagra.generated.model.ApiBinaryFilterOperator;
import bio.terra.tanagra.generated.model.ApiEntityDataset;
import bio.terra.tanagra.generated.model.ApiFilter;
import bio.terra.tanagra.generated.model.ApiGenerateDatasetSqlQueryRequest;
import bio.terra.tanagra.generated.model.ApiSqlQuery;
import bio.terra.tanagra.testing.BaseSpringUnitTest;
import bio.terra.tanagra.testing.GeneratedSqlUtils;
import java.io.IOException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Tests for measurement entity queries on the AoU synthetic underlay. There is no need to specify
 * an active profile for this test, because we want to test the main application definition.
 */
public class MeasurementEntityQueriesTest extends BaseSpringUnitTest {
  @Autowired private EntityInstancesApiController apiController;

  @Test
  @DisplayName("correct SQL string for listing all measurement entity instances")
  void generateSqlForAllMeasurementEntities() throws IOException {
    ResponseEntity<ApiSqlQuery> response =
        apiController.generateDatasetSqlQuery(
            UNDERLAY_NAME,
            MEASUREMENT_ENTITY,
            new ApiGenerateDatasetSqlQueryRequest()
                .entityDataset(
                    new ApiEntityDataset()
                        .entityVariable("measurement_alias")
                        .selectedAttributes(ALL_MEASUREMENT_ATTRIBUTES)));
    assertEquals(HttpStatus.OK, response.getStatusCode());
    String generatedSql = response.getBody().getQuery();
    GeneratedSqlUtils.checkMatchesOrOverwriteGoldenFile(
        generatedSql, "aousynthetic/all-measurement-entities.sql");
  }

  @Test
  @DisplayName("correct SQL string for listing descendants of a single measurement entity instance")
  void generateSqlForDescendantsOfAMeasurementEntity() throws IOException {
    // filter for "measurement" entity instances that are descendants of the "measurement" entity
    // instance with concept_id=37048668
    // i.e. give me all the descendants of "Glucose tolerance 2 hours panel"
    ApiFilter descendantsOfGlucoseTolerance =
        new ApiFilter()
            .binaryFilter(
                new ApiBinaryFilter()
                    .attributeVariable(
                        new ApiAttributeVariable().variable("measurement_alias").name("concept_id"))
                    .operator(ApiBinaryFilterOperator.DESCENDANT_OF_INCLUSIVE)
                    .attributeValue(new ApiAttributeValue().int64Val(37_048_668L)));

    ResponseEntity<ApiSqlQuery> response =
        apiController.generateDatasetSqlQuery(
            UNDERLAY_NAME,
            MEASUREMENT_ENTITY,
            new ApiGenerateDatasetSqlQueryRequest()
                .entityDataset(
                    new ApiEntityDataset()
                        .entityVariable("measurement_alias")
                        .selectedAttributes(ALL_MEASUREMENT_ATTRIBUTES)
                        .filter(descendantsOfGlucoseTolerance)));
    assertEquals(HttpStatus.OK, response.getStatusCode());
    String generatedSql = response.getBody().getQuery();
    GeneratedSqlUtils.checkMatchesOrOverwriteGoldenFile(
        generatedSql, "aousynthetic/measurement-entities-descendants-of-a-measurement.sql");
  }

  @Test
  @DisplayName("correct SQL string for listing children of a single measurement entity instance")
  void generateSqlForChildrenOfAMeasurementEntity() throws IOException {
    // filter for "measurement" entity instances that are children of the "measurement" entity
    // instance with concept_id=37072239
    // i.e. give me all the children of "Glucose tolerance 2 hours panel | Serum or Plasma |
    // Challenge Bank Panels"
    ApiFilter childrenOfGlucoseToleranceSerumOrPlasma =
        new ApiFilter()
            .binaryFilter(
                new ApiBinaryFilter()
                    .attributeVariable(
                        new ApiAttributeVariable().variable("measurement_alias").name("concept_id"))
                    .operator(ApiBinaryFilterOperator.CHILD_OF)
                    .attributeValue(new ApiAttributeValue().int64Val(37_072_239L)));

    ResponseEntity<ApiSqlQuery> response =
        apiController.generateDatasetSqlQuery(
            UNDERLAY_NAME,
            MEASUREMENT_ENTITY,
            new ApiGenerateDatasetSqlQueryRequest()
                .entityDataset(
                    new ApiEntityDataset()
                        .entityVariable("measurement_alias")
                        .selectedAttributes(ALL_MEASUREMENT_ATTRIBUTES)
                        .filter(childrenOfGlucoseToleranceSerumOrPlasma)));
    assertEquals(HttpStatus.OK, response.getStatusCode());
    String generatedSql = response.getBody().getQuery();
    GeneratedSqlUtils.checkMatchesOrOverwriteGoldenFile(
        generatedSql, "aousynthetic/measurement-entities-children-of-a-measurement.sql");
  }
}

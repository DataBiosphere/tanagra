package bio.terra.tanagra.aousynthetic;

import static bio.terra.tanagra.aousynthetic.UnderlayUtils.ALL_MEASUREMENT_OCCURRENCE_ATTRIBUTES;
import static bio.terra.tanagra.aousynthetic.UnderlayUtils.MEASUREMENT_OCCURRENCE_ENTITY;
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
import bio.terra.tanagra.generated.model.ApiRelationshipFilter;
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
 * Tests for measurement occurrence entity queries on the AoU synthetic underlay. There is no need
 * to specify an active profile for this test, because we want to test the main application
 * definition.
 */
public class MeasurementOccurrenceEntityQueriesTest extends BaseSpringUnitTest {
  @Autowired private EntityInstancesApiController apiController;

  @Test
  @DisplayName(
      "example dataset builder query: cohort=people who've had a hematocrit, concept set=all measurements")
  void generateSqlForMeasurementOccurrenceEntitiesRelatedToPeopleWithAMeasurement()
      throws IOException {
    // filter for "measurement" entity instances that have concept_id=3009542
    // i.e. the measurement "Hematocrit [Volume Fraction] of Blood"
    ApiFilter hematocrit =
        new ApiFilter()
            .binaryFilter(
                new ApiBinaryFilter()
                    .attributeVariable(
                        new ApiAttributeVariable().variable("measurement_alias").name("concept_id"))
                    .operator(ApiBinaryFilterOperator.EQUALS)
                    .attributeValue(new ApiAttributeValue().int64Val(3_009_542L)));

    // filter for "measurement_occurrence" entity instances that are related to "measruement" entity
    // instances that have concept_id=3009542
    // i.e. give me all the measurement occurrences of "Hematocrit [Volume Fraction] of Blood"
    ApiFilter occurrencesOfHematocrit =
        new ApiFilter()
            .relationshipFilter(
                new ApiRelationshipFilter()
                    .outerVariable("measurement_occurrence_alias1")
                    .newVariable("measurement_alias")
                    .newEntity("measurement")
                    .filter(hematocrit));

    // filter for "person" entity instances that are related to "measurement_occurrence" entity
    // instances that are related to "measruement" entity instances that have concept_id=3009542
    // i.e. give me all the people with measurement occurrences of "Hematocrit [Volume Fraction] of
    // Blood"
    ApiFilter peopleWhoHadHematocrit =
        new ApiFilter()
            .relationshipFilter(
                new ApiRelationshipFilter()
                    .outerVariable("person_alias")
                    .newVariable("measurement_occurrence_alias1")
                    .newEntity("measurement_occurrence")
                    .filter(occurrencesOfHematocrit));

    // filter for "measurement_occurrence" entity instances that are related to "person" entity
    // instances that are related to "measurement_occurrence" entity instances that are related to
    // "measurement" entity instances that have concept_id=3009542
    // i.e. give me all the measurement occurrence rows for people with "Hematocrit [Volume
    // Fraction] of Blood". this set of rows will include non-hematocrit measurement occurrences,
    // such as glucose test.
    ApiFilter allMeasurementOccurrencesForPeopleWhoHadHematocrit =
        new ApiFilter()
            .relationshipFilter(
                new ApiRelationshipFilter()
                    .outerVariable("measurement_occurrence_alias2")
                    .newVariable("person_alias")
                    .newEntity("person")
                    .filter(peopleWhoHadHematocrit));

    ResponseEntity<ApiSqlQuery> response =
        apiController.generateDatasetSqlQuery(
            UNDERLAY_NAME,
            MEASUREMENT_OCCURRENCE_ENTITY,
            new ApiGenerateDatasetSqlQueryRequest()
                .entityDataset(
                    new ApiEntityDataset()
                        .entityVariable("measurement_occurrence_alias2")
                        .selectedAttributes(ALL_MEASUREMENT_OCCURRENCE_ATTRIBUTES)
                        .filter(allMeasurementOccurrencesForPeopleWhoHadHematocrit)));
    assertEquals(HttpStatus.OK, response.getStatusCode());
    String generatedSql = response.getBody().getQuery();
    GeneratedSqlUtils.checkMatchesOrOverwriteGoldenFile(
        generatedSql,
        "aousynthetic/measurement-occurrence-entities-related-to-people-with-a-measurement.sql");
  }
}

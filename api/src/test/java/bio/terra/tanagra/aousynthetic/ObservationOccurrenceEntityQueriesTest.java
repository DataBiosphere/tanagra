package bio.terra.tanagra.aousynthetic;

import static bio.terra.tanagra.aousynthetic.UnderlayUtils.ALL_OBSERVATION_OCCURRENCE_ATTRIBUTES;
import static bio.terra.tanagra.aousynthetic.UnderlayUtils.OBSERVATION_OCCURRENCE_ENTITY;
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
 * Tests for observation occurrence entity queries on the AoU synthetic underlay. There is no need
 * to specify an active profile for this test, because we want to test the main application
 * definition.
 */
public class ObservationOccurrenceEntityQueriesTest extends BaseSpringUnitTest {
  @Autowired private EntityInstancesApiController apiController;

  @Test
  @DisplayName(
      "example dataset builder query: cohort=people who've refused a vaccine, concept set=all observations")
  void generateSqlForObservationOccurrenceEntitiesRelatedToPeopleWithAnObservations()
      throws IOException {
    // filter for "observation" entity instances that have concept_id=43531662
    // i.e. the observation "Vaccine refused by patient"
    ApiFilter refusedVaccine =
        new ApiFilter()
            .binaryFilter(
                new ApiBinaryFilter()
                    .attributeVariable(
                        new ApiAttributeVariable().variable("observation_alias").name("concept_id"))
                    .operator(ApiBinaryFilterOperator.EQUALS)
                    .attributeValue(new ApiAttributeValue().int64Val(43_531_662L)));

    // filter for "observation_occurrence" entity instances that are related to "observation" entity
    // instances that have concept_id=43531662
    // i.e. give me all the observation occurrences of "Vaccine refused by patient"
    ApiFilter occurrencesOfVaccineRefusal =
        new ApiFilter()
            .relationshipFilter(
                new ApiRelationshipFilter()
                    .outerVariable("observation_occurrence_alias1")
                    .newVariable("observation_alias")
                    .newEntity("observation")
                    .filter(refusedVaccine));

    // filter for "person" entity instances that are related to "observation_occurrence" entity
    // instances that are related to "observation" entity instances that have concept_id=43531662
    // i.e. give me all the people with observation occurrences of "Vaccine refused by patient"
    ApiFilter peopleWhoRefusedVaccine =
        new ApiFilter()
            .relationshipFilter(
                new ApiRelationshipFilter()
                    .outerVariable("person_alias")
                    .newVariable("observation_occurrence_alias1")
                    .newEntity("observation_occurrence")
                    .filter(occurrencesOfVaccineRefusal));

    // filter for "observation_occurrence" entity instances that are related to "person" entity
    // instances that are related to "observation_occurrence" entity instances that are related to
    // "observation" entity instances that have concept_id=43531662
    // i.e. give me all the observation occurrence rows for people with "Vaccine refused by
    // patient". this set of rows will include non-vaccine ingredient occurrences, such as blood
    // disorder.
    ApiFilter allObservationOccurrencesForPeopleWhoRefusedVaccine =
        new ApiFilter()
            .relationshipFilter(
                new ApiRelationshipFilter()
                    .outerVariable("observation_occurrence_alias2")
                    .newVariable("person_alias")
                    .newEntity("person")
                    .filter(peopleWhoRefusedVaccine));

    ResponseEntity<ApiSqlQuery> response =
        apiController.generateDatasetSqlQuery(
            UNDERLAY_NAME,
            OBSERVATION_OCCURRENCE_ENTITY,
            new ApiGenerateDatasetSqlQueryRequest()
                .entityDataset(
                    new ApiEntityDataset()
                        .entityVariable("observation_occurrence_alias2")
                        .selectedAttributes(ALL_OBSERVATION_OCCURRENCE_ATTRIBUTES)
                        .filter(allObservationOccurrencesForPeopleWhoRefusedVaccine)));
    assertEquals(HttpStatus.OK, response.getStatusCode());
    String generatedSql = response.getBody().getQuery();
    GeneratedSqlUtils.checkMatchesOrOverwriteGoldenFile(
        generatedSql,
        "aousynthetic/observation-occurrence-entities-related-to-people-with-an-observation.sql");
  }
}

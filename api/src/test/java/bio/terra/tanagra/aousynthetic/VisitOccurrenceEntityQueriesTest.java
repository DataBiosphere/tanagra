package bio.terra.tanagra.aousynthetic;

import static bio.terra.tanagra.aousynthetic.UnderlayUtils.ALL_VISIT_OCCURRENCE_ATTRIBUTES;
import static bio.terra.tanagra.aousynthetic.UnderlayUtils.UNDERLAY_NAME;
import static bio.terra.tanagra.aousynthetic.UnderlayUtils.VISIT_OCCURRENCE_ENTITY;
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
 * Tests for visit occurrence entity queries on the AoU synthetic underlay. There is no need to
 * specify an active profile for this test, because we want to test the main application definition.
 */
public class VisitOccurrenceEntityQueriesTest extends BaseSpringUnitTest {
  @Autowired private EntityInstancesApiController apiController;

  @Test
  @DisplayName(
      "example dataset builder query: cohort=people who've had an outpatient visit, concept set=all procedures")
  void generateSqlForVisitOccurrenceEntitiesRelatedToPeopleWithAVisit() throws IOException {
    // filter for "visit" entity instances that have concept_id=9202
    // i.e. the visit "Outpatient Visit"
    ApiFilter outpatientVisit =
        new ApiFilter()
            .binaryFilter(
                new ApiBinaryFilter()
                    .attributeVariable(
                        new ApiAttributeVariable().variable("visit_alias").name("concept_id"))
                    .operator(ApiBinaryFilterOperator.EQUALS)
                    .attributeValue(new ApiAttributeValue().int64Val(9_202L)));

    // filter for "visit_occurrence" entity instances that are related to "visit" entity
    // instances that have concept_id=9202
    // i.e. give me all the visit occurrences of "Outpatient Visit"
    ApiFilter occurrencesOfOutpatientVisit =
        new ApiFilter()
            .relationshipFilter(
                new ApiRelationshipFilter()
                    .outerVariable("visit_occurrence_alias1")
                    .newVariable("visit_alias")
                    .newEntity("visit")
                    .filter(outpatientVisit));

    // filter for "person" entity instances that are related to "visit_occurrence" entity
    // instances that are related to "visit" entity instances that have concept_id=9202
    // i.e. give me all the people with visit occurrences of "Outpatient Visit"
    ApiFilter peopleWhoHadOutpatientVisit =
        new ApiFilter()
            .relationshipFilter(
                new ApiRelationshipFilter()
                    .outerVariable("person_alias")
                    .newVariable("visit_occurrence_alias1")
                    .newEntity("visit_occurrence")
                    .filter(occurrencesOfOutpatientVisit));

    // filter for "visit_occurrence" entity instances that are related to "person" entity
    // instances that are related to "visit_occurrence" entity instances that are related to
    // "visit" entity instances that have concept_id=9202
    // i.e. give me all the visit occurrence rows for people with "Outpatient Visit"
    ApiFilter allVisitOccurrencesForPeopleWithOutpatientVisit =
        new ApiFilter()
            .relationshipFilter(
                new ApiRelationshipFilter()
                    .outerVariable("visit_occurrence_alias2")
                    .newVariable("person_alias")
                    .newEntity("person")
                    .filter(peopleWhoHadOutpatientVisit));

    ResponseEntity<ApiSqlQuery> response =
        apiController.generateDatasetSqlQuery(
            UNDERLAY_NAME,
            VISIT_OCCURRENCE_ENTITY,
            new ApiGenerateDatasetSqlQueryRequest()
                .entityDataset(
                    new ApiEntityDataset()
                        .entityVariable("visit_occurrence_alias2")
                        .selectedAttributes(ALL_VISIT_OCCURRENCE_ATTRIBUTES)
                        .filter(allVisitOccurrencesForPeopleWithOutpatientVisit)));
    assertEquals(HttpStatus.OK, response.getStatusCode());
    String generatedSql = response.getBody().getQuery();
    GeneratedSqlUtils.checkMatchesOrOverwriteGoldenFile(
        generatedSql, "aousynthetic/visit-occurrence-entities-related-to-people-with-a-visit.sql");
  }
}

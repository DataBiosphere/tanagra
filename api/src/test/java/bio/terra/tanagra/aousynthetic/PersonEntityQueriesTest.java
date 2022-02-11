package bio.terra.tanagra.aousynthetic;

import static bio.terra.tanagra.aousynthetic.UnderlayUtils.ALL_PERSON_ATTRIBUTES;
import static bio.terra.tanagra.aousynthetic.UnderlayUtils.PERSON_ENTITY;
import static bio.terra.tanagra.aousynthetic.UnderlayUtils.PERSON_ID_ATTRIBUTE;
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
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Tests for person entity queries on the AoU synthetic underlay. There is no need to specify an
 * active profile for this test, because we want to test the main application definition.
 */
public class PersonEntityQueriesTest extends BaseSpringUnitTest {
  @Autowired private EntityInstancesApiController apiController;

  @Test
  @DisplayName("correct SQL string for listing all person entity instances")
  void generateSqlForAllPersonEntities() throws IOException {
    ResponseEntity<ApiSqlQuery> response =
        apiController.generateDatasetSqlQuery(
            UNDERLAY_NAME,
            PERSON_ENTITY,
            new ApiGenerateDatasetSqlQueryRequest()
                .entityDataset(
                    new ApiEntityDataset()
                        .entityVariable("person_alias")
                        .selectedAttributes(ALL_PERSON_ATTRIBUTES)));
    assertEquals(HttpStatus.OK, response.getStatusCode());
    String generatedSql = response.getBody().getQuery();
    GeneratedSqlUtils.checkMatchesOrOverwriteGoldenFile(
        generatedSql, "aousynthetic/all-person-entities.sql");
  }

  @Test
  @DisplayName("example cohort builder breakdown query: cohort=people who've had covid")
  void generateSqlForPersonEntitiesWithACondition() throws IOException {
    // filter for "condition" entity instances that have concept_id=439676
    // i.e. the condition "Coronavirus infection"
    ApiFilter coronavirusInfection =
        new ApiFilter()
            .binaryFilter(
                new ApiBinaryFilter()
                    .attributeVariable(
                        new ApiAttributeVariable().variable("condition_alias").name("concept_id"))
                    .operator(ApiBinaryFilterOperator.EQUALS)
                    .attributeValue(new ApiAttributeValue().int64Val(439_676L)));

    // filter for "condition_occurrence" entity instances that are related to "condition" entity
    // instances that have concept_id=439676
    // i.e. give me all the condition occurrences of "Coronavirus infection"
    ApiFilter occurrencesOfCoronavirusInfection =
        new ApiFilter()
            .relationshipFilter(
                new ApiRelationshipFilter()
                    .outerVariable("condition_occurrence_alias")
                    .newVariable("condition_alias")
                    .newEntity("condition")
                    .filter(coronavirusInfection));

    // filter for "person" entity instances that are related to "condition_occurrence" entity
    // instances that are related to "condition" entity instances that have concept_id=439676
    // i.e. give me all the people with condition occurrences of "Coronavirus infection"
    ApiFilter peopleWithOccurrencesOfCoronavirusInfection =
        new ApiFilter()
            .relationshipFilter(
                new ApiRelationshipFilter()
                    .outerVariable("person_alias")
                    .newVariable("condition_occurrence_alias")
                    .newEntity("condition_occurrence")
                    .filter(occurrencesOfCoronavirusInfection));

    ResponseEntity<ApiSqlQuery> response =
        apiController.generateDatasetSqlQuery(
            UNDERLAY_NAME,
            PERSON_ENTITY,
            new ApiGenerateDatasetSqlQueryRequest()
                .entityDataset(
                    new ApiEntityDataset()
                        .entityVariable("person_alias")
                        .selectedAttributes(ImmutableList.of(PERSON_ID_ATTRIBUTE))
                        .filter(peopleWithOccurrencesOfCoronavirusInfection)));
    assertEquals(HttpStatus.OK, response.getStatusCode());
    String generatedSql = response.getBody().getQuery();
    GeneratedSqlUtils.checkMatchesOrOverwriteGoldenFile(
        generatedSql, "aousynthetic/person-entities-related-to-a-condition.sql");
  }

  @Test
  @DisplayName("example cohort builder breakdown query: cohort=people who've had mammography")
  void generateSqlForPersonEntitiesWithAProcedure() throws IOException {
    // filter for "procedure" entity instances that have concept_id=4324693
    // i.e. the procedure "Mammography"
    ApiFilter mammography =
        new ApiFilter()
            .binaryFilter(
                new ApiBinaryFilter()
                    .attributeVariable(
                        new ApiAttributeVariable().variable("procedure_alias").name("concept_id"))
                    .operator(ApiBinaryFilterOperator.EQUALS)
                    .attributeValue(new ApiAttributeValue().int64Val(4_324_693L)));

    // filter for "procedure_occurrence" entity instances that are related to "procedure" entity
    // instances that have concept_id=4324693
    // i.e. give me all the procedure occurrences of "Mammography"
    ApiFilter occurrencesOfMammography =
        new ApiFilter()
            .relationshipFilter(
                new ApiRelationshipFilter()
                    .outerVariable("procedure_occurrence_alias1")
                    .newVariable("procedure_alias")
                    .newEntity("procedure")
                    .filter(mammography));

    // filter for "person" entity instances that are related to "procedure_occurrence" entity
    // instances that are related to "procedure" entity instances that have concept_id=4324693
    // i.e. give me all the people with procedure occurrences of "Mammography"
    ApiFilter peopleWhoHadMammography =
        new ApiFilter()
            .relationshipFilter(
                new ApiRelationshipFilter()
                    .outerVariable("person_alias")
                    .newVariable("procedure_occurrence_alias1")
                    .newEntity("procedure_occurrence")
                    .filter(occurrencesOfMammography));

    ResponseEntity<ApiSqlQuery> response =
        apiController.generateDatasetSqlQuery(
            UNDERLAY_NAME,
            PERSON_ENTITY,
            new ApiGenerateDatasetSqlQueryRequest()
                .entityDataset(
                    new ApiEntityDataset()
                        .entityVariable("person_alias")
                        .selectedAttributes(ImmutableList.of(PERSON_ID_ATTRIBUTE))
                        .filter(peopleWhoHadMammography)));
    assertEquals(HttpStatus.OK, response.getStatusCode());
    String generatedSql = response.getBody().getQuery();
    GeneratedSqlUtils.checkMatchesOrOverwriteGoldenFile(
        generatedSql, "aousynthetic/person-entities-related-to-a-procedure.sql");
  }

  @Test
  @DisplayName("example cohort builder breakdown query: cohort=people who've had outpatient visit")
  void generateSqlForPersonEntitiesWithAVisit() throws IOException {
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

    ResponseEntity<ApiSqlQuery> response =
        apiController.generateDatasetSqlQuery(
            UNDERLAY_NAME,
            PERSON_ENTITY,
            new ApiGenerateDatasetSqlQueryRequest()
                .entityDataset(
                    new ApiEntityDataset()
                        .entityVariable("person_alias")
                        .selectedAttributes(ImmutableList.of(PERSON_ID_ATTRIBUTE))
                        .filter(peopleWhoHadOutpatientVisit)));
    assertEquals(HttpStatus.OK, response.getStatusCode());
    String generatedSql = response.getBody().getQuery();
    GeneratedSqlUtils.checkMatchesOrOverwriteGoldenFile(
        generatedSql, "aousynthetic/person-entities-related-to-a-visit.sql");
  }
}

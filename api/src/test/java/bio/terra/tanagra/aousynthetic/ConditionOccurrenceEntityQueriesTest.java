package bio.terra.tanagra.aousynthetic;

import static bio.terra.tanagra.aousynthetic.UnderlayUtils.ALL_CONDITION_OCCURRENCE_ATTRIBUTES;
import static bio.terra.tanagra.aousynthetic.UnderlayUtils.CONDITION_OCCURRENCE_ENTITY;
import static bio.terra.tanagra.aousynthetic.UnderlayUtils.UNDERLAY_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;

import bio.terra.tanagra.app.controller.EntityInstancesApiController;
import bio.terra.tanagra.generated.model.ApiArrayFilter;
import bio.terra.tanagra.generated.model.ApiArrayFilterOperator;
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
 * Tests for condition occurrence entity queries on the AoU synthetic underlay. There is no need to
 * specify an active profile for this test, because we want to test the main application definition.
 */
public class ConditionOccurrenceEntityQueriesTest extends BaseSpringUnitTest {
  @Autowired private EntityInstancesApiController apiController;

  @Test
  @DisplayName(
      "correct SQL string for listing condition occurrence entity instances related to people with a condition")
  void generateSqlForConditionOccurrenceEntitiesRelatedToPeopleWithACondition() throws IOException {
    // filter for "condition" entity instances that have concept_id=439676
    // i.e. the condition "Coronavirus infection"
    ApiFilter coronavirusInfection =
        new ApiFilter()
            .binaryFilter(
                new ApiBinaryFilter()
                    .attributeVariable(new ApiAttributeVariable().variable("c").name("concept_id"))
                    .operator(ApiBinaryFilterOperator.EQUALS)
                    .attributeValue(new ApiAttributeValue().int64Val(439_676L)));

    // filter for "person" entity instances that are related to "condition" entity instances that
    // have concept_id=439676
    // i.e. give me all the people with "Coronavirus infection"
    ApiFilter peopleWithCoronavirusInfection =
        new ApiFilter()
            .relationshipFilter(
                new ApiRelationshipFilter()
                    .outerVariable("p")
                    .newVariable("c")
                    .newEntity("condition")
                    .filter(coronavirusInfection));

    // filter for "condition occurrence" entity instances that are related to "person" entity
    // instances that are related to "condition" entity instances that have concept_id=439676
    // i.e. give me all the condition occurrence rows for people with "Coronavirus infection"
    ApiFilter conditionOccurrencesForPeopleWithCoronavirusInfection =
        new ApiFilter()
            .relationshipFilter(
                new ApiRelationshipFilter()
                    .outerVariable("co")
                    .newVariable("p")
                    .newEntity("person")
                    .filter(peopleWithCoronavirusInfection));

    ResponseEntity<ApiSqlQuery> response =
        apiController.generateDatasetSqlQuery(
            UNDERLAY_NAME,
            CONDITION_OCCURRENCE_ENTITY,
            new ApiGenerateDatasetSqlQueryRequest()
                .entityDataset(
                    new ApiEntityDataset()
                        .entityVariable("co")
                        .selectedAttributes(ALL_CONDITION_OCCURRENCE_ATTRIBUTES)
                        .filter(conditionOccurrencesForPeopleWithCoronavirusInfection)));
    assertEquals(HttpStatus.OK, response.getStatusCode());
    String generatedSql = response.getBody().getQuery();
    GeneratedSqlUtils.checkMatchesOrOverwriteGoldenFile(
        generatedSql,
        "aousynthetic/condition-occurrence-entities-related-to-people-with-a-condition.sql",
        ImmutableList.of("condition_person"));
  }

  @Test
  @DisplayName(
      "correct SQL string for listing condition occurrence entity instances related to people with two conditions")
  void generateSqlForConditionOccurrenceEntitiesRelatedToPeopleWithTwoConditions()
      throws IOException {
    // filter for "condition" entity instances that have concept_id=439676
    // i.e. the condition "Coronavirus infection"
    ApiFilter coronavirusInfection =
        new ApiFilter()
            .binaryFilter(
                new ApiBinaryFilter()
                    .attributeVariable(new ApiAttributeVariable().variable("c").name("concept_id"))
                    .operator(ApiBinaryFilterOperator.EQUALS)
                    .attributeValue(new ApiAttributeValue().int64Val(439_676L)));

    // filter for "person" entity instances that are related to "condition" entity instances that
    // have concept_id=439676
    // i.e. give me all the people with "Coronavirus infection"
    ApiFilter peopleWithCoronavirusInfection =
        new ApiFilter()
            .relationshipFilter(
                new ApiRelationshipFilter()
                    .outerVariable("p")
                    .newVariable("c")
                    .newEntity("condition")
                    .filter(coronavirusInfection));

    // filter for "condition" entity instances that have concept_id=132797
    // i.e. the condition "Sepsis"
    ApiFilter sepsis =
        new ApiFilter()
            .binaryFilter(
                new ApiBinaryFilter()
                    .attributeVariable(new ApiAttributeVariable().variable("c").name("concept_id"))
                    .operator(ApiBinaryFilterOperator.EQUALS)
                    .attributeValue(new ApiAttributeValue().int64Val(132_797L)));

    // filter for "person" entity instances that are related to "condition" entity instances that
    // have concept_id=132797
    // i.e. give me all the people with "Sepsis"
    ApiFilter peopleWithSepsis =
        new ApiFilter()
            .relationshipFilter(
                new ApiRelationshipFilter()
                    .outerVariable("p")
                    .newVariable("c")
                    .newEntity("condition")
                    .filter(sepsis));

    // filter for "condition occurrence" entity instances that are related to "person" entity
    // instances that are related to "condition" entity instances with concept_id=439676
    // AND are related to "condition" entity instances with concept_id=132797
    // i.e. give me all the condition occurrence rows for people with "Coronavirus infection" AND
    // "Sepsis"
    ApiFilter conditionOccurrencesForPeopleWithCoronavirusInfectionAndSepsis =
        new ApiFilter()
            .relationshipFilter(
                new ApiRelationshipFilter()
                    .outerVariable("co")
                    .newVariable("p")
                    .newEntity("person")
                    .filter(
                        new ApiFilter()
                            .arrayFilter(
                                new ApiArrayFilter()
                                    .operator(ApiArrayFilterOperator.AND)
                                    .addOperandsItem(peopleWithCoronavirusInfection)
                                    .addOperandsItem(peopleWithSepsis))));

    ResponseEntity<ApiSqlQuery> response =
        apiController.generateDatasetSqlQuery(
            UNDERLAY_NAME,
            CONDITION_OCCURRENCE_ENTITY,
            new ApiGenerateDatasetSqlQueryRequest()
                .entityDataset(
                    new ApiEntityDataset()
                        .entityVariable("co")
                        .selectedAttributes(ALL_CONDITION_OCCURRENCE_ATTRIBUTES)
                        .filter(conditionOccurrencesForPeopleWithCoronavirusInfectionAndSepsis)));
    assertEquals(HttpStatus.OK, response.getStatusCode());
    String generatedSql = response.getBody().getQuery();
    GeneratedSqlUtils.checkMatchesOrOverwriteGoldenFile(
        generatedSql,
        "aousynthetic/condition-occurrence-entities-related-to-people-with-two-conditions.sql",
        ImmutableList.of("condition_person"));
  }
}

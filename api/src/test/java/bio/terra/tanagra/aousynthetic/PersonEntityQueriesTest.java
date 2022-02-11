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

  @Test
  @DisplayName(
      "example cohort builder breakdown query: cohort=people who've had glucose tolerance test")
  void generateSqlForPersonEntitiesWithAMeasurement() throws IOException {
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

    // filter for "measurement_occurrence" entity instances that are related to "measurement" entity
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
    // instances that are related to "measurement" entity instances that have concept_id=3009542
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

    ResponseEntity<ApiSqlQuery> response =
        apiController.generateDatasetSqlQuery(
            UNDERLAY_NAME,
            PERSON_ENTITY,
            new ApiGenerateDatasetSqlQueryRequest()
                .entityDataset(
                    new ApiEntityDataset()
                        .entityVariable("person_alias")
                        .selectedAttributes(ImmutableList.of(PERSON_ID_ATTRIBUTE))
                        .filter(peopleWhoHadHematocrit)));
    assertEquals(HttpStatus.OK, response.getStatusCode());
    String generatedSql = response.getBody().getQuery();
    GeneratedSqlUtils.checkMatchesOrOverwriteGoldenFile(
        generatedSql, "aousynthetic/person-entities-related-to-a-measurement.sql");
  }

  @Test
  @DisplayName("example cohort builder breakdown query: cohort=people who took ibuprofen")
  void generateSqlForPersonEntitiesWithAnIngredient() throws IOException {
    // filter for "ingredient" entity instances that have concept_id=1177480
    // i.e. the ingredient "Ibuprofen"
    ApiFilter ibuprofen =
        new ApiFilter()
            .binaryFilter(
                new ApiBinaryFilter()
                    .attributeVariable(
                        new ApiAttributeVariable().variable("ingredient_alias").name("concept_id"))
                    .operator(ApiBinaryFilterOperator.EQUALS)
                    .attributeValue(new ApiAttributeValue().int64Val(1_177_480L)));

    // filter for "ingredient_occurrence" entity instances that are related to "ingredient" entity
    // instances that have concept_id=1177480
    // i.e. give me all the ingredient occurrences of "Ibuprofen"
    ApiFilter occurrencesOfIbuprofen =
        new ApiFilter()
            .relationshipFilter(
                new ApiRelationshipFilter()
                    .outerVariable("ingredient_occurrence_alias1")
                    .newVariable("ingredient_alias")
                    .newEntity("ingredient")
                    .filter(ibuprofen));

    // filter for "person" entity instances that are related to "ingredient_occurrence" entity
    // instances that are related to "ingredient" entity instances that have concept_id=1177480
    // i.e. give me all the people with ingredient occurrences of "Ibuprofen"
    ApiFilter peopleWhoTookIbuprofen =
        new ApiFilter()
            .relationshipFilter(
                new ApiRelationshipFilter()
                    .outerVariable("person_alias")
                    .newVariable("ingredient_occurrence_alias1")
                    .newEntity("ingredient_occurrence")
                    .filter(occurrencesOfIbuprofen));

    ResponseEntity<ApiSqlQuery> response =
        apiController.generateDatasetSqlQuery(
            UNDERLAY_NAME,
            PERSON_ENTITY,
            new ApiGenerateDatasetSqlQueryRequest()
                .entityDataset(
                    new ApiEntityDataset()
                        .entityVariable("person_alias")
                        .selectedAttributes(ImmutableList.of(PERSON_ID_ATTRIBUTE))
                        .filter(peopleWhoTookIbuprofen)));
    assertEquals(HttpStatus.OK, response.getStatusCode());
    String generatedSql = response.getBody().getQuery();
    GeneratedSqlUtils.checkMatchesOrOverwriteGoldenFile(
        generatedSql, "aousynthetic/person-entities-related-to-an-ingredient.sql");
  }

  @Test
  @DisplayName("example cohort builder breakdown query: cohort=people who refused a vaccine")
  void generateSqlForPersonEntitiesWithAnObservation() throws IOException {
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

    ResponseEntity<ApiSqlQuery> response =
        apiController.generateDatasetSqlQuery(
            UNDERLAY_NAME,
            PERSON_ENTITY,
            new ApiGenerateDatasetSqlQueryRequest()
                .entityDataset(
                    new ApiEntityDataset()
                        .entityVariable("person_alias")
                        .selectedAttributes(ImmutableList.of(PERSON_ID_ATTRIBUTE))
                        .filter(peopleWhoRefusedVaccine)));
    assertEquals(HttpStatus.OK, response.getStatusCode());
    String generatedSql = response.getBody().getQuery();
    GeneratedSqlUtils.checkMatchesOrOverwriteGoldenFile(
        generatedSql, "aousynthetic/person-entities-related-to-an-observation.sql");
  }
}

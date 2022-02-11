package bio.terra.tanagra.aousynthetic;

import static bio.terra.tanagra.aousynthetic.UnderlayUtils.ALL_INGREDIENT_OCCURRENCE_ATTRIBUTES;
import static bio.terra.tanagra.aousynthetic.UnderlayUtils.INGREDIENT_OCCURRENCE_ENTITY;
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
 * Tests for ingredient occurrence entity queries on the AoU synthetic underlay. There is no need to
 * specify an active profile for this test, because we want to test the main application definition.
 */
public class IngredientOccurrenceEntityQueriesTest extends BaseSpringUnitTest {
  @Autowired private EntityInstancesApiController apiController;

  @Test
  @DisplayName(
      "example dataset builder query: cohort=people who've taken ibuprofen, concept set=all ingredients")
  void generateSqlForIngredientOccurrenceEntitiesRelatedToPeopleWithAnIngredient()
      throws IOException {
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

    // filter for "ingredient_occurrence" entity instances that are related to "person" entity
    // instances that are related to "ingredient_occurrence" entity instances that are related to
    // "ingredient" entity instances that have concept_id=1177480
    // i.e. give me all the ingredient occurrence rows for people with "Ibuprofen". this set of rows
    // will include non-ibuprofen ingredient occurrences, such as acetaminophen.
    ApiFilter allIngredientOccurrencesForPeopleWhoTookIbuprofen =
        new ApiFilter()
            .relationshipFilter(
                new ApiRelationshipFilter()
                    .outerVariable("ingredient_occurrence_alias2")
                    .newVariable("person_alias")
                    .newEntity("person")
                    .filter(peopleWhoTookIbuprofen));

    ResponseEntity<ApiSqlQuery> response =
        apiController.generateDatasetSqlQuery(
            UNDERLAY_NAME,
            INGREDIENT_OCCURRENCE_ENTITY,
            new ApiGenerateDatasetSqlQueryRequest()
                .entityDataset(
                    new ApiEntityDataset()
                        .entityVariable("ingredient_occurrence_alias2")
                        .selectedAttributes(ALL_INGREDIENT_OCCURRENCE_ATTRIBUTES)
                        .filter(allIngredientOccurrencesForPeopleWhoTookIbuprofen)));
    assertEquals(HttpStatus.OK, response.getStatusCode());
    String generatedSql = response.getBody().getQuery();
    GeneratedSqlUtils.checkMatchesOrOverwriteGoldenFile(
        generatedSql,
        "aousynthetic/ingredient-occurrence-entities-related-to-people-with-an-ingredient.sql");
  }
}

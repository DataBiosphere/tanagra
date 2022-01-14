package bio.terra.tanagra.aousynthetic;

import static bio.terra.tanagra.aousynthetic.UnderlayUtils.ALL_INGREDIENT_ATTRIBUTES;
import static bio.terra.tanagra.aousynthetic.UnderlayUtils.INGREDIENT_ENTITY;
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
 * Tests for ingredient entity queries on the AoU synthetic underlay. There is no need to specify an
 * active profile for this test, because we want to test the main application definition.
 */
public class IngredientEntityQueriesTest extends BaseSpringUnitTest {
  @Autowired private EntityInstancesApiController apiController;

  @Test
  @DisplayName("correct SQL string for listing all ingredient entity instances")
  void generateSqlForAllIngredientEntities() throws IOException {
    ResponseEntity<ApiSqlQuery> response =
        apiController.generateDatasetSqlQuery(
            UNDERLAY_NAME,
            INGREDIENT_ENTITY,
            new ApiGenerateDatasetSqlQueryRequest()
                .entityDataset(
                    new ApiEntityDataset()
                        .entityVariable("ingredient_alias")
                        .selectedAttributes(ALL_INGREDIENT_ATTRIBUTES)));
    assertEquals(HttpStatus.OK, response.getStatusCode());
    String generatedSql = response.getBody().getQuery();
    GeneratedSqlUtils.checkMatchesOrOverwriteGoldenFile(
        generatedSql, "aousynthetic/all-ingredient-entities.sql");
  }

  @Test
  @DisplayName(
      "correct SQL string for listing all ingredient entity instances related to a single brand entity instance")
  void generateSqlForIngredientEntitiesRelatedToABrandEntity() throws IOException {
    // filter for "brand" entity instances that have concept_id=19082059
    // i.e. give me the brand "Tylenol Chest Congestion"
    ApiBinaryFilter brandIsTylenolChestCongestion =
        new ApiBinaryFilter()
            .attributeVariable(
                new ApiAttributeVariable().variable("brand_alias").name("concept_id"))
            .operator(ApiBinaryFilterOperator.EQUALS)
            .attributeValue(new ApiAttributeValue().int64Val(19_082_059L));

    // filter for "ingredient" entity instances that are related to "brand" entity instances that
    // have
    // concept_id=19082059
    // i.e. give me all the ingredients in "Tylenol Chest Congestion"
    ApiFilter ingredientsInTylenolChestCongestion =
        new ApiFilter()
            .relationshipFilter(
                new ApiRelationshipFilter()
                    .outerVariable("ingredient_alias")
                    .newVariable("brand_alias")
                    .newEntity("brand")
                    .filter(new ApiFilter().binaryFilter(brandIsTylenolChestCongestion)));

    ResponseEntity<ApiSqlQuery> response =
        apiController.generateDatasetSqlQuery(
            UNDERLAY_NAME,
            INGREDIENT_ENTITY,
            new ApiGenerateDatasetSqlQueryRequest()
                .entityDataset(
                    new ApiEntityDataset()
                        .entityVariable("ingredient_alias")
                        .selectedAttributes(ALL_INGREDIENT_ATTRIBUTES)
                        .filter(ingredientsInTylenolChestCongestion)));
    assertEquals(HttpStatus.OK, response.getStatusCode());
    String generatedSql = response.getBody().getQuery();
    GeneratedSqlUtils.checkMatchesOrOverwriteGoldenFile(
        generatedSql,
        "aousynthetic/ingredient-entities-related-to-a-brand.sql",
        "brand_ingredient");
  }
}

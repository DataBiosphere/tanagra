package bio.terra.tanagra.aousynthetic;

import static bio.terra.tanagra.aousynthetic.UnderlayUtils.ALL_INGREDIENT_ATTRIBUTES;
import static bio.terra.tanagra.aousynthetic.UnderlayUtils.INGREDIENT_ENTITY;
import static bio.terra.tanagra.aousynthetic.UnderlayUtils.INGREDIENT_HIERARCHY_NUMCHILDREN_ATTRIBUTE;
import static bio.terra.tanagra.aousynthetic.UnderlayUtils.INGREDIENT_HIERARCHY_PATH_ATTRIBUTE;
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
    // have concept_id=19082059
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
        generatedSql, "aousynthetic/ingredient-entities-related-to-a-brand.sql");
  }

  @Test
  @DisplayName("correct SQL string for listing descendants of a single ingredient entity instance")
  void generateSqlForDescendantsOfAnIngredientEntity() throws IOException {
    // filter for "ingredient" entity instances that are descendants of the "ingredient" entity
    // instance with concept_id=21600360
    // i.e. give me all the descendants of "Other cardiac preparations"
    ApiFilter descendantsOfOtherCardiacPreparations =
        new ApiFilter()
            .binaryFilter(
                new ApiBinaryFilter()
                    .attributeVariable(
                        new ApiAttributeVariable().variable("ingredient_alias").name("concept_id"))
                    .operator(ApiBinaryFilterOperator.DESCENDANT_OF_INCLUSIVE)
                    .attributeValue(new ApiAttributeValue().int64Val(21_600_360L)));

    ResponseEntity<ApiSqlQuery> response =
        apiController.generateDatasetSqlQuery(
            UNDERLAY_NAME,
            INGREDIENT_ENTITY,
            new ApiGenerateDatasetSqlQueryRequest()
                .entityDataset(
                    new ApiEntityDataset()
                        .entityVariable("ingredient_alias")
                        .selectedAttributes(ALL_INGREDIENT_ATTRIBUTES)
                        .filter(descendantsOfOtherCardiacPreparations)));
    assertEquals(HttpStatus.OK, response.getStatusCode());
    String generatedSql = response.getBody().getQuery();
    GeneratedSqlUtils.checkMatchesOrOverwriteGoldenFile(
        generatedSql, "aousynthetic/ingredient-entities-descendants-of-an-ingredient.sql");
  }

  @Test
  @DisplayName("correct SQL string for listing children of a single ingredient entity instance")
  void generateSqlForChildrenOfAnIngredientEntity() throws IOException {
    // filter for "ingredient" entity instances that are children of the "ingredient" entity
    // instance with concept_id=21603396
    // i.e. give me all the children of "Opium alkaloids and derivatives"
    ApiFilter childrenOfOpiumAlkaloids =
        new ApiFilter()
            .binaryFilter(
                new ApiBinaryFilter()
                    .attributeVariable(
                        new ApiAttributeVariable().variable("ingredient_alias").name("concept_id"))
                    .operator(ApiBinaryFilterOperator.CHILD_OF)
                    .attributeValue(new ApiAttributeValue().int64Val(21_603_396L)));

    ResponseEntity<ApiSqlQuery> response =
        apiController.generateDatasetSqlQuery(
            UNDERLAY_NAME,
            INGREDIENT_ENTITY,
            new ApiGenerateDatasetSqlQueryRequest()
                .entityDataset(
                    new ApiEntityDataset()
                        .entityVariable("ingredient_alias")
                        .selectedAttributes(ALL_INGREDIENT_ATTRIBUTES)
                        .filter(childrenOfOpiumAlkaloids)));
    assertEquals(HttpStatus.OK, response.getStatusCode());
    String generatedSql = response.getBody().getQuery();
    GeneratedSqlUtils.checkMatchesOrOverwriteGoldenFile(
        generatedSql, "aousynthetic/ingredient-entities-children-of-an-ingredient.sql");
  }

  @Test
  @DisplayName(
      "correct SQL string for getting the hierarchy attributes (path, numChildren) for a single ingredient entity instance")
  void generateSqlForHierarchyPathOfAnIngredientEntity() throws IOException {
    // filter for "ingredient" entity instances that have concept_id=1784444
    // i.e. the ingredient "Ivermectin"
    ApiFilter ivermectin =
        new ApiFilter()
            .binaryFilter(
                new ApiBinaryFilter()
                    .attributeVariable(
                        new ApiAttributeVariable().variable("ingredient_alias").name("concept_id"))
                    .operator(ApiBinaryFilterOperator.EQUALS)
                    .attributeValue(new ApiAttributeValue().int64Val(1_784_444L)));

    ResponseEntity<ApiSqlQuery> response =
        apiController.generateDatasetSqlQuery(
            UNDERLAY_NAME,
            INGREDIENT_ENTITY,
            new ApiGenerateDatasetSqlQueryRequest()
                .entityDataset(
                    new ApiEntityDataset()
                        .entityVariable("ingredient_alias")
                        .selectedAttributes(
                            ImmutableList.of(
                                INGREDIENT_HIERARCHY_PATH_ATTRIBUTE,
                                INGREDIENT_HIERARCHY_NUMCHILDREN_ATTRIBUTE))
                        .filter(ivermectin)));
    assertEquals(HttpStatus.OK, response.getStatusCode());
    String generatedSql = response.getBody().getQuery();
    GeneratedSqlUtils.checkMatchesOrOverwriteGoldenFile(
        generatedSql, "aousynthetic/ingredient-entity-hierarchy-attributes.sql");
  }

  @Test
  @DisplayName(
      "correct SQL string for listing all ingredient entity instances that are root nodes in the hierarchy")
  void generateSqlForAllRootNodeIngredientEntities() throws IOException {
    // filter for "ingredient" entity instances that have t_path_concept_id = ""
    // i.e. ingredient root nodes
    ApiFilter isRootNode =
        new ApiFilter()
            .binaryFilter(
                new ApiBinaryFilter()
                    .attributeVariable(
                        new ApiAttributeVariable()
                            .variable("ingredient_alias")
                            .name("t_path_concept_id"))
                    .operator(ApiBinaryFilterOperator.EQUALS)
                    .attributeValue(new ApiAttributeValue().stringVal("")));

    ResponseEntity<ApiSqlQuery> response =
        apiController.generateDatasetSqlQuery(
            UNDERLAY_NAME,
            INGREDIENT_ENTITY,
            new ApiGenerateDatasetSqlQueryRequest()
                .entityDataset(
                    new ApiEntityDataset()
                        .entityVariable("ingredient_alias")
                        .selectedAttributes(ALL_INGREDIENT_ATTRIBUTES)
                        .filter(isRootNode)));
    assertEquals(HttpStatus.OK, response.getStatusCode());
    String generatedSql = response.getBody().getQuery();
    GeneratedSqlUtils.checkMatchesOrOverwriteGoldenFile(
        generatedSql, "aousynthetic/ingredient-entities-root-nodes.sql");
  }
}

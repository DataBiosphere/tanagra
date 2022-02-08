package bio.terra.tanagra.aousynthetic;

import static bio.terra.tanagra.aousynthetic.UnderlayUtils.ALL_CONDITION_ATTRIBUTES;
import static bio.terra.tanagra.aousynthetic.UnderlayUtils.CONDITION_ENTITY;
import static bio.terra.tanagra.aousynthetic.UnderlayUtils.CONDITION_HIERARCHY_PATH_ATTRIBUTE;
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
import bio.terra.tanagra.generated.model.ApiTextSearchFilter;
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
 * Tests for condition entity queries on the AoU synthetic underlay. There is no need to specify an
 * active profile for this test, because we want to test the main application definition.
 */
public class ConditionEntityQueriesTest extends BaseSpringUnitTest {
  @Autowired private EntityInstancesApiController apiController;

  @Test
  @DisplayName("correct SQL string for listing all condition entity instances")
  void generateSqlForAllConditionEntities() throws IOException {
    ResponseEntity<ApiSqlQuery> response =
        apiController.generateDatasetSqlQuery(
            UNDERLAY_NAME,
            CONDITION_ENTITY,
            new ApiGenerateDatasetSqlQueryRequest()
                .entityDataset(
                    new ApiEntityDataset()
                        .entityVariable("condition_alias")
                        .selectedAttributes(ALL_CONDITION_ATTRIBUTES)));
    assertEquals(HttpStatus.OK, response.getStatusCode());
    String generatedSql = response.getBody().getQuery();
    GeneratedSqlUtils.checkMatchesOrOverwriteGoldenFile(
        generatedSql, "aousynthetic/all-condition-entities.sql");
  }

  @Test
  @DisplayName("correct SQL string for listing descendants of a single condition entity instance")
  void generateSqlForDescendantsOfAConditionEntity() throws IOException {
    // filter for "condition" entity instances that are descendants of the "condition" entity
    // instance with concept_id=201826
    // i.e. give me all the descendants of "Type 2 diabetes mellitus"
    ApiFilter descendantsOfType2Diabetes =
        new ApiFilter()
            .binaryFilter(
                new ApiBinaryFilter()
                    .attributeVariable(
                        new ApiAttributeVariable().variable("condition_alias").name("concept_id"))
                    .operator(ApiBinaryFilterOperator.DESCENDANT_OF_INCLUSIVE)
                    .attributeValue(new ApiAttributeValue().int64Val(201_826L)));

    ResponseEntity<ApiSqlQuery> response =
        apiController.generateDatasetSqlQuery(
            UNDERLAY_NAME,
            CONDITION_ENTITY,
            new ApiGenerateDatasetSqlQueryRequest()
                .entityDataset(
                    new ApiEntityDataset()
                        .entityVariable("condition_alias")
                        .selectedAttributes(ALL_CONDITION_ATTRIBUTES)
                        .filter(descendantsOfType2Diabetes)));
    assertEquals(HttpStatus.OK, response.getStatusCode());
    String generatedSql = response.getBody().getQuery();
    GeneratedSqlUtils.checkMatchesOrOverwriteGoldenFile(
        generatedSql, "aousynthetic/condition-entities-descendants-of-a-condition.sql");
  }

  @Test
  @DisplayName("correct SQL string for listing children of a single condition entity instance")
  void generateSqlForChildrenOfAConditionEntity() throws IOException {
    // filter for "condition" entity instances that are children of the "condition" entity
    // instance with concept_id=201826
    // i.e. give me all the children of "Type 2 diabetes mellitus"
    ApiFilter childrenOfType2Diabetes =
        new ApiFilter()
            .binaryFilter(
                new ApiBinaryFilter()
                    .attributeVariable(
                        new ApiAttributeVariable().variable("condition_alias").name("concept_id"))
                    .operator(ApiBinaryFilterOperator.CHILD_OF)
                    .attributeValue(new ApiAttributeValue().int64Val(201_826L)));

    ResponseEntity<ApiSqlQuery> response =
        apiController.generateDatasetSqlQuery(
            UNDERLAY_NAME,
            CONDITION_ENTITY,
            new ApiGenerateDatasetSqlQueryRequest()
                .entityDataset(
                    new ApiEntityDataset()
                        .entityVariable("condition_alias")
                        .selectedAttributes(ALL_CONDITION_ATTRIBUTES)
                        .filter(childrenOfType2Diabetes)));
    assertEquals(HttpStatus.OK, response.getStatusCode());
    String generatedSql = response.getBody().getQuery();
    GeneratedSqlUtils.checkMatchesOrOverwriteGoldenFile(
        generatedSql, "aousynthetic/condition-entities-children-of-a-condition.sql");
  }

  @Test
  @DisplayName(
      "correct SQL string for getting the hierarchy path for a single condition entity instance")
  void generateSqlForHierarchyPathOfAConditionEntity() throws IOException {
    // filter for "condition" entity instances that have concept_id=201620
    // i.e. the condition "Kidney stone"
    ApiFilter kidneyStone =
        new ApiFilter()
            .binaryFilter(
                new ApiBinaryFilter()
                    .attributeVariable(
                        new ApiAttributeVariable().variable("condition_alias").name("concept_id"))
                    .operator(ApiBinaryFilterOperator.EQUALS)
                    .attributeValue(new ApiAttributeValue().int64Val(201_620L)));

    ResponseEntity<ApiSqlQuery> response =
        apiController.generateDatasetSqlQuery(
            UNDERLAY_NAME,
            CONDITION_ENTITY,
            new ApiGenerateDatasetSqlQueryRequest()
                .entityDataset(
                    new ApiEntityDataset()
                        .entityVariable("condition_alias")
                        .selectedAttributes(ImmutableList.of(CONDITION_HIERARCHY_PATH_ATTRIBUTE))
                        .filter(kidneyStone)));
    assertEquals(HttpStatus.OK, response.getStatusCode());
    String generatedSql = response.getBody().getQuery();
    GeneratedSqlUtils.checkMatchesOrOverwriteGoldenFile(
        generatedSql, "aousynthetic/hierarchy-path-of-a-condition.sql");
  }

  @Test
  @DisplayName(
      "correct SQL string for listing all condition entity instances that are root nodes in the hierarchy")
  void generateSqlForAllRootNodeConditionEntities() throws IOException {
    // filter for "condition" entity instances that have t_path_concept_id IS NULL
    // i.e. condition root nodes
    ApiFilter isRootNode =
        new ApiFilter()
            .binaryFilter(
                new ApiBinaryFilter()
                    // not setting AttributeValue means to use a null value
                    .attributeVariable(
                        new ApiAttributeVariable()
                            .variable("condition_alias")
                            .name("t_path_concept_id"))
                    .operator(ApiBinaryFilterOperator.EQUALS));

    ResponseEntity<ApiSqlQuery> response =
        apiController.generateDatasetSqlQuery(
            UNDERLAY_NAME,
            CONDITION_ENTITY,
            new ApiGenerateDatasetSqlQueryRequest()
                .entityDataset(
                    new ApiEntityDataset()
                        .entityVariable("condition_alias")
                        .selectedAttributes(ALL_CONDITION_ATTRIBUTES)
                        .filter(isRootNode)));
    assertEquals(HttpStatus.OK, response.getStatusCode());
    String generatedSql = response.getBody().getQuery();
    GeneratedSqlUtils.checkMatchesOrOverwriteGoldenFile(
        generatedSql, "aousynthetic/condition-entities-root-nodes.sql");
  }

  @Test
  @DisplayName(
      "correct SQL string for listing all condition entity instances that match a text search")
  void generateSqlForTextSearchOnConditionEntities() throws IOException {
    // filter for "condition" entity instances that match the search term "sense of smell absent"
    // i.e. conditions that have a name or synonym that includes "sense of smell absent"
    ApiFilter senseOfSmellAbsent =
        new ApiFilter()
            .textSearchFilter(
                new ApiTextSearchFilter()
                    .entityVariable("condition_alias")
                    .term("sense of smell absent"));

    ResponseEntity<ApiSqlQuery> response =
        apiController.generateDatasetSqlQuery(
            UNDERLAY_NAME,
            CONDITION_ENTITY,
            new ApiGenerateDatasetSqlQueryRequest()
                .entityDataset(
                    new ApiEntityDataset()
                        .entityVariable("condition_alias")
                        .selectedAttributes(ALL_CONDITION_ATTRIBUTES)
                        .filter(senseOfSmellAbsent)));
    assertEquals(HttpStatus.OK, response.getStatusCode());
    String generatedSql = response.getBody().getQuery();
    GeneratedSqlUtils.checkMatchesOrOverwriteGoldenFile(
        generatedSql, "aousynthetic/condition-entities-text-search.sql");
  }
}

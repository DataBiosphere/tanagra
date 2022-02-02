package bio.terra.tanagra.aousynthetic;

import static bio.terra.tanagra.aousynthetic.UnderlayUtils.ALL_PROCEDURE_ATTRIBUTES;
import static bio.terra.tanagra.aousynthetic.UnderlayUtils.PROCEDURE_ENTITY;
import static bio.terra.tanagra.aousynthetic.UnderlayUtils.PROCEDURE_HIERARCHY_PATH_ATTRIBUTE;
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
 * Tests for procedure entity queries on the AoU synthetic underlay. There is no need to specify an
 * active profile for this test, because we want to test the main application definition.
 */
public class ProcedureEntityQueriesTest extends BaseSpringUnitTest {
  @Autowired private EntityInstancesApiController apiController;

  @Test
  @DisplayName("correct SQL string for listing all procedure entity instances")
  void generateSqlForAllProcedureEntities() throws IOException {
    ResponseEntity<ApiSqlQuery> response =
        apiController.generateDatasetSqlQuery(
            UNDERLAY_NAME,
            PROCEDURE_ENTITY,
            new ApiGenerateDatasetSqlQueryRequest()
                .entityDataset(
                    new ApiEntityDataset()
                        .entityVariable("procedure_alias")
                        .selectedAttributes(ALL_PROCEDURE_ATTRIBUTES)));
    assertEquals(HttpStatus.OK, response.getStatusCode());
    String generatedSql = response.getBody().getQuery();
    GeneratedSqlUtils.checkMatchesOrOverwriteGoldenFile(
        generatedSql, "aousynthetic/all-procedure-entities.sql");
  }

  @Test
  @DisplayName("correct SQL string for listing descendants of a single procedure entity instance")
  void generateSqlForDescendantsOfAProcedureEntity() throws IOException {
    // filter for "procedure" entity instances that are descendants of the "procedure" entity
    // instance with concept_id=4176720
    // i.e. give me all the descendants of "Viral immunization"
    ApiFilter descendantsOfViralImmunization =
        new ApiFilter()
            .binaryFilter(
                new ApiBinaryFilter()
                    .attributeVariable(
                        new ApiAttributeVariable().variable("procedure_alias").name("concept_id"))
                    .operator(ApiBinaryFilterOperator.DESCENDANT_OF_INCLUSIVE)
                    .attributeValue(new ApiAttributeValue().int64Val(4_176_720L)));

    ResponseEntity<ApiSqlQuery> response =
        apiController.generateDatasetSqlQuery(
            UNDERLAY_NAME,
            PROCEDURE_ENTITY,
            new ApiGenerateDatasetSqlQueryRequest()
                .entityDataset(
                    new ApiEntityDataset()
                        .entityVariable("procedure_alias")
                        .selectedAttributes(ALL_PROCEDURE_ATTRIBUTES)
                        .filter(descendantsOfViralImmunization)));
    assertEquals(HttpStatus.OK, response.getStatusCode());
    String generatedSql = response.getBody().getQuery();
    GeneratedSqlUtils.checkMatchesOrOverwriteGoldenFile(
        generatedSql, "aousynthetic/procedure-entities-descendants-of-a-procedure.sql");
  }

  @Test
  @DisplayName("correct SQL string for listing children of a single procedure entity instance")
  void generateSqlForChildrenOfAProcedureEntity() throws IOException {
    // filter for "procedure" entity instances that are children of the "procedure" entity
    // instance with concept_id=4179181
    // i.e. give me all the children of "Mumps vaccination"
    ApiFilter childrenOfMumpsVaccination =
        new ApiFilter()
            .binaryFilter(
                new ApiBinaryFilter()
                    .attributeVariable(
                        new ApiAttributeVariable().variable("procedure_alias").name("concept_id"))
                    .operator(ApiBinaryFilterOperator.CHILD_OF)
                    .attributeValue(new ApiAttributeValue().int64Val(4_179_181L)));

    ResponseEntity<ApiSqlQuery> response =
        apiController.generateDatasetSqlQuery(
            UNDERLAY_NAME,
            PROCEDURE_ENTITY,
            new ApiGenerateDatasetSqlQueryRequest()
                .entityDataset(
                    new ApiEntityDataset()
                        .entityVariable("procedure_alias")
                        .selectedAttributes(ALL_PROCEDURE_ATTRIBUTES)
                        .filter(childrenOfMumpsVaccination)));
    assertEquals(HttpStatus.OK, response.getStatusCode());
    String generatedSql = response.getBody().getQuery();
    GeneratedSqlUtils.checkMatchesOrOverwriteGoldenFile(
        generatedSql, "aousynthetic/procedure-entities-children-of-a-procedure.sql");
  }

  @Test
  @DisplayName(
      "correct SQL string for getting the hierarchy path for a single procedure entity instance")
  void generateSqlForHierarchyPathOfAProcedureEntity() throws IOException {
    // filter for "procedure" entity instances that have concept_id=4198190
    // i.e. the procedure "Appendectomy"
    ApiFilter appendectomy =
        new ApiFilter()
            .binaryFilter(
                new ApiBinaryFilter()
                    .attributeVariable(
                        new ApiAttributeVariable().variable("procedure_alias").name("concept_id"))
                    .operator(ApiBinaryFilterOperator.EQUALS)
                    .attributeValue(new ApiAttributeValue().int64Val(4_198_190L)));

    ResponseEntity<ApiSqlQuery> response =
        apiController.generateDatasetSqlQuery(
            UNDERLAY_NAME,
            PROCEDURE_ENTITY,
            new ApiGenerateDatasetSqlQueryRequest()
                .entityDataset(
                    new ApiEntityDataset()
                        .entityVariable("procedure_alias")
                        .selectedAttributes(ImmutableList.of(PROCEDURE_HIERARCHY_PATH_ATTRIBUTE))
                        .filter(appendectomy)));
    assertEquals(HttpStatus.OK, response.getStatusCode());
    String generatedSql = response.getBody().getQuery();
    GeneratedSqlUtils.checkMatchesOrOverwriteGoldenFile(
        generatedSql, "aousynthetic/hierarchy-path-of-a-procedure.sql");
  }

  @Test
  @DisplayName(
      "correct SQL string for listing all procedure entity instances that are root nodes in the hierarchy")
  void generateSqlForAllRootNodeProcedureEntities() throws IOException {
    // filter for "procedure" entity instances that have t_path_concept_id IS NULL
    // i.e. procedure root nodes
    ApiFilter isRootNode =
        new ApiFilter()
            .binaryFilter(
                new ApiBinaryFilter()
                    // not setting AttributeValue means to use a null value
                    .attributeVariable(
                        new ApiAttributeVariable()
                            .variable("procedure_alias")
                            .name("t_path_concept_id"))
                    .operator(ApiBinaryFilterOperator.EQUALS));

    ResponseEntity<ApiSqlQuery> response =
        apiController.generateDatasetSqlQuery(
            UNDERLAY_NAME,
            PROCEDURE_ENTITY,
            new ApiGenerateDatasetSqlQueryRequest()
                .entityDataset(
                    new ApiEntityDataset()
                        .entityVariable("procedure_alias")
                        .selectedAttributes(ALL_PROCEDURE_ATTRIBUTES)
                        .filter(isRootNode)));
    assertEquals(HttpStatus.OK, response.getStatusCode());
    String generatedSql = response.getBody().getQuery();
    GeneratedSqlUtils.checkMatchesOrOverwriteGoldenFile(
        generatedSql, "aousynthetic/procedure-entities-root-nodes.sql");
  }
}

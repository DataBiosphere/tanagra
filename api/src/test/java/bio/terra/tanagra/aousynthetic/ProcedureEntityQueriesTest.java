package bio.terra.tanagra.aousynthetic;

import static bio.terra.tanagra.aousynthetic.UnderlayUtils.ALL_PROCEDURE_ATTRIBUTES;
import static bio.terra.tanagra.aousynthetic.UnderlayUtils.PROCEDURE_ENTITY;
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
}

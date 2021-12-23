package bio.terra.tanagra.aousynthetic;

import static bio.terra.tanagra.aousynthetic.UnderlayUtils.ALL_PROCEDURE_ATTRIBUTES;
import static bio.terra.tanagra.aousynthetic.UnderlayUtils.BQ_DATASET_SQL_REFERENCE;
import static bio.terra.tanagra.aousynthetic.UnderlayUtils.PROCEDURE_ENTITY;
import static bio.terra.tanagra.aousynthetic.UnderlayUtils.UNDERLAY_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;

import bio.terra.tanagra.app.controller.EntityInstancesApiController;
import bio.terra.tanagra.generated.model.ApiEntityDataset;
import bio.terra.tanagra.generated.model.ApiGenerateDatasetSqlQueryRequest;
import bio.terra.tanagra.generated.model.ApiSqlQuery;
import bio.terra.tanagra.testing.BaseSpringUnitTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Tests for procedure entity queries on the AoU synthetic underlay. There is no need to specify an
 * active profile for this test, because we want to test the main application definition.
 */
public class ProcedureEntityQueries extends BaseSpringUnitTest {
  private static final Logger LOG = LoggerFactory.getLogger(ProcedureEntityQueries.class);
  @Autowired private EntityInstancesApiController apiController;

  @Test
  @DisplayName("correct SQL string for listing all procedure entity instances")
  void generateSqlForAllProcedureEntities() {
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
    LOG.info(generatedSql);
    assertEquals(
        "SELECT procedure_alias.concept_id AS concept_id, "
            + "procedure_alias.concept_name AS concept_name, "
            + "procedure_alias.vocabulary_id AS vocabulary_id, "
            + "(SELECT vocabulary.vocabulary_name FROM `"
            + BQ_DATASET_SQL_REFERENCE
            + "`.vocabulary WHERE vocabulary.vocabulary_id = procedure_alias.vocabulary_id) AS vocabulary_name, "
            + "procedure_alias.standard_concept AS standard_concept, "
            + "procedure_alias.concept_code AS concept_code "
            + "FROM (SELECT * FROM `"
            + BQ_DATASET_SQL_REFERENCE
            + "`.concept WHERE domain_id = 'Procedure') AS procedure_alias "
            + "WHERE TRUE",
        generatedSql);
  }
}

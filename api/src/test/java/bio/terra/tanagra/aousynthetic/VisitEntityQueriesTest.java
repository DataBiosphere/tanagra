package bio.terra.tanagra.aousynthetic;

import static bio.terra.tanagra.aousynthetic.UnderlayUtils.ALL_VISIT_ATTRIBUTES;
import static bio.terra.tanagra.aousynthetic.UnderlayUtils.BQ_DATASET_SQL_REFERENCE;
import static bio.terra.tanagra.aousynthetic.UnderlayUtils.UNDERLAY_NAME;
import static bio.terra.tanagra.aousynthetic.UnderlayUtils.VISIT_ENTITY;
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
 * Tests for visit entity queries on the AoU synthetic underlay. There is no need to specify an
 * active profile for this test, because we want to test the main application definition.
 */
public class VisitEntityQueriesTest extends BaseSpringUnitTest {
  private static final Logger LOG = LoggerFactory.getLogger(VisitEntityQueriesTest.class);
  @Autowired private EntityInstancesApiController apiController;

  @Test
  @DisplayName("correct SQL string for listing all visit entity instances")
  void generateSqlForAllVisitEntities() {
    ResponseEntity<ApiSqlQuery> response =
        apiController.generateDatasetSqlQuery(
            UNDERLAY_NAME,
            VISIT_ENTITY,
            new ApiGenerateDatasetSqlQueryRequest()
                .entityDataset(
                    new ApiEntityDataset()
                        .entityVariable("visit_alias")
                        .selectedAttributes(ALL_VISIT_ATTRIBUTES)));
    assertEquals(HttpStatus.OK, response.getStatusCode());
    String generatedSql = response.getBody().getQuery();
    LOG.info(generatedSql);
    assertEquals(
        "SELECT visit_alias.concept_id AS concept_id, "
            + "visit_alias.concept_name AS concept_name "
            + "FROM (SELECT * FROM `"
            + BQ_DATASET_SQL_REFERENCE
            + "`.concept WHERE vocabulary_id = 'Visit') AS visit_alias "
            + "WHERE TRUE",
        generatedSql);
  }
}

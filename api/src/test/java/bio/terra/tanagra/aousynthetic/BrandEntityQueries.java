package bio.terra.tanagra.aousynthetic;

import static bio.terra.tanagra.aousynthetic.UnderlayUtils.ALL_BRAND_ATTRIBUTES;
import static bio.terra.tanagra.aousynthetic.UnderlayUtils.BQ_DATASET_SQL_REFERENCE;
import static bio.terra.tanagra.aousynthetic.UnderlayUtils.BRAND_ENTITY;
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
 * Tests for brand entity queries on the AoU synthetic underlay. There is no need to specify an
 * active profile for this test, because we want to test the main application definition.
 */
public class BrandEntityQueries extends BaseSpringUnitTest {
  private static final Logger LOG = LoggerFactory.getLogger(BrandEntityQueries.class);
  @Autowired private EntityInstancesApiController apiController;

  @Test
  @DisplayName("correct SQL string for listing all brand entity instances")
  void generateSqlForAllBrandEntities() {
    ResponseEntity<ApiSqlQuery> response =
        apiController.generateDatasetSqlQuery(
            UNDERLAY_NAME,
            BRAND_ENTITY,
            new ApiGenerateDatasetSqlQueryRequest()
                .entityDataset(
                    new ApiEntityDataset()
                        .entityVariable("brand_alias")
                        .selectedAttributes(ALL_BRAND_ATTRIBUTES)));
    assertEquals(HttpStatus.OK, response.getStatusCode());
    String generatedSql = response.getBody().getQuery();
    LOG.info(generatedSql);
    assertEquals(
        "SELECT brand_alias.concept_id AS concept_id, "
            + "brand_alias.concept_name AS concept_name, "
            + "brand_alias.standard_concept AS standard_concept, "
            + "brand_alias.concept_code AS concept_code "
            + "FROM (SELECT * FROM `"
            + BQ_DATASET_SQL_REFERENCE
            + "`.concept WHERE concept_class_id = 'Brand Name') AS brand_alias "
            + "WHERE TRUE",
        generatedSql);
  }
}

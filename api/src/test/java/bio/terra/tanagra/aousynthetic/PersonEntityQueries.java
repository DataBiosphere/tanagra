package bio.terra.tanagra.aousynthetic;

import static bio.terra.tanagra.aousynthetic.UnderlayUtils.ALL_PERSON_ATTRIBUTES;
import static bio.terra.tanagra.aousynthetic.UnderlayUtils.BQ_DATASET_SQL_REFERENCE;
import static bio.terra.tanagra.aousynthetic.UnderlayUtils.PERSON_ENTITY;
import static bio.terra.tanagra.aousynthetic.UnderlayUtils.UNDERLAY_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;

import bio.terra.tanagra.app.controller.EntityInstancesApiController;
import bio.terra.tanagra.generated.model.ApiEntityDataset;
import bio.terra.tanagra.generated.model.ApiGenerateDatasetSqlQueryRequest;
import bio.terra.tanagra.generated.model.ApiSqlQuery;
import bio.terra.tanagra.testing.BaseSpringUnitTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Tests for person entity queries on the AoU synthetic underlay. There is no need to specify an
 * active profile for this test, because we want to test the main application definition.
 */
public class PersonEntityQueries extends BaseSpringUnitTest {
  @Autowired private EntityInstancesApiController apiController;

  @Test
  @DisplayName("correct SQL string for listing all person entity instances")
  void generateSqlForAllPersonEntities() {
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
    assertEquals(
        "SELECT person_alias.person_id AS person_id, "
            + "person_alias.gender_concept_id AS gender_concept_id, "
            + "(SELECT concept.concept_name FROM `"
            + BQ_DATASET_SQL_REFERENCE
            + "`.concept WHERE concept.concept_id = person_alias.gender_concept_id) AS gender, "
            + "person_alias.race_concept_id AS race_concept_id, "
            + "(SELECT concept.concept_name FROM `"
            + BQ_DATASET_SQL_REFERENCE
            + "`.concept WHERE concept.concept_id = person_alias.race_concept_id) AS race, "
            + "person_alias.ethnicity_concept_id AS ethnicity_concept_id, "
            + "(SELECT concept.concept_name FROM `"
            + BQ_DATASET_SQL_REFERENCE
            + "`.concept WHERE concept.concept_id = person_alias.ethnicity_concept_id) AS ethnicity, "
            + "person_alias.sex_at_birth_concept_id AS sex_at_birth_concept_id, "
            + "(SELECT concept.concept_name FROM `"
            + BQ_DATASET_SQL_REFERENCE
            + "`.concept WHERE concept.concept_id = person_alias.sex_at_birth_concept_id) AS sex_at_birth "
            + "FROM `"
            + BQ_DATASET_SQL_REFERENCE
            + "`.person AS person_alias "
            + "WHERE TRUE",
        generatedSql);
  }
}

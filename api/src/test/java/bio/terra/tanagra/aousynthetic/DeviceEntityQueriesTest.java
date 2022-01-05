package bio.terra.tanagra.aousynthetic;

import static bio.terra.tanagra.aousynthetic.UnderlayUtils.ALL_DEVICE_ATTRIBUTES;
import static bio.terra.tanagra.aousynthetic.UnderlayUtils.DEVICE_ENTITY;
import static bio.terra.tanagra.aousynthetic.UnderlayUtils.UNDERLAY_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;

import bio.terra.tanagra.app.controller.EntityInstancesApiController;
import bio.terra.tanagra.generated.model.ApiEntityDataset;
import bio.terra.tanagra.generated.model.ApiGenerateDatasetSqlQueryRequest;
import bio.terra.tanagra.generated.model.ApiSqlQuery;
import bio.terra.tanagra.testing.BaseSpringUnitTest;
import java.io.IOException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Tests for device entity queries on the AoU synthetic underlay. There is no need to specify an
 * active profile for this test, because we want to test the main application definition.
 */
public class DeviceEntityQueriesTest extends BaseSpringUnitTest {
  @Autowired private EntityInstancesApiController apiController;

  @Test
  @DisplayName("correct SQL string for listing all device entity instances")
  void generateSqlForAllDeviceEntities() throws IOException {
    ResponseEntity<ApiSqlQuery> response =
        apiController.generateDatasetSqlQuery(
            UNDERLAY_NAME,
            DEVICE_ENTITY,
            new ApiGenerateDatasetSqlQueryRequest()
                .entityDataset(
                    new ApiEntityDataset()
                        .entityVariable("device_alias")
                        .selectedAttributes(ALL_DEVICE_ATTRIBUTES)));
    assertEquals(HttpStatus.OK, response.getStatusCode());
    String generatedSql = response.getBody().getQuery();
    GeneratedSqlUtils.checkMatchesOrOverwriteGoldenFile(generatedSql, "all-device-entities.sql");
  }
}

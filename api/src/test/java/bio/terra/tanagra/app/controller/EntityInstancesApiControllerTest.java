package bio.terra.tanagra.app.controller;

import static bio.terra.tanagra.service.underlay.NauticalUnderlayUtils.NAUTICAL_UNDERLAY_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;

import bio.terra.tanagra.generated.model.ApiAttributeValue;
import bio.terra.tanagra.generated.model.ApiAttributeVariable;
import bio.terra.tanagra.generated.model.ApiBinaryFilter;
import bio.terra.tanagra.generated.model.ApiBinaryFilterOperator;
import bio.terra.tanagra.generated.model.ApiEntityDataset;
import bio.terra.tanagra.generated.model.ApiFilter;
import bio.terra.tanagra.generated.model.ApiGenerateDatasetSqlQueryRequest;
import bio.terra.tanagra.generated.model.ApiSqlQuery;
import bio.terra.tanagra.testing.BaseSpringUnitTest;
import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("nautical")
public class EntityInstancesApiControllerTest extends BaseSpringUnitTest {
  @Autowired private EntityInstancesApiController controller;

  @Test
  void generateDatasetSqlQuery() {
    ResponseEntity<ApiSqlQuery> response =
        controller.generateDatasetSqlQuery(
            NAUTICAL_UNDERLAY_NAME,
            "sailors",
            new ApiGenerateDatasetSqlQueryRequest()
                .entityDataset(
                    new ApiEntityDataset()
                        .entityVariable("s")
                        .selectedAttributes(ImmutableList.of("name", "rating"))
                        .filter(
                            new ApiFilter()
                                .binaryFilter(
                                    new ApiBinaryFilter()
                                        .attributeVariable(
                                            new ApiAttributeVariable().variable("s").name("rating"))
                                        .operator(ApiBinaryFilterOperator.EQUALS)
                                        .attributeValue(new ApiAttributeValue().int64Val(42L))))));
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(
        "SELECT s.s_name AS name, s.rating AS rating "
            + "FROM `my-project-id.nautical`.sailors AS s WHERE s.rating = 42",
        response.getBody().getQuery());
  }

  @Test
  void generateDatasetWithOrderBySqlQuery() {
    ResponseEntity<ApiSqlQuery> response =
        controller.generateDatasetSqlQuery(
            NAUTICAL_UNDERLAY_NAME,
            "sailors",
            new ApiGenerateDatasetSqlQueryRequest()
                .entityDataset(
                    new ApiEntityDataset()
                        .entityVariable("s")
                        .selectedAttributes(ImmutableList.of("name", "rating"))
                        .orderByAttribute("name")
                        .filter(
                            new ApiFilter()
                                .binaryFilter(
                                    new ApiBinaryFilter()
                                        .attributeVariable(
                                            new ApiAttributeVariable().variable("s").name("rating"))
                                        .operator(ApiBinaryFilterOperator.EQUALS)
                                        .attributeValue(new ApiAttributeValue().int64Val(42L))))));
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(
        "SELECT s.s_name AS name, s.rating AS rating "
            + "FROM `my-project-id.nautical`.sailors AS s WHERE s.rating = 42 "
            + "ORDER BY name",
        response.getBody().getQuery());
  }
}

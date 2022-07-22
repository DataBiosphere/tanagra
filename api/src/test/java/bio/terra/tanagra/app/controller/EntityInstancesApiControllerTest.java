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
import bio.terra.tanagra.generated.model.ApiOrderByDirection;
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
  void generateDatasetWithOrderBySimpleColumnSqlQuery() {
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
                        .orderByDirection(ApiOrderByDirection.DESC)
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
            + "ORDER BY s.s_name DESC",
        response.getBody().getQuery());
  }

  @Test
  void generateDatasetWithOrderByLookupColumnSqlQuery() {
    ResponseEntity<ApiSqlQuery> response =
        controller.generateDatasetSqlQuery(
            NAUTICAL_UNDERLAY_NAME,
            "boats",
            new ApiGenerateDatasetSqlQueryRequest()
                .entityDataset(
                    new ApiEntityDataset()
                        .entityVariable("b")
                        .selectedAttributes(ImmutableList.of("name", "color"))
                        .orderByAttribute("type_name")
                        .orderByDirection(ApiOrderByDirection.ASC)
                        .filter(
                            new ApiFilter()
                                .binaryFilter(
                                    new ApiBinaryFilter()
                                        .attributeVariable(
                                            new ApiAttributeVariable().variable("b").name("color"))
                                        .operator(ApiBinaryFilterOperator.EQUALS)
                                        .attributeValue(
                                            new ApiAttributeValue().stringVal("red"))))));
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(
        "SELECT b.b_name AS name, b.color AS color "
            + "FROM `my-project-id.nautical`.boats AS b WHERE b.color = 'red' "
            + "ORDER BY (SELECT boat_types.bt_name FROM `my-project-id.nautical`.boat_types WHERE boat_types.bt_id = b.bt_id) ASC",
        response.getBody().getQuery());
  }

  void generateDatasetSqlQueryWithLimit() {
    ResponseEntity<ApiSqlQuery> response =
            controller.generateDatasetSqlQuery(
                    NAUTICAL_UNDERLAY_NAME,
                    "sailors",
                    new ApiGenerateDatasetSqlQueryRequest()
                            .entityDataset(
                                    new ApiEntityDataset()
                                            .entityVariable("s")
                                            .selectedAttributes(ImmutableList.of("name", "rating"))
                                            .limit(1)
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
                    + "FROM `my-project-id.nautical`.sailors AS s WHERE s.rating = 42 LIMIT = 1",
            response.getBody().getQuery());
  }
}

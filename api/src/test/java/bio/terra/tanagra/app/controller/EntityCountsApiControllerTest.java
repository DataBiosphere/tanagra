package bio.terra.tanagra.app.controller;

import static bio.terra.tanagra.service.underlay.NauticalUnderlayUtils.NAUTICAL_UNDERLAY_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;

import bio.terra.tanagra.generated.model.ApiAttributeValue;
import bio.terra.tanagra.generated.model.ApiAttributeVariable;
import bio.terra.tanagra.generated.model.ApiBinaryFilter;
import bio.terra.tanagra.generated.model.ApiBinaryFilterOperator;
import bio.terra.tanagra.generated.model.ApiEntityCounts;
import bio.terra.tanagra.generated.model.ApiFilter;
import bio.terra.tanagra.generated.model.ApiGenerateCountsSqlQueryRequest;
import bio.terra.tanagra.generated.model.ApiSqlQuery;
import bio.terra.tanagra.testing.BaseSpringUnitTest;
import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("nautical")
public class EntityCountsApiControllerTest extends BaseSpringUnitTest {
  @Autowired private EntityCountsApiController controller;

  @Test
  void generateCountsSqlQuery() {
    ResponseEntity<ApiSqlQuery> response =
        controller.generateCountsSqlQuery(
            NAUTICAL_UNDERLAY_NAME,
            "boats",
            new ApiGenerateCountsSqlQueryRequest()
                .entityCounts(
                    new ApiEntityCounts()
                        .entityVariable("b")
                        .additionalSelectedAttributes(ImmutableList.of("type_name"))
                        .groupByAttributes(ImmutableList.of("type_id", "color"))
                        .filter(
                            new ApiFilter()
                                .binaryFilter(
                                    new ApiBinaryFilter()
                                        .attributeVariable(
                                            new ApiAttributeVariable().variable("b").name("id"))
                                        .operator(ApiBinaryFilterOperator.GREATER_THAN)
                                        .attributeValue(new ApiAttributeValue().int64Val(500L))))));
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(
        "SELECT COUNT(b) AS t_count, b.bt_id AS type_id, b.color AS color, "
            + "(SELECT boat_types.bt_name FROM `my-project-id.nautical`.boat_types WHERE boat_types.bt_id = b.bt_id) AS type_name "
            + "FROM `my-project-id.nautical`.boats AS b WHERE b.b_id > 500 "
            + "GROUP BY b.bt_id, b.color",
        response.getBody().getQuery());
  }

  @Test
  void generateCountsSqlQueryWithNoAdditionalFields() {
    ResponseEntity<ApiSqlQuery> response =
        controller.generateCountsSqlQuery(
            NAUTICAL_UNDERLAY_NAME,
            "boats",
            new ApiGenerateCountsSqlQueryRequest()
                .entityCounts(
                    new ApiEntityCounts()
                        .entityVariable("b")
                        .groupByAttributes(ImmutableList.of("type_id"))
                        .filter(
                            new ApiFilter()
                                .binaryFilter(
                                    new ApiBinaryFilter()
                                        .attributeVariable(
                                            new ApiAttributeVariable().variable("b").name("color"))
                                        .operator(ApiBinaryFilterOperator.EQUALS)
                                        .attributeValue(
                                            new ApiAttributeValue().stringVal("green"))))));
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(
        "SELECT COUNT(b) AS t_count, b.bt_id AS type_id "
            + "FROM `my-project-id.nautical`.boats AS b WHERE b.color = 'green' "
            + "GROUP BY b.bt_id",
        response.getBody().getQuery());
  }

  @Test
  void generateCountsSqlQueryWithNoGroupBy() {
    ResponseEntity<ApiSqlQuery> response =
        controller.generateCountsSqlQuery(
            NAUTICAL_UNDERLAY_NAME,
            "boats",
            new ApiGenerateCountsSqlQueryRequest()
                .entityCounts(new ApiEntityCounts().entityVariable("b")));
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(
        "SELECT COUNT(b) AS t_count " + "FROM `my-project-id.nautical`.boats AS b WHERE TRUE",
        response.getBody().getQuery());
  }
}

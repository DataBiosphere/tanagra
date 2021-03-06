package bio.terra.tanagra.app.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;

import bio.terra.tanagra.generated.model.*;
import bio.terra.tanagra.service.underlay.NauticalUnderlayUtils;
import bio.terra.tanagra.testing.BaseSpringUnitTest;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("nautical")
public class EntitiesFilterApiControllerTest extends BaseSpringUnitTest {
  @Autowired private EntitiesFiltersApiController apiController;

  @Test
  void generateSqlQueryBinaryFilter() {
    ApiEntityFilter apiEntityFilter =
        new ApiEntityFilter()
            .entityVariable("s")
            .filter(
                new ApiFilter()
                    .binaryFilter(
                        new ApiBinaryFilter()
                            .attributeVariable(
                                new ApiAttributeVariable().variable("s").name("rating"))
                            .operator(ApiBinaryFilterOperator.EQUALS)
                            .attributeValue(new ApiAttributeValue().int64Val(42L))));

    ResponseEntity<ApiSqlQuery> response =
        apiController.generateSqlQuery(
            NauticalUnderlayUtils.NAUTICAL_UNDERLAY_NAME, "sailors", apiEntityFilter);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(
        "SELECT s.s_id AS primary_key FROM `my-project-id.nautical`.sailors AS s "
            + "WHERE s.rating = 42",
        response.getBody().getQuery());
  }

  @Test
  @DisplayName(
      "correct SQL string for filtering entity instances based on a binary filter with the CHILD_OF operator")
  void generateSqlQueryBinaryFilterChildOf() {
    // filter for "boats" entity instances that have a type that is a child of type 423
    // i.e. say sailboat = boat type 423. give me all the boats that are an immediate sub-type of
    // sailboat
    ApiEntityFilter apiEntityFilter =
        new ApiEntityFilter()
            .entityVariable("boat")
            .filter(
                new ApiFilter()
                    .binaryFilter(
                        new ApiBinaryFilter()
                            .attributeVariable(
                                new ApiAttributeVariable().variable("boat").name("type_id"))
                            .operator(ApiBinaryFilterOperator.CHILD_OF)
                            .attributeValue(new ApiAttributeValue().int64Val(423L))));

    ResponseEntity<ApiSqlQuery> response =
        apiController.generateSqlQuery(
            NauticalUnderlayUtils.NAUTICAL_UNDERLAY_NAME, "boats", apiEntityFilter);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(
        "SELECT boat.b_id AS primary_key FROM `my-project-id.nautical`.boats AS boat "
            + "WHERE boat.bt_id IN (SELECT btc_child FROM "
            + "(SELECT * FROM `my-project-id.nautical`.boat_types_children WHERE btc_is_expired = 'false') "
            + "WHERE btc_parent = 423)",
        response.getBody().getQuery());
  }

  @Test
  @DisplayName(
      "correct SQL string for filtering entity instances based on a relationship where the entity=entity1")
  void generateSqlQueryRelationshipFilterForEntity1() {
    // filter for "boats" entity instances that have color=red
    // i.e. give me all the boats that are red
    ApiBinaryFilter boatsThatAreRed =
        new ApiBinaryFilter()
            .attributeVariable(new ApiAttributeVariable().variable("boat").name("color"))
            .operator(ApiBinaryFilterOperator.EQUALS)
            .attributeValue(new ApiAttributeValue().stringVal("red"));

    // filter for "sailors" entity instances that are related to "boats" entity instances that have
    // color=red
    // i.e. give me all the sailors with a favorite boat that is red
    ApiEntityFilter apiEntityFilter =
        new ApiEntityFilter()
            .entityVariable("sailor")
            .filter(
                new ApiFilter()
                    .relationshipFilter(
                        new ApiRelationshipFilter()
                            .outerVariable("sailor")
                            .newVariable("boat")
                            .newEntity("boats")
                            .filter(new ApiFilter().binaryFilter(boatsThatAreRed))));

    ResponseEntity<ApiSqlQuery> response =
        apiController.generateSqlQuery(
            NauticalUnderlayUtils.NAUTICAL_UNDERLAY_NAME, "sailors", apiEntityFilter);
    assertEquals(HttpStatus.OK, response.getStatusCode());

    String actualSql = response.getBody().getQuery();
    String expectedSql =
        "SELECT sailor.s_id AS primary_key FROM `my-project-id.nautical`.sailors AS sailor "
            + "WHERE sailor.s_id IN "
            + "(SELECT sailor_boat1329273297.s_id FROM `my-project-id.nautical`.sailors_favorite_boats AS sailor_boat1329273297 "
            + "WHERE sailor_boat1329273297.b_id IN ("
            + "SELECT boat.b_id FROM `my-project-id.nautical`.boats AS boat WHERE boat.color = 'red'))";
    assertEquals(expectedSql, actualSql);
  }

  @Test
  @DisplayName(
      "correct SQL string for filtering entity instances based on a relationship where the entity=entity2")
  void generateSqlQueryRelationshipFilterForEntity2() {
    // filter for "sailors" entity instances that have name=Jim
    // i.e. give me all the sailors named Jim
    ApiBinaryFilter sailorsNamedJim =
        new ApiBinaryFilter()
            .attributeVariable(new ApiAttributeVariable().variable("sailor").name("name"))
            .operator(ApiBinaryFilterOperator.EQUALS)
            .attributeValue(new ApiAttributeValue().stringVal("Jim"));

    // filter for "boats" entity instances that are related to "sailors" entity instances that have
    // name=Jim
    // i.e. give me all the favorite boats for sailors named Jim
    ApiEntityFilter apiEntityFilter =
        new ApiEntityFilter()
            .entityVariable("boat")
            .filter(
                new ApiFilter()
                    .relationshipFilter(
                        new ApiRelationshipFilter()
                            .outerVariable("boat")
                            .newVariable("sailor")
                            .newEntity("sailors")
                            .filter(new ApiFilter().binaryFilter(sailorsNamedJim))));

    ResponseEntity<ApiSqlQuery> response =
        apiController.generateSqlQuery(
            NauticalUnderlayUtils.NAUTICAL_UNDERLAY_NAME, "boats", apiEntityFilter);
    assertEquals(HttpStatus.OK, response.getStatusCode());

    String actualSql = response.getBody().getQuery();
    String expectedSql =
        "SELECT boat.b_id AS primary_key FROM `my-project-id.nautical`.boats AS boat "
            + "WHERE boat.b_id IN "
            + "(SELECT sailor_boat1329273297.b_id FROM `my-project-id.nautical`.sailors_favorite_boats AS sailor_boat1329273297 "
            + "WHERE sailor_boat1329273297.s_id IN ("
            + "SELECT sailor.s_id FROM `my-project-id.nautical`.sailors AS sailor WHERE sailor.s_name = 'Jim'))";
    assertEquals(expectedSql, actualSql);
  }

  @Test
  @DisplayName(
      "correct SQL string for filtering entity instances based on a binary filter with a NULL value")
  void generateSqlQueryBinaryFilterNullValue() {
    // filter for "sailors" entity instances that have no rating
    ApiEntityFilter apiEntityFilter =
        new ApiEntityFilter()
            .entityVariable("sailor")
            .filter(
                new ApiFilter()
                    .binaryFilter(
                        new ApiBinaryFilter()
                            // not setting AttributeValue means to use a null value
                            .attributeVariable(
                                new ApiAttributeVariable().variable("sailor").name("rating"))
                            .operator(ApiBinaryFilterOperator.EQUALS)));

    ResponseEntity<ApiSqlQuery> response =
        apiController.generateSqlQuery(
            NauticalUnderlayUtils.NAUTICAL_UNDERLAY_NAME, "sailors", apiEntityFilter);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(
        "SELECT sailor.s_id AS primary_key FROM `my-project-id.nautical`.sailors AS sailor "
            + "WHERE sailor.rating IS NULL",
        response.getBody().getQuery());
  }

  @Test
  @DisplayName(
      "correct SQL string for filtering entity instances based on a binary filter with a non-NULL value")
  void generateSqlQueryBinaryFilterNonNullValue() {
    // filter for "sailors" entity instances that have a rating
    ApiEntityFilter apiEntityFilter =
        new ApiEntityFilter()
            .entityVariable("sailor")
            .filter(
                new ApiFilter()
                    .binaryFilter(
                        new ApiBinaryFilter()
                            // not setting AttributeValue means to use a null value
                            .attributeVariable(
                                new ApiAttributeVariable().variable("sailor").name("rating"))
                            .operator(ApiBinaryFilterOperator.NOT_EQUALS)));

    ResponseEntity<ApiSqlQuery> response =
        apiController.generateSqlQuery(
            NauticalUnderlayUtils.NAUTICAL_UNDERLAY_NAME, "sailors", apiEntityFilter);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(
        "SELECT sailor.s_id AS primary_key FROM `my-project-id.nautical`.sailors AS sailor "
            + "WHERE sailor.rating IS NOT NULL",
        response.getBody().getQuery());
  }

  @Test
  @DisplayName("correct SQL string for filtering entity instances based on a text search filter")
  void generateSqlQueryTextSearchFilter() {
    // filter for "sailors" entity instances by the search text "george"
    ApiEntityFilter apiEntityFilter =
        new ApiEntityFilter()
            .entityVariable("sailor_alias")
            .filter(
                new ApiFilter()
                    .textSearchFilter(
                        new ApiTextSearchFilter().entityVariable("sailor_alias").term("george")));

    ResponseEntity<ApiSqlQuery> response =
        apiController.generateSqlQuery(
            NauticalUnderlayUtils.NAUTICAL_UNDERLAY_NAME, "sailors", apiEntityFilter);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(
        "SELECT sailor_alias.s_id AS primary_key FROM `my-project-id.nautical`.sailors AS sailor_alias "
            + "WHERE sailor_alias.s_id IN "
            + "(SELECT s_id FROM `my-project-id.nautical`.sailors WHERE CONTAINS_SUBSTR(s_name, 'george'))",
        response.getBody().getQuery());
  }

  @Test
  @DisplayName("correct SQL string for filtering entity instances based on a unary filter")
  void generateSqlQueryUnaryFilter() {
    // filter for "sailors" entity instances that do NOT have a rating between 40 and 45

    List<ApiFilter> arrayFilterOperands =
        List.of(
            new ApiFilter()
                .binaryFilter(
                    new ApiBinaryFilter()
                        .attributeVariable(
                            new ApiAttributeVariable().variable("sailor").name("rating"))
                        .operator(ApiBinaryFilterOperator.GREATER_THAN)
                        .attributeValue(new ApiAttributeValue().int64Val(40L))),
            new ApiFilter()
                .binaryFilter(
                    new ApiBinaryFilter()
                        .attributeVariable(
                            new ApiAttributeVariable().variable("sailor").name("rating"))
                        .operator(ApiBinaryFilterOperator.LESS_THAN)
                        .attributeValue(new ApiAttributeValue().int64Val(45L))));

    ApiFilter unaryFilterOperand =
        new ApiFilter()
            .arrayFilter(
                new ApiArrayFilter()
                    .operator(ApiArrayFilterOperator.AND)
                    .operands(arrayFilterOperands));

    ApiEntityFilter apiEntityFilter =
        new ApiEntityFilter()
            .entityVariable("sailor")
            .filter(
                new ApiFilter()
                    .unaryFilter(
                        new ApiUnaryFilter()
                            .operator(ApiUnaryFilterOperator.NOT)
                            .operand(unaryFilterOperand)));

    ResponseEntity<ApiSqlQuery> response =
        apiController.generateSqlQuery(
            NauticalUnderlayUtils.NAUTICAL_UNDERLAY_NAME, "sailors", apiEntityFilter);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(
        "SELECT sailor.s_id AS primary_key FROM `my-project-id.nautical`.sailors AS sailor "
            + "WHERE NOT ((sailor.rating > 40 AND sailor.rating < 45))",
        response.getBody().getQuery());
  }
}

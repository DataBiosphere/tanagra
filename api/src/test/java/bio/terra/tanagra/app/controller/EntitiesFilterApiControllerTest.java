package bio.terra.tanagra.app.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;

import bio.terra.tanagra.generated.model.ApiAttributeValue;
import bio.terra.tanagra.generated.model.ApiAttributeVariable;
import bio.terra.tanagra.generated.model.ApiBinaryFilter;
import bio.terra.tanagra.generated.model.ApiBinaryFilterOperator;
import bio.terra.tanagra.generated.model.ApiEntityFilter;
import bio.terra.tanagra.generated.model.ApiFilter;
import bio.terra.tanagra.generated.model.ApiRelationshipFilter;
import bio.terra.tanagra.generated.model.ApiSqlQuery;
import bio.terra.tanagra.service.underlay.NauticalUnderlayUtils;
import bio.terra.tanagra.testing.BaseSpringUnitTest;
import bio.terra.tanagra.testing.GeneratedSqlUtils;
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
            + "(SELECT sailor_boatb61b6fa5_9756_4de7_9c68_bc57fe223635.s_id FROM `my-project-id.nautical`.sailors_favorite_boats AS sailor_boatb61b6fa5_9756_4de7_9c68_bc57fe223635 "
            + "WHERE sailor_boatb61b6fa5_9756_4de7_9c68_bc57fe223635.b_id IN ("
            + "SELECT boat.b_id FROM `my-project-id.nautical`.boats AS boat WHERE boat.color = 'red'))";
    expectedSql =
        GeneratedSqlUtils.replaceGeneratedIntermediateTableAliasDiffs(
            expectedSql, actualSql, "sailor_boat");
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
            + "(SELECT sailor_boatb61b6fa5_9756_4de7_9c68_bc57fe223635.b_id FROM `my-project-id.nautical`.sailors_favorite_boats AS sailor_boatb61b6fa5_9756_4de7_9c68_bc57fe223635 "
            + "WHERE sailor_boatb61b6fa5_9756_4de7_9c68_bc57fe223635.s_id IN ("
            + "SELECT sailor.s_id FROM `my-project-id.nautical`.sailors AS sailor WHERE sailor.s_name = 'Jim'))";
    expectedSql =
        GeneratedSqlUtils.replaceGeneratedIntermediateTableAliasDiffs(
            expectedSql, actualSql, "sailor_boat");
    assertEquals(expectedSql, actualSql);
  }
}

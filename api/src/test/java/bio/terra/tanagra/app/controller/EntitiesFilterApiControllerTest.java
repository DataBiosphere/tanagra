package bio.terra.tanagra.app.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.text.StringSubstitutor;
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
  void generateSqlQueryRelationshipFilter() {
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
        replaceGeneratedIntermediateTableAliasDiffs(expectedSql, actualSql, "sailor_boat");
    assertEquals(expectedSql, actualSql);
  }

  /**
   * Replace the generated table aliases in the expected SQL with those in the actual SQL. The
   * purpose of this helper method is to allow a test to compare the expected and actual SQL without
   * failing on the generated alias names. This is necessary because the generated table names
   * include a randomly generated UUID, and so we will get a different alias each time.
   *
   * @param expected the expected SQL string that contains generated table aliases (e.g.
   *     sailor_boatb61b6fa5_9756_4de7_9c68_bc57fe223635)
   * @param actual the actual SQL string that contains generated table aliases (e.g.
   *     sailor_boat237e13f7_ab74_42b4_9b0c_e5a45299f942)
   * @param relationshipName the name of the relationship with the intermediate table
   * @return the expected SQL string, with its generated table aliases replaced with those in the
   *     actual SQL string
   */
  public static String replaceGeneratedIntermediateTableAliasDiffs(
      String expected, String actual, String relationshipName) {
    Pattern generatedTableAliasRegex =
        Pattern.compile(
            relationshipName
                + "[a-fA-F0-9]{8}_[a-fA-F0-9]{4}_[a-fA-F0-9]{4}_[a-fA-F0-9]{4}_[a-fA-F0-9]{12}");

    // find the generated table alias in the actual SQL
    Matcher actualAliasMatcher = generatedTableAliasRegex.matcher(actual);
    assertTrue(actualAliasMatcher.find(), "generated intermediate table alias not found");
    String actualAlias = actual.substring(actualAliasMatcher.start(), actualAliasMatcher.end());

    // replace all the generated table aliases in the expected SQL with ${generated_table_alias}
    String expectedTemplate =
        generatedTableAliasRegex.matcher(expected).replaceAll("\\$\\{generated_table_alias\\}");

    // substitute the ${generated_table_alias} in the expected SQL with the alias from the actual
    // SQL
    Map<String, String> params =
        ImmutableMap.<String, String>builder().put("generated_table_alias", actualAlias).build();
    return StringSubstitutor.replace(expectedTemplate, params);
  }
}

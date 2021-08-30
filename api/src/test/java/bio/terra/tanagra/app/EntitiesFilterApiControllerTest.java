package bio.terra.tanagra.app;

import static org.junit.jupiter.api.Assertions.assertEquals;

import bio.terra.tanagra.app.controller.EntitiesFiltersApiController;
import bio.terra.tanagra.generated.model.ApiAttributeValue;
import bio.terra.tanagra.generated.model.ApiAttributeVariable;
import bio.terra.tanagra.generated.model.ApiBinaryFilter;
import bio.terra.tanagra.generated.model.ApiBinaryFilterOperator;
import bio.terra.tanagra.generated.model.ApiEntityFilter;
import bio.terra.tanagra.generated.model.ApiFilter;
import bio.terra.tanagra.generated.model.ApiSqlQuery;
import bio.terra.tanagra.service.underlay.NauticalUnderlayUtils;
import bio.terra.tanagra.testing.BaseSpringUnitTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("nautical")
public class EntitiesFilterApiControllerTest extends BaseSpringUnitTest {
  @Autowired private EntitiesFiltersApiController apiController;

  @Test
  void generateSqlQuery() {
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
        "SELECT s.s_id FROM `my-project-id.nautical`.sailors AS s WHERE s.rating = 42",
        response.getBody().getQuery());
  }
}

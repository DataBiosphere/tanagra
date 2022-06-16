package bio.terra.tanagra.app.controller;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import bio.terra.common.exception.NotFoundException;
import bio.terra.tanagra.generated.model.ApiListUnderlaysResponse;
import bio.terra.tanagra.generated.model.ApiUnderlay;
import bio.terra.tanagra.service.underlay.NauticalUnderlayUtils;
import bio.terra.tanagra.testing.BaseSpringUnitTest;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles(profiles = {"nautical", "orders"})
public class UnderlaysApiControllerTest extends BaseSpringUnitTest {

  @Autowired private UnderlaysApiController controller;

  // The static string variables below are only included here because they are required
  // to pass the unit tests while criteriaConfigs is hard-coded on the backend.
  static String columns = "[" +
          "{\"key\":\"concept_name\",\"width\":\"100%\",\"title\":\"Concept Name\"}," +
          "{\"key\":\"concept_id\",\"width\":120,\"title\":\"Concept ID\"}," +
          "{\"key\":\"standard_concept\",\"width\":180,\"title\":\"Source/Standard\"}," +
          "{\"key\":\"vocabulary_id\",\"width\":120,\"title\":\"Vocab\"}," +
          "{\"key\":\"concept_code\",\"width\":120,\"title\":\"Code\"}" +
          "],";
  static String criteriaConfigs = "[" +
          "{\"type\":\"concept\",\"title\":\"Conditions\",\"defaultName\":\"Contains Conditions Codes\",\"plugin\":{\"columns\":" + columns + "\"entities\":[{\"name\":\"condition\",\"selectable\":true,\"hierarchical\":true}]}}," +
          "{\"type\":\"concept\",\"title\":\"Procedures\",\"defaultName\":\"Contains Procedures Codes\",\"plugin\":{\"columns\":" + columns + "\"entities\":[{\"name\":\"procedure\",\"selectable\":true,\"hierarchical\":true}]}}," +
          "{\"type\":\"concept\",\"title\":\"Observations\",\"defaultName\":\"Contains Observations Codes\",\"plugin\":{\"columns\":" + columns + "\"entities\":[{\"name\":\"observation\",\"selectable\":true}]}}," +
          "{\"type\":\"concept\",\"title\":\"Drugs\",\"defaultName\":\"Contains Drugs Codes\",\"plugin\":{\"columns\":" + columns + "\"entities\":[{\"name\":\"ingredient\",\"selectable\":true,\"hierarchical\":true},{\"name\":\"brand\",\"sourceConcepts\":true,\"attributes\":[\"concept_name\",\"concept_id\",\"standard_concept\",\"concept_code\"],\"listChildren\":{\"entity\":\"ingredient\",\"idPath\":\"relationshipFilter.filter.binaryFilter.attributeValue\",\"filter\":{\"relationshipFilter\":{\"outerVariable\":\"ingredient\",\"newVariable\":\"brand\",\"newEntity\":\"brand\",\"filter\":{\"binaryFilter\":{\"attributeVariable\":{\"variable\":\"brand\",\"name\":\"concept_id\"},\"operator\":\"EQUALS\",\"attributeValue\":{\"int64Val\":0}}}}}}}]}}," +
          "{\"type\":\"attribute\",\"title\":\"Ethnicity\",\"defaultName\":\"Contains Ethnicity Codes\",\"plugin\":{\"attribute\":\"ethnicity_concept_id\"}}," +
          "{\"type\":\"attribute\",\"title\":\"Gender Identity\",\"defaultName\":\"Contains Gender Identity Codes\",\"plugin\":{\"attribute\":\"gender_concept_id\"}}," +
          "{\"type\":\"attribute\",\"title\":\"Race\",\"defaultName\":\"Contains Race Codes\",\"plugin\":{\"attribute\":\"race_concept_id\"}}," +
          "{\"type\":\"attribute\",\"title\":\"Sex Assigned at Birth\",\"defaultName\":\"Contains Sex Assigned at Birth Codes\",\"plugin\":{\"attribute\":\"sex_at_birth_concept_id\"}},{\"type\":\"attribute\",\"title\":\"Year at Birth\",\"defaultName\":\"Contains Year at Birth Values\",\"plugin\":{\"attribute\":\"year_of_birth\"}}" +
          "]";

  /** ApiUnderlay for the underlay specified by the nautical profile. */
  private static final ApiUnderlay NAUTICAL_API_UNDERLAY =
      new ApiUnderlay()
          .name(NauticalUnderlayUtils.NAUTICAL_UNDERLAY_NAME)
          .entityNames(
              ImmutableList.of(
                  "boat_electric_anchors", "boat_engines", "boats", "reservations", "sailors"))
              .criteriaConfigs(criteriaConfigs);

  @Test
  void getUnderlay() {
    ResponseEntity<ApiUnderlay> response =
        controller.getUnderlay(NauticalUnderlayUtils.NAUTICAL_UNDERLAY_NAME);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(NAUTICAL_API_UNDERLAY, response.getBody());
  }

  @Test
  void getUnderlayNotFound() {
    assertThrows(NotFoundException.class, () -> controller.getUnderlay("unknown_underlay"));
  }

  @Test
  void listUnderlays() {
    ResponseEntity<ApiListUnderlaysResponse> response = controller.listUnderlays(null, null);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals("", response.getBody().getNextPageToken());
    assertThat(response.getBody().getUnderlays(), Matchers.hasItem(NAUTICAL_API_UNDERLAY));
  }

  @Test
  void listUnderlaysPagination() {
    List<ApiUnderlay> allUnderlays = new ArrayList<>();
    ResponseEntity<ApiListUnderlaysResponse> response =
        controller.listUnderlays(/* pageSize= */ 1, /* pageToken= */ null);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    allUnderlays.addAll(response.getBody().getUnderlays());

    assertThat(response.getBody().getNextPageToken(), Matchers.not(Matchers.emptyString()));
    while (!response.getBody().getNextPageToken().isEmpty()) {
      response = controller.listUnderlays(1, response.getBody().getNextPageToken());
      assertEquals(HttpStatus.OK, response.getStatusCode());
      assertThat(response.getBody().getUnderlays(), Matchers.not(Matchers.empty()));
      allUnderlays.addAll(response.getBody().getUnderlays());
    }
    assertThat(allUnderlays, Matchers.hasItem(NAUTICAL_API_UNDERLAY));
  }
}

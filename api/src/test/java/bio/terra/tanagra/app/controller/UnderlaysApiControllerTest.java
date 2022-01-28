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

  /** ApiUnderlay for the underlay specified by the nautical profile. */
  private static final ApiUnderlay NAUTICAL_API_UNDERLAY =
      new ApiUnderlay()
          .name(NauticalUnderlayUtils.NAUTICAL_UNDERLAY_NAME)
          .entityNames(
              ImmutableList.of(
                  "boat_electric_anchors", "boat_engines", "boats", "reservations", "sailors"));

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

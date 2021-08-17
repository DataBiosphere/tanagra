package bio.terra.tanagra.app;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import bio.terra.common.exception.NotFoundException;
import bio.terra.tanagra.app.controller.UnderlaysApiController;
import bio.terra.tanagra.generated.model.ApiListUnderlaysResponse;
import bio.terra.tanagra.generated.model.ApiUnderlay;
import bio.terra.tanagra.service.underlay.NauticalUnderlayUtils;
import bio.terra.tanagra.testing.BaseSpringUnitTest;
import com.google.common.collect.ImmutableList;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("nautical")
public class UnderlaysApiControllerTest extends BaseSpringUnitTest {

  @Autowired private UnderlaysApiController underlaysApiController;

  /** ApiUnderlay for the underlay specified by the nautical profile. */
  private static final ApiUnderlay NAUTICAL_API_UNDERLAY =
      new ApiUnderlay()
          .name(NauticalUnderlayUtils.NAUTICAL_UNDERLAY_NAME)
          .entityNames(ImmutableList.of("boats", "reservations", "sailors"));

  @Test
  void getUnderlay() {
    ResponseEntity<ApiUnderlay> response =
        underlaysApiController.getUnderlay(NauticalUnderlayUtils.NAUTICAL_UNDERLAY_NAME);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(NAUTICAL_API_UNDERLAY, response.getBody());
  }

  @Test
  void getUnderlayNotFound() {
    assertThrows(
        NotFoundException.class, () -> underlaysApiController.getUnderlay("unknown_underlay"));
  }

  @Test
  void listUnderlays() {
    ResponseEntity<ApiListUnderlaysResponse> response =
        underlaysApiController.listUnderlays(null, null);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals("", response.getBody().getNextPageToken());
    assertThat(response.getBody().getUnderlays(), Matchers.hasItem(NAUTICAL_API_UNDERLAY));
  }
}

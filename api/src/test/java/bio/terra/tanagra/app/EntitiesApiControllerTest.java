package bio.terra.tanagra.app;

import static bio.terra.tanagra.service.underlay.NauticalUnderlayUtils.NAUTICAL_UNDERLAY_NAME;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import bio.terra.common.exception.BadRequestException;
import bio.terra.common.exception.NotFoundException;
import bio.terra.tanagra.app.controller.EntitiesApiController;
import bio.terra.tanagra.generated.model.ApiAttribute;
import bio.terra.tanagra.generated.model.ApiDataType;
import bio.terra.tanagra.generated.model.ApiEntity;
import bio.terra.tanagra.generated.model.ApiListEntitiesResponse;
import bio.terra.tanagra.service.underlay.OrdersUnderlayUtils;
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
public class EntitiesApiControllerTest extends BaseSpringUnitTest {

  @Autowired private EntitiesApiController controller;

  private static final ApiEntity SAILOR_API_ENTITY =
      new ApiEntity()
          .name("sailors")
          .attributes(
              ImmutableList.of(
                  new ApiAttribute().name("id").dataType(ApiDataType.INT64),
                  new ApiAttribute().name("name").dataType(ApiDataType.STRING),
                  new ApiAttribute().name("rating").dataType(ApiDataType.INT64)));

  @Test
  void getEntity() {
    ResponseEntity<ApiEntity> response = controller.getEntity(NAUTICAL_UNDERLAY_NAME, "sailors");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(SAILOR_API_ENTITY, response.getBody());
  }

  @Test
  void getEntityNotFound() {
    assertThrows(
        NotFoundException.class, () -> controller.getEntity("bogus_underlay_name", "sailors"));
    assertThrows(
        NotFoundException.class,
        () -> controller.getEntity(NAUTICAL_UNDERLAY_NAME, "bogus_entity_name"));
  }

  @Test
  void listEntities() {
    ResponseEntity<ApiListEntitiesResponse> response =
        controller.listEntities(
            NAUTICAL_UNDERLAY_NAME, /* pageSize= */ null, /* pageToken= */ null);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals("", response.getBody().getNextPageToken());
    assertThat(response.getBody().getEntities(), Matchers.hasItem(SAILOR_API_ENTITY));
  }

  @Test
  void listEntitiesPagination() {
    List<ApiEntity> allEntities = new ArrayList<>();
    ResponseEntity<ApiListEntitiesResponse> response =
        controller.listEntities(NAUTICAL_UNDERLAY_NAME, /* pageSize= */ 1, /* pageToken= */ null);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    allEntities.addAll(response.getBody().getEntities());

    assertThat(response.getBody().getNextPageToken(), Matchers.not(Matchers.emptyString()));
    while (!response.getBody().getNextPageToken().isEmpty()) {
      response =
          controller.listEntities(NAUTICAL_UNDERLAY_NAME, 1, response.getBody().getNextPageToken());
      assertEquals(HttpStatus.OK, response.getStatusCode());
      assertThat(response.getBody().getEntities(), Matchers.not(Matchers.empty()));
      allEntities.addAll(response.getBody().getEntities());
    }
    assertThat(allEntities, Matchers.hasItem(SAILOR_API_ENTITY));
  }

  @Test
  void listEntitiesPaginationParameterChangeThrows() {
    ResponseEntity<ApiListEntitiesResponse> response =
        controller.listEntities(NAUTICAL_UNDERLAY_NAME, /* pageSize= */ 1, /* pageToken= */ null);
    // Change the underlayName but use an old page token.
    assertThrows(
        BadRequestException.class,
        () ->
            controller.listEntities(
                OrdersUnderlayUtils.ORDERS_UNDERLAY_NAME,
                1,
                response.getBody().getNextPageToken()));
  }
}

package bio.terra.tanagra.app.controller;

import bio.terra.common.exception.NotFoundException;
import bio.terra.tanagra.common.Paginator;
import bio.terra.tanagra.common.Paginator.Page;
import bio.terra.tanagra.generated.controller.EntitiesApi;
import bio.terra.tanagra.generated.model.ApiAttribute;
import bio.terra.tanagra.generated.model.ApiDataType;
import bio.terra.tanagra.generated.model.ApiEntity;
import bio.terra.tanagra.generated.model.ApiListEntitiesResponse;
import bio.terra.tanagra.service.search.Attribute;
import bio.terra.tanagra.service.search.DataType;
import bio.terra.tanagra.service.search.Entity;
import bio.terra.tanagra.service.underlay.Underlay;
import bio.terra.tanagra.service.underlay.UnderlayService;
import com.google.common.collect.ImmutableList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.validation.Valid;
import javax.validation.constraints.Min;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

/** An {@link EntitiesApi} controller for getting metadata about entities. */
@Controller
public class EntitiesApiController implements EntitiesApi {
  private static final int DEFAULT_PAGE_SIZE = 100;
  private final UnderlayService underlayService;

  @Autowired
  public EntitiesApiController(UnderlayService underlayService) {
    this.underlayService = underlayService;
  }

  @Override
  public ResponseEntity<ApiEntity> getEntity(String underlayName, String entityName) {
    // TODO authorization.
    Underlay underlay = getUnderlay(underlayName);
    Entity entity = getEntity(underlay, entityName);
    ApiEntity apiEntity = convert(entity, underlay);
    return ResponseEntity.ok(apiEntity);
  }

  @Override
  public ResponseEntity<ApiListEntitiesResponse> listEntities(
      String underlayName, @Min(0) @Valid Integer pageSize, @Valid String pageToken) {
    // TODO authorization.
    Underlay underlay = getUnderlay(underlayName);
    List<Entity> sortedEntities =
        underlay.entities().values().stream()
            .sorted(Comparator.comparing(Entity::name))
            .collect(ImmutableList.toImmutableList());

    int parsedPageSize = (pageSize == null || pageSize == 0) ? DEFAULT_PAGE_SIZE : pageSize;

    Page<Entity> page =
        new Paginator<>(parsedPageSize, hashListEntitiesParameters(underlayName))
            .getPage(sortedEntities, pageToken);
    List<ApiEntity> apiEntities =
        page.results().stream()
            .map(entity -> convert(entity, underlay))
            .collect(Collectors.toList());

    ApiListEntitiesResponse response =
        new ApiListEntitiesResponse().entities(apiEntities).nextPageToken(page.nextPageToken());
    return ResponseEntity.ok(response);
  }

  private Underlay getUnderlay(String underlayName) {
    Optional<Underlay> underlay = underlayService.getUnderlay(underlayName);
    return underlay.orElseThrow(
        () ->
            new NotFoundException(String.format("No known underlay with name '%s'", underlayName)));
  }

  private static Entity getEntity(Underlay underlay, String entityName) {
    Entity entity = underlay.entities().get(entityName);
    if (entity == null) {
      throw new NotFoundException(
          String.format(
              "No known entity with name '%s' in underlay '%s'", entityName, underlay.name()));
    }
    return entity;
  }

  /**
   * Returns a consistent hash on the list entities parameters to use for pagination token parameter
   * consistency checking.
   *
   * <p>The choice of has is unimportant, just that it is consistent across services.
   */
  private static String hashListEntitiesParameters(String underlayName) {
    return String.valueOf(underlayName.hashCode());
  }

  private static ApiEntity convert(Entity entity, Underlay underlay) {
    List<ApiAttribute> attributes =
        underlay.attributes().row(entity).values().stream()
            .sorted(Comparator.comparing(Attribute::name))
            .map(EntitiesApiController::convert)
            .collect(Collectors.toList());
    return new ApiEntity().name(entity.name()).attributes(attributes);
  }

  private static ApiAttribute convert(Attribute attribute) {
    return new ApiAttribute().name(attribute.name()).dataType(convert(attribute.dataType()));
  }

  private static ApiDataType convert(DataType dataType) {
    switch (dataType) {
      case STRING:
        return ApiDataType.STRING;
      case INT64:
        return ApiDataType.INT64;
      default:
        throw new UnsupportedOperationException(
            String.format("Unable to convert DataType '%s'", dataType));
    }
  }
}

package bio.terra.tanagra.app.controller;

import bio.terra.common.exception.NotFoundException;
import bio.terra.tanagra.common.Paginator;
import bio.terra.tanagra.common.Paginator.Page;
import bio.terra.tanagra.generated.controller.EntitiesApi;
import bio.terra.tanagra.generated.model.ApiAttribute;
import bio.terra.tanagra.generated.model.ApiAttributeFilterHint;
import bio.terra.tanagra.generated.model.ApiAttributeValue;
import bio.terra.tanagra.generated.model.ApiDataType;
import bio.terra.tanagra.generated.model.ApiEntity;
import bio.terra.tanagra.generated.model.ApiEntitySearchHint;
import bio.terra.tanagra.generated.model.ApiEnumHint;
import bio.terra.tanagra.generated.model.ApiEnumHintValue;
import bio.terra.tanagra.generated.model.ApiIntegerBoundsHint;
import bio.terra.tanagra.generated.model.ApiListEntitiesResponse;
import bio.terra.tanagra.generated.model.ApiRelationship;
import bio.terra.tanagra.proto.underlay.EntitySearchHint;
import bio.terra.tanagra.proto.underlay.EnumHint;
import bio.terra.tanagra.proto.underlay.EnumHintValue;
import bio.terra.tanagra.proto.underlay.FilterableAttribute;
import bio.terra.tanagra.proto.underlay.IntegerBoundsHint;
import bio.terra.tanagra.service.search.Attribute;
import bio.terra.tanagra.service.search.DataType;
import bio.terra.tanagra.service.search.Entity;
import bio.terra.tanagra.service.search.Relationship;
import bio.terra.tanagra.service.underlay.EntityFiltersSchema;
import bio.terra.tanagra.service.underlay.Underlay;
import bio.terra.tanagra.service.underlay.UnderlayService;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
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
    ApiEntity apiEntity =
        convert(entity, Optional.ofNullable(underlay.entityFiltersSchemas().get(entity)), underlay);
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
            .map(
                entity ->
                    convert(
                        entity,
                        Optional.ofNullable(underlay.entityFiltersSchemas().get(entity)),
                        underlay))
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

  private static ApiEntity convert(
      Entity entity, Optional<EntityFiltersSchema> filtersSchema, Underlay underlay) {
    List<ApiAttribute> attributes =
        underlay.attributes().row(entity).values().stream()
            .sorted(Comparator.comparing(Attribute::name))
            .map(attribute -> convert(attribute, filtersSchema))
            .collect(Collectors.toList());

    ImmutableSet<Relationship> filterableRelationships =
        filtersSchema.map(EntityFiltersSchema::filterableRelationships).orElse(ImmutableSet.of());
    List<ApiRelationship> relationships =
        underlay.getRelationshipsOf(entity).stream()
            .map(relationship -> convert(relationship, entity, filterableRelationships))
            .collect(Collectors.toList());

    return new ApiEntity().name(entity.name()).attributes(attributes).relationships(relationships);
  }

  private static ApiAttribute convert(
      Attribute attribute, Optional<EntityFiltersSchema> filtersSchema) {
    ApiAttributeFilterHint filterHint =
        filtersSchema
            .map(EntityFiltersSchema::filterableAttributes)
            .map(filterableAttributes -> filterableAttributes.get(attribute))
            .map(EntitiesApiController::convert)
            .orElse(null);
    return new ApiAttribute()
        .name(attribute.name())
        .dataType(convert(attribute.dataType()))
        .attributeFilterHint(filterHint);
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

  @VisibleForTesting
  static ApiAttributeFilterHint convert(FilterableAttribute filterableAttribute) {
    switch (filterableAttribute.getHintCase()) {
      case ENTITY_SEARCH_HINT:
        return new ApiAttributeFilterHint()
            .entitySearchHint(convert(filterableAttribute.getEntitySearchHint()));
      case ENUM_HINT:
        return new ApiAttributeFilterHint().enumHint(convert(filterableAttribute.getEnumHint()));
      case INTEGER_BOUNDS_HINT:
        return new ApiAttributeFilterHint()
            .integerBoundsHint(convert(filterableAttribute.getIntegerBoundsHint()));
      case HINT_NOT_SET:
        // Don't set any hints if there are none.
        break;
      default:
        throw new UnsupportedOperationException(
            String.format(
                "Unable to convert filterable attribute case '%s'",
                filterableAttribute.getHintCase().name()));
    }
    return null;
  }

  private static ApiEntitySearchHint convert(EntitySearchHint entitySearchHint) {
    return new ApiEntitySearchHint().entityName(entitySearchHint.getEntity());
  }

  private static ApiEnumHint convert(EnumHint enumHint) {
    return new ApiEnumHint()
        .enumHintValues(
            enumHint.getEnumHintValuesList().stream()
                .map(EntitiesApiController::convert)
                .collect(Collectors.toList()));
  }

  private static ApiEnumHintValue convert(EnumHintValue enumHintValue) {
    ApiAttributeValue apiAttributeValue = new ApiAttributeValue();
    switch (enumHintValue.getValueCase()) {
      case INT64_VAL:
        apiAttributeValue.int64Val(enumHintValue.getInt64Val());
        break;
      case STRING_VAL:
        apiAttributeValue.stringVal(enumHintValue.getStringVal());
        break;
      case BOOL_VAL:
        apiAttributeValue.boolVal(enumHintValue.getBoolVal());
        break;
      case VALUE_NOT_SET:
        apiAttributeValue = null;
        break;
      default:
        throw new UnsupportedOperationException(
            String.format(
                "Unable to convert enum hint value case: '%s'", enumHintValue.getValueCase()));
    }
    return new ApiEnumHintValue()
        .displayName(enumHintValue.getDisplayName())
        .description(enumHintValue.getDescription())
        .attributeValue(apiAttributeValue);
  }

  private static ApiIntegerBoundsHint convert(IntegerBoundsHint integerBoundsHint) {
    ApiIntegerBoundsHint hint = new ApiIntegerBoundsHint();
    if (integerBoundsHint.hasMin()) {
      hint.setMin(integerBoundsHint.getMin());
    }
    if (integerBoundsHint.hasMax()) {
      hint.setMax(integerBoundsHint.getMax());
    }
    return hint;
  }

  private static ApiRelationship convert(
      Relationship relationship,
      Entity containingEntity,
      ImmutableSet<Relationship> filterableRelationship) {
    Entity relatedEntity = relationship.other(containingEntity);
    return new ApiRelationship()
        .name(relationship.name())
        .relatedEntity(relatedEntity.name())
        .filterable(filterableRelationship.contains(relationship));
  }
}

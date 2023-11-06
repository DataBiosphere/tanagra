package bio.terra.tanagra.app.controller;

import static bio.terra.tanagra.service.accesscontrol.Action.QUERY_COUNTS;
import static bio.terra.tanagra.service.accesscontrol.Action.QUERY_INSTANCES;
import static bio.terra.tanagra.service.accesscontrol.Action.READ;
import static bio.terra.tanagra.service.accesscontrol.ResourceType.UNDERLAY;

import bio.terra.tanagra.api.query.EntityCountRequest;
import bio.terra.tanagra.api.query.EntityCountResult;
import bio.terra.tanagra.api.query.EntityHintRequest;
import bio.terra.tanagra.api.query.EntityHintResult;
import bio.terra.tanagra.api.query.EntityInstance;
import bio.terra.tanagra.api.query.EntityQueryRequest;
import bio.terra.tanagra.api.query.EntityQueryResult;
import bio.terra.tanagra.api.query.filter.EntityFilter;
import bio.terra.tanagra.app.authentication.SpringAuthentication;
import bio.terra.tanagra.app.controller.objmapping.FromApiUtils;
import bio.terra.tanagra.app.controller.objmapping.ToApiUtils;
import bio.terra.tanagra.exception.SystemException;
import bio.terra.tanagra.generated.controller.UnderlaysApi;
import bio.terra.tanagra.generated.model.ApiCountQuery;
import bio.terra.tanagra.generated.model.ApiDisplayHint;
import bio.terra.tanagra.generated.model.ApiDisplayHintDisplayHint;
import bio.terra.tanagra.generated.model.ApiDisplayHintEnum;
import bio.terra.tanagra.generated.model.ApiDisplayHintEnumEnumHintValues;
import bio.terra.tanagra.generated.model.ApiDisplayHintList;
import bio.terra.tanagra.generated.model.ApiDisplayHintNumericRange;
import bio.terra.tanagra.generated.model.ApiEntity;
import bio.terra.tanagra.generated.model.ApiEntityList;
import bio.terra.tanagra.generated.model.ApiHintQuery;
import bio.terra.tanagra.generated.model.ApiInstance;
import bio.terra.tanagra.generated.model.ApiInstanceCountList;
import bio.terra.tanagra.generated.model.ApiInstanceHierarchyFields;
import bio.terra.tanagra.generated.model.ApiInstanceListResult;
import bio.terra.tanagra.generated.model.ApiInstanceRelationshipFields;
import bio.terra.tanagra.generated.model.ApiQuery;
import bio.terra.tanagra.generated.model.ApiUnderlay;
import bio.terra.tanagra.generated.model.ApiUnderlayList;
import bio.terra.tanagra.generated.model.ApiValueDisplay;
import bio.terra.tanagra.service.accesscontrol.AccessControlService;
import bio.terra.tanagra.service.accesscontrol.Permissions;
import bio.terra.tanagra.service.accesscontrol.ResourceCollection;
import bio.terra.tanagra.service.accesscontrol.ResourceId;
import bio.terra.tanagra.service.query.UnderlayService;
import bio.terra.tanagra.underlay.Attribute;
import bio.terra.tanagra.underlay.DisplayHint;
import bio.terra.tanagra.underlay.Entity;
import bio.terra.tanagra.underlay.HierarchyField;
import bio.terra.tanagra.underlay.RelationshipField;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.ValueDisplay;
import bio.terra.tanagra.underlay.displayhint.EnumVals;
import bio.terra.tanagra.underlay.displayhint.NumericRange;
import bio.terra.tanagra.utils.SqlFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

@Controller
public class UnderlaysApiController implements UnderlaysApi {
  private final UnderlayService underlayService;
  private final AccessControlService accessControlService;

  @Autowired
  public UnderlaysApiController(
      UnderlayService underlayService, AccessControlService accessControlService) {
    this.underlayService = underlayService;
    this.accessControlService = accessControlService;
  }

  @Override
  public ResponseEntity<ApiUnderlayList> listUnderlays() {
    ResourceCollection authorizedUnderlayNames =
        accessControlService.listAuthorizedResources(
            SpringAuthentication.getCurrentUser(), Permissions.forActions(UNDERLAY, READ));
    List<Underlay> authorizedUnderlays = underlayService.listUnderlays(authorizedUnderlayNames);
    ApiUnderlayList apiUnderlays = new ApiUnderlayList();
    authorizedUnderlays.stream()
        .forEach(underlay -> apiUnderlays.addUnderlaysItem(ToApiUtils.toApiObject(underlay)));
    return ResponseEntity.ok(apiUnderlays);
  }

  @Override
  public ResponseEntity<ApiUnderlay> getUnderlay(String underlayName) {
    accessControlService.throwIfUnauthorized(
        SpringAuthentication.getCurrentUser(),
        Permissions.forActions(UNDERLAY, READ),
        ResourceId.forUnderlay(underlayName));
    return ResponseEntity.ok(ToApiUtils.toApiObject(underlayService.getUnderlay(underlayName)));
  }

  @Override
  public ResponseEntity<ApiEntityList> listEntities(String underlayName) {
    accessControlService.throwIfUnauthorized(
        SpringAuthentication.getCurrentUser(),
        Permissions.forActions(UNDERLAY, READ),
        ResourceId.forUnderlay(underlayName));
    ApiEntityList apiEntities = new ApiEntityList();
    List<Entity> entities = underlayService.listEntities(underlayName);
    entities.stream().forEach(entity -> apiEntities.addEntitiesItem(toApiObject(entity)));
    return ResponseEntity.ok(apiEntities);
  }

  @Override
  public ResponseEntity<ApiEntity> getEntity(String underlayName, String entityName) {
    accessControlService.throwIfUnauthorized(
        SpringAuthentication.getCurrentUser(),
        Permissions.forActions(UNDERLAY, READ),
        ResourceId.forUnderlay(underlayName));
    Entity entity = underlayService.getEntity(underlayName, entityName);
    return ResponseEntity.ok(toApiObject(entity));
  }

  @Override
  public ResponseEntity<ApiInstanceListResult> listInstances(
      String underlayName, String entityName, ApiQuery body) {
    accessControlService.throwIfUnauthorized(
        SpringAuthentication.getCurrentUser(),
        Permissions.forActions(UNDERLAY, QUERY_INSTANCES),
        ResourceId.forUnderlay(underlayName));
    EntityQueryRequest entityQueryRequest =
        FromApiUtils.fromApiObject(body, underlayService.getEntity(underlayName, entityName));
    EntityQueryResult entityQueryResult = UnderlayService.listEntityInstances(entityQueryRequest);
    return ResponseEntity.ok(
        new ApiInstanceListResult()
            .instances(
                entityQueryResult.getEntityInstances().stream()
                    .map(entityInstance -> toApiObject(entityInstance))
                    .collect(Collectors.toList()))
            .sql(SqlFormatter.format(entityQueryResult.getSql()))
            .pageMarker(
                entityQueryResult.getPageMarker() == null
                    ? null
                    : entityQueryResult.getPageMarker().serialize()));
  }

  @Override
  public ResponseEntity<ApiInstanceCountList> countInstances(
      String underlayName, String entityName, ApiCountQuery body) {
    accessControlService.throwIfUnauthorized(
        SpringAuthentication.getCurrentUser(),
        Permissions.forActions(UNDERLAY, QUERY_COUNTS),
        ResourceId.forUnderlay(underlayName));
    Entity entity = underlayService.getEntity(underlayName, entityName);

    List<Attribute> attributes = new ArrayList<>();
    if (body.getAttributes() != null) {
      attributes =
          body.getAttributes().stream()
              .map(attrName -> FromApiUtils.getAttribute(entity, attrName))
              .collect(Collectors.toList());
    }

    EntityFilter entityFilter = null;
    if (body.getFilter() != null) {
      entityFilter = FromApiUtils.fromApiObject(body.getFilter(), entity, underlayName);
    }

    EntityCountResult entityCountResult =
        underlayService.countEntityInstances(
            new EntityCountRequest.Builder()
                .entity(entity)
                .mappingType(Underlay.MappingType.INDEX)
                .attributes(attributes)
                .filter(entityFilter)
                .build());
    return ResponseEntity.ok(
        new ApiInstanceCountList()
            .instanceCounts(
                entityCountResult.getEntityCounts().stream()
                    .map(ToApiUtils::toApiObject)
                    .collect(Collectors.toList()))
            .sql(SqlFormatter.format(entityCountResult.getSql())));
  }

  @Override
  public ResponseEntity<ApiDisplayHintList> queryHints(
      String underlayName, String entityName, ApiHintQuery body) {
    accessControlService.throwIfUnauthorized(
        SpringAuthentication.getCurrentUser(),
        Permissions.forActions(UNDERLAY, QUERY_COUNTS),
        ResourceId.forUnderlay(underlayName));
    Entity entity = underlayService.getEntity(underlayName, entityName);

    EntityHintRequest.Builder entityHintRequest = new EntityHintRequest.Builder().entity(entity);
    if (body != null && body.getRelatedEntity() != null) {
      // Return display hints for entity instances that are related to an instance of another entity
      // (e.g. numeric range for measurement_occurrence.value_numeric, computed across
      // measurement_occurrence instances that are related to measurement=BodyHeight).
      Entity relatedEntity =
          underlayService.getEntity(underlayName, body.getRelatedEntity().getName());
      entityHintRequest
          .relatedEntity(relatedEntity)
          .relatedEntityId(FromApiUtils.fromApiObject(body.getRelatedEntity().getId()))
          .entityGroup(
              FromApiUtils.getRelationship(
                      entity.getUnderlay().getEntityGroups().values(), entity, relatedEntity)
                  .getEntityGroup());
    } // else {} Return display hints computed across all entity instances (e.g. enum values for
    // person.gender).
    EntityHintResult entityHintResult = underlayService.listEntityHints(entityHintRequest.build());
    return ResponseEntity.ok(toApiObject(entityHintResult));
  }

  private ApiEntity toApiObject(Entity entity) {
    return new ApiEntity()
        .name(entity.getName())
        .idAttribute(entity.getIdAttribute().getName())
        .attributes(
            entity.getAttributes().stream()
                .map(a -> ToApiUtils.toApiObject(a))
                .collect(Collectors.toList()));
  }

  private ApiInstance toApiObject(EntityInstance entityInstance) {
    ApiInstance instance = new ApiInstance();
    Map<String, ApiValueDisplay> attributes = new HashMap<>();
    for (Map.Entry<Attribute, ValueDisplay> attributeValue :
        entityInstance.getAttributeValues().entrySet()) {
      attributes.put(
          attributeValue.getKey().getName(), ToApiUtils.toApiObject(attributeValue.getValue()));
    }

    Map<String, ApiInstanceHierarchyFields> hierarchyFieldSets = new HashMap<>();
    for (Map.Entry<HierarchyField, ValueDisplay> hierarchyFieldValue :
        entityInstance.getHierarchyFieldValues().entrySet()) {
      HierarchyField hierarchyField = hierarchyFieldValue.getKey();
      ValueDisplay valueDisplay = hierarchyFieldValue.getValue();

      ApiInstanceHierarchyFields hierarchyFieldSet =
          hierarchyFieldSets.get(hierarchyField.getHierarchy().getName());
      if (hierarchyFieldSet == null) {
        hierarchyFieldSet =
            new ApiInstanceHierarchyFields().hierarchy(hierarchyField.getHierarchy().getName());
        hierarchyFieldSets.put(hierarchyField.getHierarchy().getName(), hierarchyFieldSet);
      }
      switch (hierarchyField.getType()) {
        case IS_MEMBER:
          hierarchyFieldSet.isMember(valueDisplay.getValue().getBooleanVal());
          break;
        case IS_ROOT:
          hierarchyFieldSet.isRoot(valueDisplay.getValue().getBooleanVal());
          break;
        case PATH:
          if (valueDisplay != null) {
            hierarchyFieldSet.path(valueDisplay.getValue().getStringVal());
          }
          break;
        case NUM_CHILDREN:
          hierarchyFieldSet.numChildren(Math.toIntExact(valueDisplay.getValue().getInt64Val()));
          break;
        default:
          throw new SystemException("Unknown hierarchy field type: " + hierarchyField.getType());
      }
    }

    Map<String, ApiInstanceRelationshipFields> relationshipFieldSets = new HashMap<>();
    for (Map.Entry<RelationshipField, ValueDisplay> relationshipFieldValue :
        entityInstance.getRelationshipFieldValues().entrySet()) {
      RelationshipField relationshipField = relationshipFieldValue.getKey();
      ValueDisplay valueDisplay = relationshipFieldValue.getValue();

      ApiInstanceRelationshipFields relationshipFieldSet =
          relationshipFieldSets.get(relationshipField.getName());
      if (relationshipFieldSet == null) {
        relationshipFieldSet =
            new ApiInstanceRelationshipFields()
                .relatedEntity(
                    relationshipField
                        .getRelationship()
                        .getRelatedEntity(relationshipField.getEntity())
                        .getName())
                .hierarchy(
                    relationshipField.getHierarchy() == null
                        ? null
                        : relationshipField.getHierarchy().getName());
        relationshipFieldSets.put(relationshipField.getName(), relationshipFieldSet);
      }
      switch (relationshipField.getType()) {
        case COUNT:
          if (valueDisplay != null) {
            relationshipFieldSet.count(Math.toIntExact(valueDisplay.getValue().getInt64Val()));
          }
          break;
        case DISPLAY_HINTS:
          relationshipFieldSet.displayHints(valueDisplay.getValue().getStringVal());
          break;
        default:
          throw new SystemException(
              "Unknown relationship field type: " + relationshipField.getType());
      }
    }

    return instance
        .attributes(attributes)
        .hierarchyFields(hierarchyFieldSets.values().stream().collect(Collectors.toList()))
        .relationshipFields(relationshipFieldSets.values().stream().collect(Collectors.toList()));
  }

  private ApiDisplayHintList toApiObject(EntityHintResult entityHintResult) {
    return new ApiDisplayHintList()
        .sql(SqlFormatter.format(entityHintResult.getSql()))
        .displayHints(
            entityHintResult.getHintMap().entrySet().stream()
                .map(
                    attrHint -> {
                      Attribute attr = attrHint.getKey();
                      DisplayHint hint = attrHint.getValue();
                      return new ApiDisplayHint()
                          .attribute(ToApiUtils.toApiObject(attr))
                          .displayHint(hint == null ? null : toApiObject(hint));
                    })
                .collect(Collectors.toList()));
  }

  private ApiDisplayHintDisplayHint toApiObject(DisplayHint displayHint) {
    switch (displayHint.getType()) {
      case ENUM:
        EnumVals enumVals = (EnumVals) displayHint;
        return new ApiDisplayHintDisplayHint()
            .enumHint(
                new ApiDisplayHintEnum()
                    .enumHintValues(
                        enumVals.getEnumValsList().stream()
                            .map(
                                ev ->
                                    new ApiDisplayHintEnumEnumHintValues()
                                        .enumVal(ToApiUtils.toApiObject(ev.getValueDisplay()))
                                        .count(Math.toIntExact(ev.getCount())))
                            .collect(Collectors.toList())));
      case RANGE:
        NumericRange numericRange = (NumericRange) displayHint;
        return new ApiDisplayHintDisplayHint()
            .numericRangeHint(
                new ApiDisplayHintNumericRange()
                    .min(numericRange.getMinVal())
                    .max(numericRange.getMaxVal()));
      default:
        throw new SystemException("Unknown display hint type: " + displayHint.getType());
    }
  }
}

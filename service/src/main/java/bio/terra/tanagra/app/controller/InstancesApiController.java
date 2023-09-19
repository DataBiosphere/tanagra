package bio.terra.tanagra.app.controller;

import static bio.terra.tanagra.service.accesscontrol.Action.QUERY_COUNTS;
import static bio.terra.tanagra.service.accesscontrol.Action.QUERY_INSTANCES;
import static bio.terra.tanagra.service.accesscontrol.ResourceType.UNDERLAY;

import bio.terra.tanagra.api.query.*;
import bio.terra.tanagra.api.query.filter.EntityFilter;
import bio.terra.tanagra.app.authentication.SpringAuthentication;
import bio.terra.tanagra.app.controller.objmapping.FromApiUtils;
import bio.terra.tanagra.app.controller.objmapping.ToApiUtils;
import bio.terra.tanagra.exception.SystemException;
import bio.terra.tanagra.generated.controller.InstancesApi;
import bio.terra.tanagra.generated.model.*;
import bio.terra.tanagra.service.accesscontrol.AccessControlService;
import bio.terra.tanagra.service.accesscontrol.Permissions;
import bio.terra.tanagra.service.accesscontrol.ResourceId;
import bio.terra.tanagra.service.query.UnderlayService;
import bio.terra.tanagra.underlay.Attribute;
import bio.terra.tanagra.underlay.Entity;
import bio.terra.tanagra.underlay.HierarchyField;
import bio.terra.tanagra.underlay.RelationshipField;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.ValueDisplay;
import bio.terra.tanagra.utils.SqlFormatter;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

@Controller
public class InstancesApiController implements InstancesApi {
  private final UnderlayService underlayService;
  private final AccessControlService accessControlService;

  @Autowired
  public InstancesApiController(
      UnderlayService underlayService, AccessControlService accessControlService) {
    this.underlayService = underlayService;
    this.accessControlService = accessControlService;
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
}

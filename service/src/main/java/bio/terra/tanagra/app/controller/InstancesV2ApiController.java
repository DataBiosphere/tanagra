package bio.terra.tanagra.app.controller;

import static bio.terra.tanagra.service.accesscontrol.Action.QUERY_COUNTS;
import static bio.terra.tanagra.service.accesscontrol.Action.QUERY_INSTANCES;
import static bio.terra.tanagra.service.accesscontrol.ResourceType.UNDERLAY;

import bio.terra.tanagra.app.auth.SpringAuthentication;
import bio.terra.tanagra.exception.SystemException;
import bio.terra.tanagra.generated.controller.InstancesV2Api;
import bio.terra.tanagra.generated.model.*;
import bio.terra.tanagra.service.*;
import bio.terra.tanagra.service.accesscontrol.Permissions;
import bio.terra.tanagra.service.accesscontrol.ResourceId;
import bio.terra.tanagra.service.instances.*;
import bio.terra.tanagra.service.instances.filter.EntityFilter;
import bio.terra.tanagra.service.utils.ToApiConversionUtils;
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
public class InstancesV2ApiController implements InstancesV2Api {
  private final UnderlaysService underlaysService;
  private final QuerysService querysService;
  private final FromApiConversionService fromApiConversionService;
  private final AccessControlService accessControlService;

  @Autowired
  public InstancesV2ApiController(
      UnderlaysService underlaysService,
      QuerysService querysService,
      FromApiConversionService fromApiConversionService,
      AccessControlService accessControlService) {
    this.underlaysService = underlaysService;
    this.querysService = querysService;
    this.fromApiConversionService = fromApiConversionService;
    this.accessControlService = accessControlService;
  }

  @Override
  public ResponseEntity<ApiInstanceListResultV2> listInstances(
      String underlayName, String entityName, ApiQueryV2 body) {
    accessControlService.throwIfUnauthorized(
        SpringAuthentication.getCurrentUser(),
        Permissions.forActions(UNDERLAY, QUERY_INSTANCES),
        ResourceId.forUnderlay(underlayName));
    EntityQueryRequest entityQueryRequest =
        fromApiConversionService.fromApiObject(body, underlayName, entityName);
    EntityQueryResult entityQueryResult = querysService.listEntityInstances(entityQueryRequest);
    return ResponseEntity.ok(
        new ApiInstanceListResultV2()
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

  private ApiInstanceV2 toApiObject(EntityInstance entityInstance) {
    ApiInstanceV2 instance = new ApiInstanceV2();
    Map<String, ApiValueDisplayV2> attributes = new HashMap<>();
    for (Map.Entry<Attribute, ValueDisplay> attributeValue :
        entityInstance.getAttributeValues().entrySet()) {
      attributes.put(
          attributeValue.getKey().getName(),
          ToApiConversionUtils.toApiObject(attributeValue.getValue()));
    }

    Map<String, ApiInstanceV2HierarchyFields> hierarchyFieldSets = new HashMap<>();
    for (Map.Entry<HierarchyField, ValueDisplay> hierarchyFieldValue :
        entityInstance.getHierarchyFieldValues().entrySet()) {
      HierarchyField hierarchyField = hierarchyFieldValue.getKey();
      ValueDisplay valueDisplay = hierarchyFieldValue.getValue();

      ApiInstanceV2HierarchyFields hierarchyFieldSet =
          hierarchyFieldSets.get(hierarchyField.getHierarchy().getName());
      if (hierarchyFieldSet == null) {
        hierarchyFieldSet =
            new ApiInstanceV2HierarchyFields().hierarchy(hierarchyField.getHierarchy().getName());
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

    Map<String, ApiInstanceV2RelationshipFields> relationshipFieldSets = new HashMap<>();
    for (Map.Entry<RelationshipField, ValueDisplay> relationshipFieldValue :
        entityInstance.getRelationshipFieldValues().entrySet()) {
      RelationshipField relationshipField = relationshipFieldValue.getKey();
      ValueDisplay valueDisplay = relationshipFieldValue.getValue();

      ApiInstanceV2RelationshipFields relationshipFieldSet =
          relationshipFieldSets.get(relationshipField.getName());
      if (relationshipFieldSet == null) {
        relationshipFieldSet =
            new ApiInstanceV2RelationshipFields()
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
  public ResponseEntity<ApiInstanceCountListV2> countInstances(
      String underlayName, String entityName, ApiCountQueryV2 body) {
    accessControlService.throwIfUnauthorized(
        SpringAuthentication.getCurrentUser(),
        Permissions.forActions(UNDERLAY, QUERY_COUNTS),
        ResourceId.forUnderlay(underlayName));
    Entity entity = underlaysService.getEntity(underlayName, entityName);

    List<Attribute> attributes = new ArrayList<>();
    if (body.getAttributes() != null) {
      attributes =
          body.getAttributes().stream()
              .map(attrName -> underlaysService.getAttribute(entity, attrName))
              .collect(Collectors.toList());
    }

    EntityFilter entityFilter = null;
    if (body.getFilter() != null) {
      entityFilter = fromApiConversionService.fromApiObject(body.getFilter(), entity, underlayName);
    }

    EntityCountResult entityCountResult =
        querysService.countEntityInstances(
            new EntityCountRequest.Builder()
                .entity(entity)
                .mappingType(Underlay.MappingType.INDEX)
                .attributes(attributes)
                .filter(entityFilter)
                .build());
    return ResponseEntity.ok(
        new ApiInstanceCountListV2()
            .instanceCounts(
                entityCountResult.getEntityCounts().stream()
                    .map(ToApiConversionUtils::toApiObject)
                    .collect(Collectors.toList()))
            .sql(SqlFormatter.format(entityCountResult.getSql())));
  }
}

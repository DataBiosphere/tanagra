package bio.terra.tanagra.app.controller;

import static bio.terra.tanagra.service.accesscontrol.Action.QUERY_COUNTS;
import static bio.terra.tanagra.service.accesscontrol.Action.QUERY_INSTANCES;
import static bio.terra.tanagra.service.accesscontrol.ResourceType.UNDERLAY;

import bio.terra.tanagra.app.auth.SpringAuthentication;
import bio.terra.tanagra.exception.SystemException;
import bio.terra.tanagra.generated.controller.InstancesV2Api;
import bio.terra.tanagra.generated.model.*;
import bio.terra.tanagra.query.OrderByDirection;
import bio.terra.tanagra.query.PageMarker;
import bio.terra.tanagra.service.*;
import bio.terra.tanagra.service.accesscontrol.ResourceId;
import bio.terra.tanagra.service.export.DataExport;
import bio.terra.tanagra.service.export.ExportRequest;
import bio.terra.tanagra.service.export.ExportResult;
import bio.terra.tanagra.service.instances.*;
import bio.terra.tanagra.service.instances.filter.EntityFilter;
import bio.terra.tanagra.service.utils.ToApiConversionUtils;
import bio.terra.tanagra.service.utils.ValidationUtils;
import bio.terra.tanagra.underlay.Attribute;
import bio.terra.tanagra.underlay.Entity;
import bio.terra.tanagra.underlay.Hierarchy;
import bio.terra.tanagra.underlay.HierarchyField;
import bio.terra.tanagra.underlay.Relationship;
import bio.terra.tanagra.underlay.RelationshipField;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.ValueDisplay;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
  private final DataExportService dataExportService;

  @Autowired
  public InstancesV2ApiController(
      UnderlaysService underlaysService,
      QuerysService querysService,
      FromApiConversionService fromApiConversionService,
      AccessControlService accessControlService,
      DataExportService dataExportService) {
    this.underlaysService = underlaysService;
    this.querysService = querysService;
    this.fromApiConversionService = fromApiConversionService;
    this.accessControlService = accessControlService;
    this.dataExportService = dataExportService;
  }

  @Override
  public ResponseEntity<ApiInstanceListResultV2> listInstances(
      String underlayName, String entityName, ApiQueryV2 body) {
    accessControlService.throwIfUnauthorized(
        SpringAuthentication.getCurrentUser(),
        QUERY_INSTANCES,
        UNDERLAY,
        ResourceId.forUnderlay(underlayName));

    Entity entity = underlaysService.getEntity(underlayName, entityName);
    List<Attribute> selectAttributes = selectAttributesFromRequest(body, entity);
    List<HierarchyField> selectHierarchyFields = selectHierarchyFieldsFromRequest(body, entity);
    List<RelationshipField> selectRelationshipFields =
        selectRelationshipFieldsFromRequest(body, entity, underlayName);
    List<EntityQueryOrderBy> entityOrderBys = entityOrderBysFromRequest(body, entity, underlayName);
    EntityFilter entityFilter = null;
    if (body.getFilter() != null) {
      ValidationUtils.validateApiFilter(body.getFilter());
      entityFilter = fromApiConversionService.fromApiObject(body.getFilter(), entity, underlayName);
    }

    EntityQueryResult entityQueryResult =
        querysService.listEntityInstances(
            new EntityQueryRequest.Builder()
                .entity(entity)
                .mappingType(Underlay.MappingType.INDEX)
                .selectAttributes(selectAttributes)
                .selectHierarchyFields(selectHierarchyFields)
                .selectRelationshipFields(selectRelationshipFields)
                .filter(entityFilter)
                .orderBys(entityOrderBys)
                .limit(body.getLimit())
                .pageSize(body.getPageSize())
                .pageMarker(PageMarker.deserialize(body.getPageMarker()))
                .build());
    return ResponseEntity.ok(
        new ApiInstanceListResultV2()
            .instances(
                entityQueryResult.getEntityInstances().stream()
                    .map(entityInstance -> toApiObject(entityInstance))
                    .collect(Collectors.toList()))
            .sql(entityQueryResult.getSql())
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
        QUERY_COUNTS,
        UNDERLAY,
        ResourceId.forUnderlay(underlayName));
    Entity entity = underlaysService.getEntity(underlayName, entityName);

    ValidationUtils.validateApiFilter(body.getFilter());

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
                    .map(
                        entityInstanceCount ->
                            ToApiConversionUtils.toApiObject(entityInstanceCount))
                    .collect(Collectors.toList()))
            .sql(entityCountResult.getSql()));
  }

  @Override
  public ResponseEntity<ApiExportFile> exportInstances(
      String underlayName, String entityName, ApiQueryV2 body) {
    accessControlService.throwIfUnauthorized(
        SpringAuthentication.getCurrentUser(),
        QUERY_INSTANCES,
        UNDERLAY,
        ResourceId.forUnderlay(underlayName));

    ValidationUtils.validateApiFilter(body.getFilter());
    Entity entity = underlaysService.getEntity(underlayName, entityName);
    List<Attribute> selectAttributes = selectAttributesFromRequest(body, entity);
    List<HierarchyField> selectHierarchyFields = selectHierarchyFieldsFromRequest(body, entity);
    List<RelationshipField> selectRelationshipFields =
        selectRelationshipFieldsFromRequest(body, entity, underlayName);
    List<EntityQueryOrderBy> entityOrderBys = entityOrderBysFromRequest(body, entity, underlayName);
    EntityFilter entityFilter = null;
    if (body.getFilter() != null) {
      entityFilter = fromApiConversionService.fromApiObject(body.getFilter(), entity, underlayName);
    }

    ExportResult exportResult =
        dataExportService.run(
            ExportRequest.builder()
                .model(DataExport.Model.LIST_OF_SIGNED_URLS)
                .includeAnnotations(false),
            List.of(
                new EntityQueryRequest.Builder()
                    .entity(entity)
                    .mappingType(Underlay.MappingType.INDEX)
                    .selectAttributes(selectAttributes)
                    .selectHierarchyFields(selectHierarchyFields)
                    .selectRelationshipFields(selectRelationshipFields)
                    .filter(entityFilter)
                    .orderBys(entityOrderBys)
                    .limit(body.getLimit())
                    .build()));
    String gcsSignedUrl = exportResult.getOutputs().get("entity:" + entityName);
    return ResponseEntity.ok(new ApiExportFile().gcsSignedUrl(gcsSignedUrl));
  }

  private List<Attribute> selectAttributesFromRequest(ApiQueryV2 body, Entity entity) {
    List<Attribute> selectAttributes = new ArrayList<>();
    if (body.getIncludeAttributes() != null) {
      selectAttributes =
          body.getIncludeAttributes().stream()
              .map(attrName -> underlaysService.getAttribute(entity, attrName))
              .collect(Collectors.toList());
    }
    return selectAttributes;
  }

  private static List<HierarchyField> selectHierarchyFieldsFromRequest(
      ApiQueryV2 body, Entity entity) {
    List<HierarchyField> selectHierarchyFields = new ArrayList<>();
    if (body.getIncludeHierarchyFields() != null) {
      // for each hierarchy, return all the fields specified
      body.getIncludeHierarchyFields().getHierarchies().stream()
          .forEach(
              hierarchyName -> {
                Hierarchy hierarchy = entity.getHierarchy(hierarchyName);
                body.getIncludeHierarchyFields().getFields().stream()
                    .forEach(
                        hierarchyFieldName ->
                            selectHierarchyFields.add(
                                hierarchy.getField(
                                    HierarchyField.Type.valueOf(hierarchyFieldName.name()))));
              });
    }
    return selectHierarchyFields;
  }

  private List<RelationshipField> selectRelationshipFieldsFromRequest(
      ApiQueryV2 body, Entity entity, String underlayName) {
    List<RelationshipField> selectRelationshipFields = new ArrayList<>();
    if (body.getIncludeRelationshipFields() != null) {
      // for each related entity, return all the fields specified
      body.getIncludeRelationshipFields().stream()
          .forEach(
              includeRelationshipField -> {
                Entity relatedEntity =
                    underlaysService.getEntity(
                        underlayName, includeRelationshipField.getRelatedEntity());
                List<Hierarchy> hierarchies = new ArrayList<>();
                hierarchies.add(null); // Always return the NO_HIERARCHY rollups.
                if (includeRelationshipField.getHierarchies() != null
                    && !includeRelationshipField.getHierarchies().isEmpty()) {
                  includeRelationshipField.getHierarchies().stream()
                      .forEach(
                          hierarchyName -> hierarchies.add(entity.getHierarchy(hierarchyName)));
                }

                hierarchies.stream()
                    .forEach(
                        hierarchy -> {
                          Relationship relationship = entity.getRelationship(relatedEntity);
                          selectRelationshipFields.add(
                              relationship.getField(
                                  RelationshipField.Type.COUNT, entity, hierarchy));
                        });
              });
    }
    return selectRelationshipFields;
  }

  private List<EntityQueryOrderBy> entityOrderBysFromRequest(
      ApiQueryV2 body, Entity entity, String underlayName) {
    List<EntityQueryOrderBy> entityOrderBys = new ArrayList<>();
    if (body.getOrderBys() != null) {
      body.getOrderBys().stream()
          .forEach(
              orderBy -> {
                OrderByDirection direction =
                    orderBy.getDirection() == null
                        ? OrderByDirection.ASCENDING
                        : OrderByDirection.valueOf(orderBy.getDirection().name());
                String attrName = orderBy.getAttribute();
                if (attrName != null) {
                  entityOrderBys.add(
                      new EntityQueryOrderBy(
                          underlaysService.getAttribute(entity, attrName), direction));
                } else {
                  Entity relatedEntity =
                      underlaysService.getEntity(
                          underlayName, orderBy.getRelationshipField().getRelatedEntity());
                  Relationship relationship = entity.getRelationship(relatedEntity);

                  String hierName = orderBy.getRelationshipField().getHierarchy();
                  Hierarchy hierarchy = hierName == null ? null : entity.getHierarchy(hierName);
                  entityOrderBys.add(
                      new EntityQueryOrderBy(
                          relationship.getField(RelationshipField.Type.COUNT, entity, hierarchy),
                          direction));
                }
              });
    }
    return entityOrderBys;
  }
}

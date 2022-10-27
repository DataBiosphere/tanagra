package bio.terra.tanagra.app.controller;

import bio.terra.tanagra.api.EntityFilter;
import bio.terra.tanagra.api.EntityInstance;
import bio.terra.tanagra.api.EntityInstanceCount;
import bio.terra.tanagra.api.EntityQueryRequest;
import bio.terra.tanagra.api.FromApiConversionService;
import bio.terra.tanagra.api.QuerysService;
import bio.terra.tanagra.api.UnderlaysService;
import bio.terra.tanagra.api.utils.ToApiConversionUtils;
import bio.terra.tanagra.exception.SystemException;
import bio.terra.tanagra.generated.controller.InstancesV2Api;
import bio.terra.tanagra.generated.model.ApiCountQueryV2;
import bio.terra.tanagra.generated.model.ApiInstanceCountListV2;
import bio.terra.tanagra.generated.model.ApiInstanceCountV2;
import bio.terra.tanagra.generated.model.ApiInstanceListV2;
import bio.terra.tanagra.generated.model.ApiInstanceV2;
import bio.terra.tanagra.generated.model.ApiInstanceV2HierarchyFields;
import bio.terra.tanagra.generated.model.ApiInstanceV2RelationshipFields;
import bio.terra.tanagra.generated.model.ApiQueryV2;
import bio.terra.tanagra.generated.model.ApiValueDisplayV2;
import bio.terra.tanagra.query.OrderByDirection;
import bio.terra.tanagra.query.QueryRequest;
import bio.terra.tanagra.underlay.Attribute;
import bio.terra.tanagra.underlay.Entity;
import bio.terra.tanagra.underlay.EntityMapping;
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

  @Autowired
  public InstancesV2ApiController(
      UnderlaysService underlaysService,
      QuerysService querysService,
      FromApiConversionService fromApiConversionService) {
    this.underlaysService = underlaysService;
    this.querysService = querysService;
    this.fromApiConversionService = fromApiConversionService;
  }

  @Override
  public ResponseEntity<ApiInstanceListV2> queryInstances(
      String underlayName, String entityName, ApiQueryV2 body) {
    Entity entity = underlaysService.getEntity(underlayName, entityName);
    // TODO: Allow building queries against the source data mapping also.
    EntityMapping entityMapping = entity.getMapping(Underlay.MappingType.INDEX);

    List<Attribute> selectAttributes = new ArrayList<>();
    if (body.getIncludeAttributes() != null) {
      selectAttributes =
          body.getIncludeAttributes().stream()
              .map(attrName -> querysService.getAttribute(entity, attrName))
              .collect(Collectors.toList());
    }
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
                          includeRelationshipField.getFields().stream()
                              .forEach(
                                  relationshipFieldName ->
                                      selectRelationshipFields.add(
                                          relationship.getField(
                                              RelationshipField.Type.valueOf(
                                                  relationshipFieldName.name()),
                                              entity,
                                              hierarchy)));
                        });
              });
    }

    List<Attribute> orderByAttributes = new ArrayList<>();
    OrderByDirection orderByDirection = OrderByDirection.ASCENDING;
    if (body.getOrderBy() != null) {
      orderByAttributes =
          body.getOrderBy().getAttributes().stream()
              .map(attrName -> querysService.getAttribute(entity, attrName))
              .collect(Collectors.toList());
      orderByDirection = OrderByDirection.valueOf(body.getOrderBy().getDirection().name());
    }

    EntityFilter entityFilter = null;
    if (body.getFilter() != null) {
      entityFilter = fromApiConversionService.fromApiObject(body.getFilter(), entity, underlayName);
    }

    QueryRequest queryRequest =
        querysService.buildInstancesQuery(
            new EntityQueryRequest.Builder()
                .entity(entity)
                .mappingType(Underlay.MappingType.INDEX)
                .selectAttributes(selectAttributes)
                .selectHierarchyFields(selectHierarchyFields)
                .selectRelationshipFields(selectRelationshipFields)
                .filter(entityFilter)
                .orderByAttributes(orderByAttributes)
                .orderByDirection(orderByDirection)
                .limit(body.getLimit())
                .build());
    List<EntityInstance> entityInstances =
        querysService.runInstancesQuery(
            entityMapping,
            selectAttributes,
            selectHierarchyFields,
            selectRelationshipFields,
            queryRequest);

    return ResponseEntity.ok(
        new ApiInstanceListV2()
            .instances(
                entityInstances.stream()
                    .map(entityInstance -> toApiObject(entityInstance))
                    .collect(Collectors.toList()))
            .sql(queryRequest.getSql()));
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
          hierarchyFieldSet.path(valueDisplay.getValue().getStringVal());
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
          relationshipFieldSet.count(Math.toIntExact(valueDisplay.getValue().getInt64Val()));
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
    Entity entity = underlaysService.getEntity(underlayName, entityName);

    List<Attribute> attributes = new ArrayList<>();
    if (body.getAttributes() != null) {
      attributes =
          body.getAttributes().stream()
              .map(attrName -> querysService.getAttribute(entity, attrName))
              .collect(Collectors.toList());
    }

    EntityFilter entityFilter = null;
    if (body.getFilter() != null) {
      entityFilter = fromApiConversionService.fromApiObject(body.getFilter(), entity, underlayName);
    }

    QueryRequest queryRequest =
        querysService.buildInstanceCountsQuery(
            entity, Underlay.MappingType.INDEX, attributes, entityFilter);
    List<EntityInstanceCount> entityInstanceCounts =
        querysService.runInstanceCountsQuery(
            entity.getMapping(Underlay.MappingType.INDEX).getTablePointer().getDataPointer(),
            attributes,
            queryRequest);

    return ResponseEntity.ok(
        new ApiInstanceCountListV2()
            .instanceCounts(
                entityInstanceCounts.stream()
                    .map(entityInstanceCount -> toApiObject(entityInstanceCount))
                    .collect(Collectors.toList()))
            .sql(queryRequest.getSql()));
  }

  private ApiInstanceCountV2 toApiObject(EntityInstanceCount entityInstanceCount) {
    ApiInstanceCountV2 instanceCount = new ApiInstanceCountV2();
    Map<String, ApiValueDisplayV2> attributes = new HashMap<>();
    for (Map.Entry<Attribute, ValueDisplay> attributeValue :
        entityInstanceCount.getAttributeValues().entrySet()) {
      attributes.put(
          attributeValue.getKey().getName(),
          ToApiConversionUtils.toApiObject(attributeValue.getValue()));
    }

    return instanceCount
        .count(Math.toIntExact(entityInstanceCount.getCount()))
        .attributes(attributes);
  }
}

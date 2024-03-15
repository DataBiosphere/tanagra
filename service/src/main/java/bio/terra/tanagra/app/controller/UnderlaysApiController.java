package bio.terra.tanagra.app.controller;

import static bio.terra.tanagra.service.accesscontrol.Action.QUERY_COUNTS;
import static bio.terra.tanagra.service.accesscontrol.Action.QUERY_INSTANCES;
import static bio.terra.tanagra.service.accesscontrol.Action.READ;
import static bio.terra.tanagra.service.accesscontrol.ResourceType.UNDERLAY;

import bio.terra.tanagra.api.field.AttributeField;
import bio.terra.tanagra.api.field.HierarchyIsMemberField;
import bio.terra.tanagra.api.field.HierarchyIsRootField;
import bio.terra.tanagra.api.field.HierarchyNumChildrenField;
import bio.terra.tanagra.api.field.HierarchyPathField;
import bio.terra.tanagra.api.field.RelatedEntityIdCountField;
import bio.terra.tanagra.api.field.ValueDisplayField;
import bio.terra.tanagra.api.filter.EntityFilter;
import bio.terra.tanagra.api.query.PageMarker;
import bio.terra.tanagra.api.query.count.CountQueryResult;
import bio.terra.tanagra.api.query.hint.HintInstance;
import bio.terra.tanagra.api.query.hint.HintQueryRequest;
import bio.terra.tanagra.api.query.hint.HintQueryResult;
import bio.terra.tanagra.api.query.list.ListInstance;
import bio.terra.tanagra.api.query.list.ListQueryRequest;
import bio.terra.tanagra.api.query.list.ListQueryResult;
import bio.terra.tanagra.api.shared.ValueDisplay;
import bio.terra.tanagra.app.authentication.SpringAuthentication;
import bio.terra.tanagra.app.controller.objmapping.FromApiUtils;
import bio.terra.tanagra.app.controller.objmapping.ToApiUtils;
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
import bio.terra.tanagra.generated.model.ApiUnderlaySerializedConfiguration;
import bio.terra.tanagra.generated.model.ApiUnderlaySummaryList;
import bio.terra.tanagra.generated.model.ApiValueDisplay;
import bio.terra.tanagra.service.UnderlayService;
import bio.terra.tanagra.service.accesscontrol.AccessControlService;
import bio.terra.tanagra.service.accesscontrol.Permissions;
import bio.terra.tanagra.service.accesscontrol.ResourceCollection;
import bio.terra.tanagra.service.accesscontrol.ResourceId;
import bio.terra.tanagra.underlay.ClientConfig;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.entitymodel.Entity;
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
  public ResponseEntity<ApiUnderlaySummaryList> listUnderlaySummaries() {
    ResourceCollection authorizedUnderlayNames =
        accessControlService.listAuthorizedResources(
            SpringAuthentication.getCurrentUser(), Permissions.forActions(UNDERLAY, READ));
    List<Underlay> authorizedUnderlays = underlayService.listUnderlays(authorizedUnderlayNames);
    ApiUnderlaySummaryList apiUnderlays = new ApiUnderlaySummaryList();
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
    Underlay underlay = underlayService.getUnderlay(underlayName);
    return ResponseEntity.ok(
        new ApiUnderlay()
            .summary(ToApiUtils.toApiObject(underlay))
            .serializedConfiguration(toApiObject(underlay.getClientConfig()))
            .uiConfiguration(underlay.getUiConfig()));
  }

  @Override
  public ResponseEntity<ApiEntityList> listEntities(String underlayName) {
    accessControlService.throwIfUnauthorized(
        SpringAuthentication.getCurrentUser(),
        Permissions.forActions(UNDERLAY, READ),
        ResourceId.forUnderlay(underlayName));
    ApiEntityList apiEntities = new ApiEntityList();
    underlayService.getUnderlay(underlayName).getEntities().stream()
        .forEach(entity -> apiEntities.addEntitiesItem(toApiObject(entity)));
    return ResponseEntity.ok(apiEntities);
  }

  @Override
  public ResponseEntity<ApiEntity> getEntity(String underlayName, String entityName) {
    accessControlService.throwIfUnauthorized(
        SpringAuthentication.getCurrentUser(),
        Permissions.forActions(UNDERLAY, READ),
        ResourceId.forUnderlay(underlayName));
    Entity entity = underlayService.getUnderlay(underlayName).getEntity(entityName);
    return ResponseEntity.ok(toApiObject(entity));
  }

  @Override
  public ResponseEntity<ApiInstanceListResult> listInstances(
      String underlayName, String entityName, ApiQuery body) {
    accessControlService.throwIfUnauthorized(
        SpringAuthentication.getCurrentUser(),
        Permissions.forActions(UNDERLAY, QUERY_INSTANCES),
        ResourceId.forUnderlay(underlayName));
    Underlay underlay = underlayService.getUnderlay(underlayName);
    ListQueryRequest listQueryRequest =
        FromApiUtils.fromApiObject(body, underlay.getEntity(entityName), underlay);

    // Run the list query and map the results back to API objects.
    ListQueryResult listQueryResult = underlay.getQueryRunner().run(listQueryRequest);
    return ResponseEntity.ok(
        new ApiInstanceListResult()
            .instances(
                listQueryResult.getListInstances().stream()
                    .map(listInstance -> toApiObject(listInstance))
                    .collect(Collectors.toList()))
            .sql(SqlFormatter.format(listQueryResult.getSqlNoParams()))
            .pageMarker(
                listQueryResult.getPageMarker() == null
                    ? null
                    : listQueryResult.getPageMarker().serialize()));
  }

  @Override
  public ResponseEntity<ApiInstanceCountList> countInstances(
      String underlayName, String entityName, ApiCountQuery body) {
    accessControlService.throwIfUnauthorized(
        SpringAuthentication.getCurrentUser(),
        Permissions.forActions(UNDERLAY, QUERY_COUNTS),
        ResourceId.forUnderlay(underlayName));

    // Build the entity filter.
    Underlay underlay = underlayService.getUnderlay(underlayName);
    Entity entity = underlay.getEntity(entityName);
    EntityFilter filter =
        body.getFilter() == null
            ? null
            : FromApiUtils.fromApiObject(body.getFilter(), entity, underlay);

    // Run the count query and map the results back to API objects.
    CountQueryResult countQueryResult =
        underlayService.runCountQuery(
            underlay,
            entity,
            body.getAttributes() == null ? List.of() : body.getAttributes(),
            filter,
            PageMarker.deserialize(body.getPageMarker()),
            body.getPageSize());
    return ResponseEntity.ok(ToApiUtils.toApiObject(countQueryResult));
  }

  @Override
  public ResponseEntity<ApiDisplayHintList> queryHints(
      String underlayName, String entityName, ApiHintQuery body) {
    accessControlService.throwIfUnauthorized(
        SpringAuthentication.getCurrentUser(),
        Permissions.forActions(UNDERLAY, QUERY_COUNTS),
        ResourceId.forUnderlay(underlayName));
    Underlay underlay = underlayService.getUnderlay(underlayName);
    Entity entity = underlay.getEntity(entityName);

    boolean isEntityLevelHints = body == null || body.getRelatedEntity() == null;
    HintQueryRequest hintQueryRequest;
    if (isEntityLevelHints) {
      hintQueryRequest = new HintQueryRequest(underlay, entity, null, null, null, false);
    } else { // isInstanceLevelHints
      Entity relatedEntity = underlay.getEntity(body.getRelatedEntity().getName());
      hintQueryRequest =
          new HintQueryRequest(
              underlay,
              entity,
              relatedEntity,
              FromApiUtils.fromApiObject(body.getRelatedEntity().getId()),
              underlay.getRelationship(entity, relatedEntity).getLeft(),
              false);
    }
    HintQueryResult hintQueryResult = underlay.getQueryRunner().run(hintQueryRequest);
    return ResponseEntity.ok(
        new ApiDisplayHintList()
            .sql(SqlFormatter.format(hintQueryResult.getSql()))
            .displayHints(
                hintQueryResult.getHintInstances().stream()
                    .map(hintInstance -> toApiObject(hintInstance))
                    .collect(Collectors.toList())));
  }

  private ApiUnderlaySerializedConfiguration toApiObject(ClientConfig clientConfig) {
    return new ApiUnderlaySerializedConfiguration()
        .underlay(clientConfig.serializeUnderlay())
        .entities(clientConfig.serializeEntities())
        .groupItemsEntityGroups(clientConfig.serializeGroupItemsEntityGroups())
        .criteriaOccurrenceEntityGroups(clientConfig.serializeCriteriaOccurrenceEntityGroups())
        .criteriaSelectors(clientConfig.serializeCriteriaSelectors())
        .prepackagedDataFeatures(clientConfig.serializePrepackagedDataFeatures());
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

  private static ApiInstance toApiObject(ListInstance listInstance) {
    Map<String, ApiValueDisplay> attributes = new HashMap<>();
    Map<String, ApiInstanceHierarchyFields> hierarchyFieldSets = new HashMap<>();
    List<ApiInstanceRelationshipFields> relationshipFieldSets = new ArrayList<>();
    listInstance.getEntityFieldValues().entrySet().stream()
        .forEach(
            fieldValuePair -> {
              ValueDisplayField field = fieldValuePair.getKey();
              ValueDisplay value = fieldValuePair.getValue();

              if (field instanceof AttributeField) {
                attributes.put(
                    ((AttributeField) field).getAttribute().getName(),
                    ToApiUtils.toApiObject(value));
              } else if (field instanceof HierarchyPathField) {
                getHierarchyFieldSet(
                        hierarchyFieldSets, ((HierarchyPathField) field).getHierarchy().getName())
                    .setPath(value.getValue().getStringVal());
              } else if (field instanceof HierarchyNumChildrenField) {
                getHierarchyFieldSet(
                        hierarchyFieldSets,
                        ((HierarchyNumChildrenField) field).getHierarchy().getName())
                    .setNumChildren(Math.toIntExact(value.getValue().getInt64Val()));
              } else if (field instanceof HierarchyIsRootField) {
                getHierarchyFieldSet(
                        hierarchyFieldSets, ((HierarchyIsRootField) field).getHierarchy().getName())
                    .setIsRoot(value.getValue().getBooleanVal());
              } else if (field instanceof HierarchyIsMemberField) {
                getHierarchyFieldSet(
                        hierarchyFieldSets,
                        ((HierarchyIsMemberField) field).getHierarchy().getName())
                    .setIsMember(value.getValue().getBooleanVal());
              } else if (field instanceof RelatedEntityIdCountField) {
                RelatedEntityIdCountField countField = (RelatedEntityIdCountField) field;
                relationshipFieldSets.add(
                    new ApiInstanceRelationshipFields()
                        .relatedEntity(countField.getCountedEntity().getName())
                        .hierarchy(
                            countField.getHierarchy() == null
                                ? null
                                : countField.getHierarchy().getName())
                        .count(Math.toIntExact(value.getValue().getInt64Val())));
              }
            });
    return new ApiInstance()
        .attributes(attributes)
        .hierarchyFields(hierarchyFieldSets.values().stream().collect(Collectors.toList()))
        .relationshipFields(relationshipFieldSets);
  }

  private static ApiInstanceHierarchyFields getHierarchyFieldSet(
      Map<String, ApiInstanceHierarchyFields> hierarchyFieldSets, String hierarchyName) {
    if (!hierarchyFieldSets.containsKey(hierarchyName)) {
      hierarchyFieldSets.put(hierarchyName, new ApiInstanceHierarchyFields());
    }
    return hierarchyFieldSets.get(hierarchyName);
  }

  private ApiDisplayHint toApiObject(HintInstance hintInstance) {
    ApiDisplayHintDisplayHint apiHint = null;
    if (hintInstance.isEnumHint()) {
      apiHint =
          new ApiDisplayHintDisplayHint()
              .enumHint(
                  new ApiDisplayHintEnum()
                      .enumHintValues(
                          hintInstance.getEnumValueCounts().entrySet().stream()
                              .map(
                                  entry -> {
                                    ValueDisplay attributeValueDisplay = entry.getKey();
                                    Long count = entry.getValue();
                                    return new ApiDisplayHintEnumEnumHintValues()
                                        .enumVal(ToApiUtils.toApiObject(attributeValueDisplay))
                                        .count(Math.toIntExact(count));
                                  })
                              .collect(Collectors.toList())));
    } else if (hintInstance.isRangeHint()) {
      apiHint =
          new ApiDisplayHintDisplayHint()
              .numericRangeHint(
                  new ApiDisplayHintNumericRange()
                      .min(hintInstance.getMin())
                      .max(hintInstance.getMax()));
    }
    return new ApiDisplayHint()
        .attribute(ToApiUtils.toApiObject(hintInstance.getAttribute()))
        .displayHint(apiHint);
  }
}

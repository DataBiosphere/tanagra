package bio.terra.tanagra.app.controller;

import static bio.terra.tanagra.service.accesscontrol.Action.READ;
import static bio.terra.tanagra.service.accesscontrol.ResourceType.UNDERLAY;

import bio.terra.common.exception.BadRequestException;
import bio.terra.tanagra.api.field.AttributeField;
import bio.terra.tanagra.api.field.ValueDisplayField;
import bio.terra.tanagra.api.filter.EntityFilter;
import bio.terra.tanagra.api.query.PageMarker;
import bio.terra.tanagra.api.query.count.CountQueryResult;
import bio.terra.tanagra.api.query.hint.HintInstance;
import bio.terra.tanagra.api.query.hint.HintQueryRequest;
import bio.terra.tanagra.api.query.hint.HintQueryResult;
import bio.terra.tanagra.api.query.list.ListQueryRequest;
import bio.terra.tanagra.api.query.list.ListQueryResult;
import bio.terra.tanagra.api.query.list.OrderBy;
import bio.terra.tanagra.api.shared.Literal;
import bio.terra.tanagra.api.shared.OrderByDirection;
import bio.terra.tanagra.app.authentication.SpringAuthentication;
import bio.terra.tanagra.app.controller.objmapping.FromApiUtils;
import bio.terra.tanagra.app.controller.objmapping.ToApiUtils;
import bio.terra.tanagra.generated.controller.UnderlaysApi;
import bio.terra.tanagra.generated.model.ApiCountQuery;
import bio.terra.tanagra.generated.model.ApiCriteriaCountQuery;
import bio.terra.tanagra.generated.model.ApiDisplayHint;
import bio.terra.tanagra.generated.model.ApiDisplayHintDisplayHint;
import bio.terra.tanagra.generated.model.ApiDisplayHintEnum;
import bio.terra.tanagra.generated.model.ApiDisplayHintEnumEnumHintValues;
import bio.terra.tanagra.generated.model.ApiDisplayHintList;
import bio.terra.tanagra.generated.model.ApiDisplayHintNumericRange;
import bio.terra.tanagra.generated.model.ApiEntity;
import bio.terra.tanagra.generated.model.ApiEntityList;
import bio.terra.tanagra.generated.model.ApiHintQuery;
import bio.terra.tanagra.generated.model.ApiInstanceCountList;
import bio.terra.tanagra.generated.model.ApiInstanceListResult;
import bio.terra.tanagra.generated.model.ApiQuery;
import bio.terra.tanagra.generated.model.ApiQueryFilterOnPrimaryEntity;
import bio.terra.tanagra.generated.model.ApiUnderlay;
import bio.terra.tanagra.generated.model.ApiUnderlaySerializedConfiguration;
import bio.terra.tanagra.generated.model.ApiUnderlaySummaryList;
import bio.terra.tanagra.generated.model.ApiValueDisplay;
import bio.terra.tanagra.service.UnderlayService;
import bio.terra.tanagra.service.accesscontrol.AccessControlService;
import bio.terra.tanagra.service.accesscontrol.Permissions;
import bio.terra.tanagra.service.accesscontrol.ResourceCollection;
import bio.terra.tanagra.service.accesscontrol.ResourceId;
import bio.terra.tanagra.service.artifact.model.CohortRevision.CriteriaGroupSection;
import bio.terra.tanagra.service.filter.FilterBuilderService;
import bio.terra.tanagra.underlay.ClientConfig;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.entitymodel.Attribute;
import bio.terra.tanagra.underlay.entitymodel.Entity;
import bio.terra.tanagra.utils.SqlFormatter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.CollectionUtils;

@Controller
public class UnderlaysApiController implements UnderlaysApi {
  private final UnderlayService underlayService;
  private final FilterBuilderService filterBuilderService;
  private final AccessControlService accessControlService;
  private final ApiDisplayHintComparator apiDisplayHintComparator = new ApiDisplayHintComparator();
  private final ApiDisplayHintEnumValuesComparator apiDisplayHintEnumValuesComparator =
      new ApiDisplayHintEnumValuesComparator();

  @Autowired
  public UnderlaysApiController(
      UnderlayService underlayService,
      FilterBuilderService filterBuilderService,
      AccessControlService accessControlService) {
    this.underlayService = underlayService;
    this.filterBuilderService = filterBuilderService;
    this.accessControlService = accessControlService;
  }

  @Override
  public ResponseEntity<ApiUnderlaySummaryList> listUnderlaySummaries() {
    ResourceCollection authorizedUnderlayNames =
        accessControlService.listAuthorizedResources(
            SpringAuthentication.getCurrentUser(),
            Permissions.forActions(UNDERLAY, READ),
            (ResourceId) null,
            0,
            1000);
    List<Underlay> authorizedUnderlays = underlayService.listUnderlays(authorizedUnderlayNames);
    ApiUnderlaySummaryList apiUnderlays = new ApiUnderlaySummaryList();
    authorizedUnderlays.forEach(
        underlay -> apiUnderlays.addUnderlaysItem(ToApiUtils.toApiObject(underlay)));
    return ResponseEntity.ok(apiUnderlays);
  }

  @Override
  public ResponseEntity<ApiUnderlay> getUnderlay(String underlayName) {
    accessControlService.checkUnderlayAccess(underlayName);
    Underlay underlay = underlayService.getUnderlay(underlayName);
    return ResponseEntity.ok(
        new ApiUnderlay()
            .summary(ToApiUtils.toApiObject(underlay))
            .serializedConfiguration(toApiObject(underlay.getClientConfig()))
            .uiConfiguration(underlay.getUiConfig()));
  }

  @Override
  public ResponseEntity<ApiEntityList> listEntities(String underlayName) {
    accessControlService.checkUnderlayAccess(underlayName);
    ApiEntityList apiEntities = new ApiEntityList();
    underlayService
        .getUnderlay(underlayName)
        .getEntities()
        .forEach(entity -> apiEntities.addEntitiesItem(toApiObject(entity)));
    return ResponseEntity.ok(apiEntities);
  }

  @Override
  public ResponseEntity<ApiEntity> getEntity(String underlayName, String entityName) {
    accessControlService.checkUnderlayAccess(underlayName);
    Entity entity = underlayService.getUnderlay(underlayName).getEntity(entityName);
    return ResponseEntity.ok(toApiObject(entity));
  }

  @Override
  public ResponseEntity<ApiInstanceListResult> listInstances(
      String underlayName, String entityName, ApiQuery body) {
    accessControlService.checkUnderlayAccess(underlayName);
    Underlay underlay = underlayService.getUnderlay(underlayName);
    ListQueryRequest listQueryRequest =
        FromApiUtils.fromApiObject(body, underlay.getEntity(entityName), underlay);

    // Run the list query and map the results back to API objects.
    ListQueryResult listQueryResult = underlay.getQueryRunner().run(listQueryRequest);
    return ResponseEntity.ok(ToApiUtils.toApiObject(listQueryResult));
  }

  @Override
  public ResponseEntity<ApiInstanceListResult> listInstancesForPrimaryEntity(
      String underlayName, String entityName, ApiQueryFilterOnPrimaryEntity body) {
    accessControlService.checkUnderlayAccess(underlayName);
    Underlay underlay = underlayService.getUnderlay(underlayName);

    // Build the attribute fields to select.
    Entity outputEntity = underlay.getEntity(entityName);
    List<ValueDisplayField> selectFields =
        outputEntity.getAttributes().stream()
            .filter(
                attribute ->
                    CollectionUtils.isEmpty(body.getIncludeAttributes())
                        || body.getIncludeAttributes().contains(attribute.getName()))
            .map(attribute -> new AttributeField(underlay, outputEntity, attribute, false))
            .collect(Collectors.toList());

    // Build the filter on the output entity.
    Literal primaryEntityId = FromApiUtils.fromApiObject(body.getPrimaryEntityId());
    EntityFilter outputEntityFilter =
        filterBuilderService.buildFilterForPrimaryEntityId(
            underlayName, entityName, primaryEntityId);

    // Build the order by fields.
    List<OrderBy> orderByFields = new ArrayList<>();
    if (body.getOrderBys() != null) {
      body.getOrderBys()
          .forEach(
              orderByField -> {
                Attribute attribute = outputEntity.getAttribute(orderByField.getAttribute());
                ValueDisplayField valueDisplayField =
                    new AttributeField(underlay, outputEntity, attribute, false);
                OrderByDirection direction =
                    OrderByDirection.valueOf(orderByField.getDirection().name());
                orderByFields.add(new OrderBy(valueDisplayField, direction));
              });
    }

    // Run the list query and map the results back to API objects.
    ListQueryRequest listQueryRequest =
        ListQueryRequest.againstIndexData(
            underlay,
            outputEntity,
            selectFields,
            outputEntityFilter,
            orderByFields,
            null,
            PageMarker.deserialize(body.getPageMarker()),
            body.getPageSize());
    ListQueryResult listQueryResult = underlay.getQueryRunner().run(listQueryRequest);
    return ResponseEntity.ok(ToApiUtils.toApiObject(listQueryResult));
  }

  @Override
  public ResponseEntity<ApiInstanceCountList> countInstances(
      String underlayName, String entityName, ApiCountQuery body) {
    accessControlService.checkUnderlayAccess(underlayName);

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
            null,
            body.getAttributes() == null ? List.of() : body.getAttributes(),
            filter,
            OrderByDirection.DESCENDING,
            null,
            PageMarker.deserialize(body.getPageMarker()),
            body.getPageSize());
    return ResponseEntity.ok(ToApiUtils.toApiObject(countQueryResult));
  }

  @Override
  public ResponseEntity<ApiDisplayHintList> queryHints(
      String underlayName, String entityName, ApiHintQuery body) {
    accessControlService.checkUnderlayAccess(underlayName);
    Underlay underlay = underlayService.getUnderlay(underlayName);
    Entity entity = underlay.getEntity(entityName);
    HintQueryResult hintQueryResult;

    if (body.getFilter() != null) { // dynamic entity level hints
      if (body.getRelatedEntity() != null) {
        throw new BadRequestException("Only one of relatedEntity and filter may be provided");
      }

      EntityFilter hintFilter = FromApiUtils.fromApiObject(body.getFilter(), entity, underlay);
      hintQueryResult = underlayService.getEntityLevelHints(underlay, entity, hintFilter);

    } else if (body.getRelatedEntity() != null) { // instance level hints
      Entity relatedEntity = underlay.getEntity(body.getRelatedEntity().getName());
      HintQueryRequest hintQueryRequest =
          new HintQueryRequest(
              underlay,
              entity,
              relatedEntity,
              FromApiUtils.fromApiObject(body.getRelatedEntity().getId()),
              underlay.getRelationship(entity, relatedEntity).getLeft(),
              false);
      hintQueryResult = underlay.getQueryRunner().run(hintQueryRequest);

    } else { // static entity level hints
      hintQueryResult = underlayService.getEntityLevelHints(underlay, entity);
    }

    ApiDisplayHintList displayHintList =
        new ApiDisplayHintList()
            .sql(SqlFormatter.format(hintQueryResult.getSql()))
            .displayHints(
                hintQueryResult.getHintInstances().stream()
                    .map(this::toApiObject)
                    .sorted(apiDisplayHintComparator)
                    .toList());
    return ResponseEntity.ok(displayHintList);
  }

  @Override
  public ResponseEntity<ApiInstanceCountList> queryCriteriaCounts(
      String underlayName, ApiCriteriaCountQuery body) {
    accessControlService.checkUnderlayAccess(underlayName);

    // Build the entity filter.
    List<CriteriaGroupSection> criteriaGroupSections =
        body.getCriteriaGroupSections().stream().map(FromApiUtils::fromApiObject).toList();
    EntityFilter criteriaFilter =
        filterBuilderService.buildFilterForCriteriaGroupSections(
            underlayName, criteriaGroupSections);

    Underlay underlay = underlayService.getUnderlay(underlayName);
    Entity outputEntity =
        body.getEntity() == null
            ? underlay.getPrimaryEntity()
            : underlay.getEntity(body.getEntity());
    EntityFilter outputEntityFilteredOnCriteria =
        filterBuilderService.filterOutputByPrimaryEntity(
            underlay, outputEntity, null, criteriaFilter);

    // Run the count query and map the results back to API objects.
    CountQueryResult countQueryResult =
        underlayService.runCountQuery(
            underlay,
            outputEntity,
            body.getCountDistinctAttribute(),
            body.getGroupByAttributes(),
            outputEntityFilteredOnCriteria,
            body.getOrderByDirection() == null
                ? OrderByDirection.DESCENDING
                : OrderByDirection.valueOf(body.getOrderByDirection().name()),
            body.getLimit(),
            PageMarker.deserialize(body.getPageMarker()),
            body.getPageSize());
    return ResponseEntity.ok(ToApiUtils.toApiObject(countQueryResult));
  }

  private ApiUnderlaySerializedConfiguration toApiObject(ClientConfig clientConfig) {
    return new ApiUnderlaySerializedConfiguration()
        .underlay(clientConfig.serializeUnderlay())
        .entities(clientConfig.serializeEntities())
        .groupItemsEntityGroups(clientConfig.serializeGroupItemsEntityGroups())
        .criteriaOccurrenceEntityGroups(clientConfig.serializeCriteriaOccurrenceEntityGroups())
        .criteriaSelectors(clientConfig.serializeCriteriaSelectors())
        .prepackagedDataFeatures(clientConfig.serializePrepackagedDataFeatures())
        .visualizations(clientConfig.serializeVisualizations());
  }

  private ApiEntity toApiObject(Entity entity) {
    return new ApiEntity()
        .name(entity.getName())
        .idAttribute(entity.getIdAttribute().getName())
        .attributes(
            entity.getAttributes().stream()
                .map(ToApiUtils::toApiObject)
                .collect(Collectors.toList()));
  }

  private ApiDisplayHint toApiObject(HintInstance hintInstance) {
    ApiDisplayHintDisplayHint apiHint = new ApiDisplayHintDisplayHint();
    if (hintInstance.isEnumHint()) {
      apiHint.setEnumHint(
          new ApiDisplayHintEnum()
              .enumHintValues(
                  hintInstance.getEnumValueCounts().entrySet().stream()
                      .map(
                          entry ->
                              new ApiDisplayHintEnumEnumHintValues()
                                  .enumVal(ToApiUtils.toApiObject(entry.getKey()))
                                  .count(Math.toIntExact(entry.getValue())))
                      .sorted(apiDisplayHintEnumValuesComparator)
                      .collect(Collectors.toList())));
    } else if (hintInstance.isRangeHint()) {
      apiHint.setNumericRangeHint(
          new ApiDisplayHintNumericRange().min(hintInstance.getMin()).max(hintInstance.getMax()));
    }
    return new ApiDisplayHint()
        .attribute(ToApiUtils.toApiObject(hintInstance.getAttribute()))
        .displayHint(apiHint);
  }

  private static class ApiDisplayHintComparator
      implements Comparator<ApiDisplayHint>, Serializable {
    @Override
    public int compare(ApiDisplayHint o1, ApiDisplayHint o2) {
      // order by: attribute ASC
      return StringUtils.compare(o1.getAttribute().getName(), o2.getAttribute().getName());
    }
  }

  private static class ApiDisplayHintEnumValuesComparator
      implements Comparator<ApiDisplayHintEnumEnumHintValues>, Serializable {
    @Override
    public int compare(ApiDisplayHintEnumEnumHintValues o1, ApiDisplayHintEnumEnumHintValues o2) {
      // order by: display ASC, value ASC, count DESC
      ApiValueDisplay vd1 = o1.getEnumVal();
      ApiValueDisplay vd2 = o2.getEnumVal();
      int result = StringUtils.compare(vd1.getDisplay(), vd2.getDisplay(), false);
      result =
          (result != 0)
              ? result
              : StringUtils.compare(
                  vd1.getValue().getValueUnion().getStringVal(),
                  vd2.getValue().getValueUnion().getStringVal(),
                  false);
      return (result != 0) ? result : Integer.compare(o2.getCount(), o1.getCount());
    }
  }
}

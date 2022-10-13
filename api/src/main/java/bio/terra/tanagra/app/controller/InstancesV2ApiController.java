package bio.terra.tanagra.app.controller;

import bio.terra.tanagra.api.EntityFilter;
import bio.terra.tanagra.api.QuerysService;
import bio.terra.tanagra.api.UnderlaysService;
import bio.terra.tanagra.api.entityfilter.AttributeFilter;
import bio.terra.tanagra.api.entityfilter.HierarchyAncestorFilter;
import bio.terra.tanagra.api.entityfilter.HierarchyParentFilter;
import bio.terra.tanagra.api.entityfilter.HierarchyRootFilter;
import bio.terra.tanagra.api.entityfilter.TextFilter;
import bio.terra.tanagra.api.utils.FromApiConversionUtils;
import bio.terra.tanagra.api.utils.ToApiConversionUtils;
import bio.terra.tanagra.exception.SystemException;
import bio.terra.tanagra.generated.controller.InstancesV2Api;
import bio.terra.tanagra.generated.model.ApiAttributeFilterV2;
import bio.terra.tanagra.generated.model.ApiHierarchyFilterV2;
import bio.terra.tanagra.generated.model.ApiInstanceListV2;
import bio.terra.tanagra.generated.model.ApiInstanceV2;
import bio.terra.tanagra.generated.model.ApiQueryV2;
import bio.terra.tanagra.generated.model.ApiTextFilterV2;
import bio.terra.tanagra.query.OrderByDirection;
import bio.terra.tanagra.query.QueryRequest;
import bio.terra.tanagra.underlay.Attribute;
import bio.terra.tanagra.underlay.Entity;
import bio.terra.tanagra.underlay.EntityMapping;
import bio.terra.tanagra.underlay.HierarchyMapping;
import bio.terra.tanagra.underlay.ValueDisplay;
import java.util.ArrayList;
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

  @Autowired
  public InstancesV2ApiController(UnderlaysService underlaysService, QuerysService querysService) {
    this.underlaysService = underlaysService;
    this.querysService = querysService;
  }

  @Override
  public ResponseEntity<ApiInstanceListV2> queryInstances(
      String underlayName, String entityName, ApiQueryV2 body) {
    Entity entity = underlaysService.getEntity(underlayName, entityName);
    // TODO: Allow building queries against the source data mapping also.
    EntityMapping entityMapping = entity.getIndexDataMapping();

    List<Attribute> selectAttributes =
        body.getIncludeAttributes().stream()
            .map(attrName -> querysService.getAttribute(entity, attrName))
            .collect(Collectors.toList());
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
      switch (body.getFilter().getFilterType()) {
        case ATTRIBUTE:
          ApiAttributeFilterV2 apiAttributeFilter =
              body.getFilter().getFilterUnion().getAttributeFilter();
          entityFilter =
              new AttributeFilter(
                  entity,
                  entityMapping,
                  querysService.getAttribute(entity, apiAttributeFilter.getAttribute()),
                  FromApiConversionUtils.fromApiObject(apiAttributeFilter.getOperator()),
                  FromApiConversionUtils.fromApiObject(apiAttributeFilter.getValue()));
          break;
        case TEXT:
          ApiTextFilterV2 apiTextFilter = body.getFilter().getFilterUnion().getTextFilter();
          TextFilter.Builder textFilterBuilder =
              new TextFilter.Builder()
                  .entity(entity)
                  .entityMapping(entityMapping)
                  .functionTemplate(
                      FromApiConversionUtils.fromApiObject(apiTextFilter.getMatchType()))
                  .text(apiTextFilter.getText());
          if (apiTextFilter.getAttribute() != null) {
            textFilterBuilder.attribute(
                querysService.getAttribute(entity, apiTextFilter.getAttribute()));
          }
          entityFilter = textFilterBuilder.build();
          break;
        case HIERARCHY:
          ApiHierarchyFilterV2 apiHierarchyFilter =
              body.getFilter().getFilterUnion().getHierarchyFilter();
          HierarchyMapping hierarchyMapping =
              querysService.getHierarchy(entityMapping, apiHierarchyFilter.getHierarchy());
          switch (apiHierarchyFilter.getOperator()) {
            case IS_ROOT:
              entityFilter = new HierarchyRootFilter(entity, entityMapping, hierarchyMapping);
              break;
            case CHILD_OF:
              entityFilter =
                  new HierarchyParentFilter(
                      entity,
                      entityMapping,
                      hierarchyMapping,
                      FromApiConversionUtils.fromApiObject(apiHierarchyFilter.getValue()));
              break;
            case DESCENDANT_OF_INCLUSIVE:
              entityFilter =
                  new HierarchyAncestorFilter(
                      entity,
                      entityMapping,
                      hierarchyMapping,
                      FromApiConversionUtils.fromApiObject(apiHierarchyFilter.getValue()));
              break;
            default:
              throw new SystemException(
                  "Unknown API hierarchy filter operator: " + apiHierarchyFilter.getOperator());
          }
          break;
        default:
          throw new SystemException("Unknown API filter type: " + body.getFilter().getFilterType());
      }
    }

    QueryRequest queryRequest =
        querysService.buildInstancesQuery(
            entityMapping,
            selectAttributes,
            entityFilter,
            orderByAttributes,
            orderByDirection,
            body.getLimit());

    return ResponseEntity.ok(
        toApiObject(
            querysService.runInstancesQuery(entityMapping, selectAttributes, queryRequest),
            queryRequest.getSql()));
  }

  private ApiInstanceListV2 toApiObject(List<Map<String, ValueDisplay>> queryResults, String sql) {
    return new ApiInstanceListV2()
        .entities(
            queryResults.stream().map(result -> toApiObject(result)).collect(Collectors.toList()))
        .sql(sql);
  }

  private ApiInstanceV2 toApiObject(Map<String, ValueDisplay> result) {
    ApiInstanceV2 instance = new ApiInstanceV2();
    for (Map.Entry<String, ValueDisplay> selectedProperty : result.entrySet()) {
      instance.put(
          selectedProperty.getKey(), ToApiConversionUtils.toApiObject(selectedProperty.getValue()));
    }
    return instance;
  }
}

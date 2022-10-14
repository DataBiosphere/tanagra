package bio.terra.tanagra.app.controller;

import bio.terra.tanagra.api.EntityFilter;
import bio.terra.tanagra.api.EntityInstance;
import bio.terra.tanagra.api.FromApiConversionService;
import bio.terra.tanagra.api.QuerysService;
import bio.terra.tanagra.api.UnderlaysService;
import bio.terra.tanagra.api.utils.ToApiConversionUtils;
import bio.terra.tanagra.exception.SystemException;
import bio.terra.tanagra.generated.controller.InstancesV2Api;
import bio.terra.tanagra.generated.model.ApiInstanceListV2;
import bio.terra.tanagra.generated.model.ApiInstanceV2;
import bio.terra.tanagra.generated.model.ApiInstanceV2HierarchyFields;
import bio.terra.tanagra.generated.model.ApiQueryV2;
import bio.terra.tanagra.generated.model.ApiValueDisplayV2;
import bio.terra.tanagra.query.OrderByDirection;
import bio.terra.tanagra.query.QueryRequest;
import bio.terra.tanagra.underlay.Attribute;
import bio.terra.tanagra.underlay.Entity;
import bio.terra.tanagra.underlay.EntityMapping;
import bio.terra.tanagra.underlay.HierarchyField;
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
    EntityMapping entityMapping = entity.getIndexDataMapping();

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
                body.getIncludeHierarchyFields().getFields().stream()
                    .forEach(
                        hierarchyFieldName ->
                            selectHierarchyFields.add(
                                FromApiConversionService.fromApiObject(
                                    hierarchyName, hierarchyFieldName)));
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
      entityFilter =
          fromApiConversionService.fromApiObject(
              body.getFilter(), entity, entityMapping, underlayName);
    }

    QueryRequest queryRequest =
        querysService.buildInstancesQuery(
            entityMapping,
            selectAttributes,
            selectHierarchyFields,
            entityFilter,
            orderByAttributes,
            orderByDirection,
            body.getLimit());

    return ResponseEntity.ok(
        toApiObject(
            querysService.runInstancesQuery(
                entityMapping, selectAttributes, selectHierarchyFields, queryRequest),
            queryRequest.getSql()));
  }

  private ApiInstanceListV2 toApiObject(List<EntityInstance> entityInstances, String sql) {
    return new ApiInstanceListV2()
        .entities(
            entityInstances.stream()
                .map(entityInstance -> toApiObject(entityInstance))
                .collect(Collectors.toList()))
        .sql(sql);
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
          hierarchyFieldSets.get(hierarchyField.getHierarchyName());
      if (hierarchyFieldSet == null) {
        hierarchyFieldSet =
            new ApiInstanceV2HierarchyFields().hierarchy(hierarchyField.getHierarchyName());
        hierarchyFieldSets.put(hierarchyField.getHierarchyName(), hierarchyFieldSet);
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

    return instance
        .attributes(attributes)
        .hierarchyFields(hierarchyFieldSets.values().stream().collect(Collectors.toList()));
  }
}

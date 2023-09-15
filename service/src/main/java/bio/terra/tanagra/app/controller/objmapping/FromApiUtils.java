package bio.terra.tanagra.app.controller.objmapping;

import bio.terra.tanagra.api.query.*;
import bio.terra.tanagra.api.query.filter.*;
import bio.terra.tanagra.exception.InvalidConfigException;
import bio.terra.tanagra.exception.InvalidQueryException;
import bio.terra.tanagra.exception.SystemException;
import bio.terra.tanagra.generated.model.*;
import bio.terra.tanagra.query.Literal;
import bio.terra.tanagra.query.OrderByDirection;
import bio.terra.tanagra.query.PageMarker;
import bio.terra.tanagra.query.filtervariable.BinaryFilterVariable;
import bio.terra.tanagra.query.filtervariable.BooleanAndOrFilterVariable;
import bio.terra.tanagra.query.filtervariable.FunctionFilterVariable;
import bio.terra.tanagra.service.artifact.model.Criteria;
import bio.terra.tanagra.service.query.UnderlayService;
import bio.terra.tanagra.underlay.*;
import java.util.*;
import java.util.stream.Collectors;

public final class FromApiUtils {
  private FromApiUtils() {}

  public static EntityFilter fromApiObject(ApiFilterV2 apiFilter, Underlay underlay) {
    return fromApiObject(apiFilter, underlay.getPrimaryEntity(), underlay.getName());
  }

  public static EntityFilter fromApiObject(
      ApiFilterV2 apiFilter, Entity entity, String underlayName) {
    validateApiFilter(apiFilter);
    switch (apiFilter.getFilterType()) {
      case ATTRIBUTE:
        ApiAttributeFilterV2 apiAttributeFilter = apiFilter.getFilterUnion().getAttributeFilter();
        return new AttributeFilter(
            UnderlayService.getAttribute(entity, apiAttributeFilter.getAttribute()),
            FromApiUtils.fromApiObject(apiAttributeFilter.getOperator()),
            FromApiUtils.fromApiObject(apiAttributeFilter.getValue()));
      case TEXT:
        ApiTextFilterV2 apiTextFilter = apiFilter.getFilterUnion().getTextFilter();
        TextFilter.Builder textFilterBuilder =
            new TextFilter.Builder()
                .textSearch(entity.getTextSearch())
                .functionTemplate(FromApiUtils.fromApiObject(apiTextFilter.getMatchType()))
                .text(apiTextFilter.getText());
        if (apiTextFilter.getAttribute() != null) {
          textFilterBuilder.attribute(
              UnderlayService.getAttribute(entity, apiTextFilter.getAttribute()));
        }
        return textFilterBuilder.build();
      case HIERARCHY:
        ApiHierarchyFilterV2 apiHierarchyFilter = apiFilter.getFilterUnion().getHierarchyFilter();
        Hierarchy hierarchy =
            UnderlayService.getHierarchy(entity, apiHierarchyFilter.getHierarchy());
        switch (apiHierarchyFilter.getOperator()) {
          case IS_ROOT:
            return new HierarchyRootFilter(hierarchy);
          case IS_MEMBER:
            return new HierarchyMemberFilter(hierarchy);
          case CHILD_OF:
            return new HierarchyParentFilter(
                hierarchy, FromApiUtils.fromApiObject(apiHierarchyFilter.getValue()));
          case DESCENDANT_OF_INCLUSIVE:
            return new HierarchyAncestorFilter(
                hierarchy, FromApiUtils.fromApiObject(apiHierarchyFilter.getValue()));
          default:
            throw new SystemException(
                "Unknown API hierarchy filter operator: " + apiHierarchyFilter.getOperator());
        }
      case RELATIONSHIP:
        ApiRelationshipFilterV2 apiRelationshipFilter =
            apiFilter.getFilterUnion().getRelationshipFilter();
        Collection<EntityGroup> entityGroups = entity.getUnderlay().getEntityGroups().values();
        Entity relatedEntity = entity.getUnderlay().getEntity(apiRelationshipFilter.getEntity());
        Relationship relationship =
            UnderlayService.getRelationship(entityGroups, entity, relatedEntity);
        EntityFilter subFilter =
            fromApiObject(apiRelationshipFilter.getSubfilter(), relatedEntity, underlayName);

        Attribute groupByCountAttribute = null;
        BinaryFilterVariable.BinaryOperator groupByCountOperator = null;
        Integer groupByCountValue = null;
        if (apiRelationshipFilter.getGroupByCountOperator() != null
            && apiRelationshipFilter.getGroupByCountValue() != null) {
          groupByCountAttribute =
              apiRelationshipFilter.getGroupByCountAttribute() == null
                  ? null
                  : relatedEntity.getAttribute(apiRelationshipFilter.getGroupByCountAttribute());
          groupByCountOperator = fromApiObject(apiRelationshipFilter.getGroupByCountOperator());
          groupByCountValue = apiRelationshipFilter.getGroupByCountValue();
        }

        return new RelationshipFilter(
            entity,
            relationship,
            subFilter,
            groupByCountAttribute,
            groupByCountOperator,
            groupByCountValue);
      case BOOLEAN_LOGIC:
        ApiBooleanLogicFilterV2 apiBooleanLogicFilter =
            apiFilter.getFilterUnion().getBooleanLogicFilter();
        List<EntityFilter> subFilters =
            apiBooleanLogicFilter.getSubfilters().stream()
                .map(apiSubFilter -> fromApiObject(apiSubFilter, entity, underlayName))
                .collect(Collectors.toList());
        switch (apiBooleanLogicFilter.getOperator()) {
          case NOT:
            if (subFilters.size() != 1) {
              throw new InvalidQueryException(
                  "Boolean logic operator NOT can only have one sub-filter specified");
            }
            return new BooleanNotFilter(subFilters.get(0));
          case OR:
          case AND:
            if (subFilters.size() < 2) { // NOPMD - Allow using a literal in this conditional.
              throw new InvalidQueryException(
                  "Boolean logic operators OR, AND must have more than one sub-filter specified");
            }
            return new BooleanAndOrFilter(
                BooleanAndOrFilterVariable.LogicalOperator.valueOf(
                    apiBooleanLogicFilter.getOperator().name()),
                subFilters);
          default:
            throw new SystemException(
                "Unknown boolean logic operator: " + apiBooleanLogicFilter.getOperator());
        }
      default:
        throw new SystemException("Unknown API filter type: " + apiFilter.getFilterType());
    }
  }

  public static void validateApiFilter(ApiFilterV2 filter) {
    // If one RelationshipFilterV2 group_by field is set, all group_by fields must be set.
    if (filter != null && filter.getFilterType() == ApiFilterV2.FilterTypeEnum.RELATIONSHIP) {
      ApiRelationshipFilterV2 relationshipFilter = filter.getFilterUnion().getRelationshipFilter();
      if (!((relationshipFilter.getGroupByCountAttribute() == null
              && relationshipFilter.getGroupByCountOperator() == null
              && relationshipFilter.getGroupByCountValue() == null)
          || (relationshipFilter.getGroupByCountAttribute() != null
              && relationshipFilter.getGroupByCountOperator() != null
              && relationshipFilter.getGroupByCountValue() != null))) {
        throw new InvalidConfigException(
            "If one RelationshipFilterV2 group_by field is set, all group_by fields must be set");
      }
    }
  }

  public static EntityQueryRequest fromApiObject(ApiQueryV2 apiObj, Entity entity) {
    List<Attribute> selectAttributes = selectAttributesFromRequest(apiObj, entity);
    List<HierarchyField> selectHierarchyFields = selectHierarchyFieldsFromRequest(apiObj, entity);
    List<RelationshipField> selectRelationshipFields =
        selectRelationshipFieldsFromRequest(apiObj, entity);
    List<EntityQueryOrderBy> entityOrderBys = entityOrderBysFromRequest(apiObj, entity);
    EntityFilter entityFilter = null;
    if (apiObj.getFilter() != null) {
      entityFilter = fromApiObject(apiObj.getFilter(), entity, entity.getUnderlay().getName());
    }
    return new EntityQueryRequest.Builder()
        .entity(entity)
        .mappingType(Underlay.MappingType.INDEX)
        .selectAttributes(selectAttributes)
        .selectHierarchyFields(selectHierarchyFields)
        .selectRelationshipFields(selectRelationshipFields)
        .filter(entityFilter)
        .orderBys(entityOrderBys)
        .limit(apiObj.getLimit())
        .pageSize(apiObj.getPageSize())
        .pageMarker(PageMarker.deserialize(apiObj.getPageMarker()))
        .build();
  }

  public static Literal fromApiObject(ApiLiteralV2 apiLiteral) {
    switch (apiLiteral.getDataType()) {
      case INT64:
        return new Literal(apiLiteral.getValueUnion().getInt64Val());
      case STRING:
        return new Literal(apiLiteral.getValueUnion().getStringVal());
      case BOOLEAN:
        return new Literal(apiLiteral.getValueUnion().isBoolVal());
      case DATE:
        return Literal.forDate(apiLiteral.getValueUnion().getDateVal());
      default:
        throw new SystemException("Unknown API data type: " + apiLiteral.getDataType());
    }
  }

  public static BinaryFilterVariable.BinaryOperator fromApiObject(ApiBinaryOperatorV2 apiOperator) {
    return BinaryFilterVariable.BinaryOperator.valueOf(apiOperator.name());
  }

  public static FunctionFilterVariable.FunctionTemplate fromApiObject(
      ApiTextFilterV2.MatchTypeEnum apiMatchType) {
    switch (apiMatchType) {
      case EXACT_MATCH:
        return FunctionFilterVariable.FunctionTemplate.TEXT_EXACT_MATCH;
      case FUZZY_MATCH:
        return FunctionFilterVariable.FunctionTemplate.TEXT_FUZZY_MATCH;
      default:
        throw new SystemException("Unknown API text match type: " + apiMatchType.name());
    }
  }

  public static Criteria fromApiObject(ApiCriteriaV2 apiObj) {
    return Criteria.builder()
        .id(apiObj.getId())
        .displayName(apiObj.getDisplayName())
        .pluginName(apiObj.getPluginName())
        .uiConfig(apiObj.getUiConfig())
        .selectionData(apiObj.getSelectionData())
        .tags(apiObj.getTags())
        .build();
  }

  public static List<Attribute> selectAttributesFromRequest(ApiQueryV2 body, Entity entity) {
    List<Attribute> selectAttributes = new ArrayList<>();
    if (body.getIncludeAttributes() != null) {
      selectAttributes =
          body.getIncludeAttributes().stream()
              .map(attrName -> UnderlayService.getAttribute(entity, attrName))
              .collect(Collectors.toList());
    }
    return selectAttributes;
  }

  public static List<HierarchyField> selectHierarchyFieldsFromRequest(
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

  public static List<RelationshipField> selectRelationshipFieldsFromRequest(
      ApiQueryV2 body, Entity entity) {
    List<RelationshipField> selectRelationshipFields = new ArrayList<>();
    if (body.getIncludeRelationshipFields() != null) {
      // for each related entity, return all the fields specified
      body.getIncludeRelationshipFields().stream()
          .forEach(
              includeRelationshipField -> {
                Entity relatedEntity =
                    entity.getUnderlay().getEntity(includeRelationshipField.getRelatedEntity());
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

  public static List<EntityQueryOrderBy> entityOrderBysFromRequest(ApiQueryV2 body, Entity entity) {
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
                          UnderlayService.getAttribute(entity, attrName), direction));
                } else {
                  Entity relatedEntity =
                      entity
                          .getUnderlay()
                          .getEntity(orderBy.getRelationshipField().getRelatedEntity());
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

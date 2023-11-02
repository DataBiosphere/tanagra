package bio.terra.tanagra.app.controller.objmapping;

import bio.terra.common.exception.NotFoundException;
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
import bio.terra.tanagra.underlay.*;
import java.util.*;
import java.util.stream.Collectors;

public final class FromApiUtils {
  private FromApiUtils() {}

  public static EntityFilter fromApiObject(ApiFilter apiFilter, Underlay underlay) {
    return fromApiObject(apiFilter, underlay.getPrimaryEntity(), underlay.getName());
  }

  public static EntityFilter fromApiObject(
      ApiFilter apiFilter, Entity entity, String underlayName) {
    validateApiFilter(apiFilter);
    switch (apiFilter.getFilterType()) {
      case ATTRIBUTE:
        ApiAttributeFilter apiAttributeFilter = apiFilter.getFilterUnion().getAttributeFilter();
        return new AttributeFilter(
            getAttribute(entity, apiAttributeFilter.getAttribute()),
            FromApiUtils.fromApiObject(apiAttributeFilter.getOperator()),
            FromApiUtils.fromApiObject(apiAttributeFilter.getValue()));
      case TEXT:
        ApiTextFilter apiTextFilter = apiFilter.getFilterUnion().getTextFilter();
        TextFilter.Builder textFilterBuilder =
            new TextFilter.Builder()
                .textSearch(entity.getTextSearch())
                .functionTemplate(FromApiUtils.fromApiObject(apiTextFilter.getMatchType()))
                .text(apiTextFilter.getText());
        if (apiTextFilter.getAttribute() != null) {
          textFilterBuilder.attribute(getAttribute(entity, apiTextFilter.getAttribute()));
        }
        return textFilterBuilder.build();
      case HIERARCHY:
        ApiHierarchyFilter apiHierarchyFilter = apiFilter.getFilterUnion().getHierarchyFilter();
        Hierarchy hierarchy = getHierarchy(entity, apiHierarchyFilter.getHierarchy());
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
        ApiRelationshipFilter apiRelationshipFilter =
            apiFilter.getFilterUnion().getRelationshipFilter();
        Collection<EntityGroup> entityGroups = entity.getUnderlay().getEntityGroups().values();
        Entity relatedEntity = entity.getUnderlay().getEntity(apiRelationshipFilter.getEntity());
        Relationship relationship = getRelationship(entityGroups, entity, relatedEntity);
        EntityFilter subFilter =
            apiRelationshipFilter.getSubfilter() == null
                ? null
                : fromApiObject(apiRelationshipFilter.getSubfilter(), relatedEntity, underlayName);

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
        ApiBooleanLogicFilter apiBooleanLogicFilter =
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

  public static void validateApiFilter(ApiFilter filter) {
    if (filter != null && filter.getFilterType() == ApiFilter.FilterTypeEnum.RELATIONSHIP) {
      ApiRelationshipFilter relationshipFilter = filter.getFilterUnion().getRelationshipFilter();
      if (!((relationshipFilter.getGroupByCountOperator() == null
              && relationshipFilter.getGroupByCountValue() == null)
          || (relationshipFilter.getGroupByCountOperator() != null
              && relationshipFilter.getGroupByCountValue() != null))) {
        throw new InvalidConfigException(
            "If one RelationshipFilter group_by field is set, all group_by fields must be set");
      }
    }
  }

  public static EntityQueryRequest fromApiObject(ApiQuery apiObj, Entity entity) {
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

  public static Literal fromApiObject(ApiLiteral apiLiteral) {
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

  public static BinaryFilterVariable.BinaryOperator fromApiObject(ApiBinaryOperator apiOperator) {
    return BinaryFilterVariable.BinaryOperator.valueOf(apiOperator.name());
  }

  public static FunctionFilterVariable.FunctionTemplate fromApiObject(
      ApiTextFilter.MatchTypeEnum apiMatchType) {
    switch (apiMatchType) {
      case EXACT_MATCH:
        return FunctionFilterVariable.FunctionTemplate.TEXT_EXACT_MATCH;
      case FUZZY_MATCH:
        return FunctionFilterVariable.FunctionTemplate.TEXT_FUZZY_MATCH;
      default:
        throw new SystemException("Unknown API text match type: " + apiMatchType.name());
    }
  }

  public static Criteria fromApiObject(ApiCriteria apiObj) {
    return Criteria.builder()
        .id(apiObj.getId())
        .displayName(apiObj.getDisplayName())
        .pluginName(apiObj.getPluginName())
        .pluginVersion(apiObj.getPluginVersion() == null ? 0 : apiObj.getPluginVersion())
        .uiConfig(apiObj.getUiConfig())
        .selectionData(apiObj.getSelectionData())
        .tags(apiObj.getTags())
        .build();
  }

  public static List<Attribute> selectAttributesFromRequest(ApiQuery body, Entity entity) {
    List<Attribute> selectAttributes = new ArrayList<>();
    if (body.getIncludeAttributes() != null) {
      selectAttributes =
          body.getIncludeAttributes().stream()
              .map(attrName -> getAttribute(entity, attrName))
              .collect(Collectors.toList());
    }
    return selectAttributes;
  }

  public static List<HierarchyField> selectHierarchyFieldsFromRequest(
      ApiQuery body, Entity entity) {
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
      ApiQuery body, Entity entity) {
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

  public static List<EntityQueryOrderBy> entityOrderBysFromRequest(ApiQuery body, Entity entity) {
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
                      new EntityQueryOrderBy(getAttribute(entity, attrName), direction));
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

  public static Attribute getAttribute(Entity entity, String attributeName) {
    Attribute attribute = entity.getAttribute(attributeName);
    if (attribute == null) {
      throw new NotFoundException(
          "Attribute not found: " + entity.getName() + ", " + attributeName);
    }
    return attribute;
  }

  private static Hierarchy getHierarchy(Entity entity, String hierarchyName) {
    Hierarchy hierarchy = entity.getHierarchy(hierarchyName);
    if (hierarchy == null) {
      throw new NotFoundException("Hierarchy not found: " + hierarchyName);
    }
    return hierarchy;
  }

  public static Relationship getRelationship(
      Collection<EntityGroup> entityGroups, Entity entity, Entity relatedEntity) {
    for (EntityGroup entityGroup : entityGroups) {
      Optional<Relationship> relationship = entityGroup.getRelationship(entity, relatedEntity);
      if (relationship.isPresent()) {
        return relationship.get();
      }
    }
    throw new NotFoundException(
        "Relationship not found for entities: "
            + entity.getName()
            + " -- "
            + relatedEntity.getName());
  }
}

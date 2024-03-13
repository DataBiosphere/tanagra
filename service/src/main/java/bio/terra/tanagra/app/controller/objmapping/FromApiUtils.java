package bio.terra.tanagra.app.controller.objmapping;

import bio.terra.tanagra.api.field.AttributeField;
import bio.terra.tanagra.api.field.HierarchyIsMemberField;
import bio.terra.tanagra.api.field.HierarchyIsRootField;
import bio.terra.tanagra.api.field.HierarchyNumChildrenField;
import bio.terra.tanagra.api.field.HierarchyPathField;
import bio.terra.tanagra.api.field.RelatedEntityIdCountField;
import bio.terra.tanagra.api.field.ValueDisplayField;
import bio.terra.tanagra.api.filter.AttributeFilter;
import bio.terra.tanagra.api.filter.BooleanAndOrFilter;
import bio.terra.tanagra.api.filter.BooleanNotFilter;
import bio.terra.tanagra.api.filter.EntityFilter;
import bio.terra.tanagra.api.filter.GroupHasItemsFilter;
import bio.terra.tanagra.api.filter.HierarchyHasAncestorFilter;
import bio.terra.tanagra.api.filter.HierarchyHasParentFilter;
import bio.terra.tanagra.api.filter.HierarchyIsMemberFilter;
import bio.terra.tanagra.api.filter.HierarchyIsRootFilter;
import bio.terra.tanagra.api.filter.ItemInGroupFilter;
import bio.terra.tanagra.api.filter.OccurrenceForPrimaryFilter;
import bio.terra.tanagra.api.filter.PrimaryWithCriteriaFilter;
import bio.terra.tanagra.api.filter.RelationshipFilter;
import bio.terra.tanagra.api.filter.TextSearchFilter;
import bio.terra.tanagra.api.query.PageMarker;
import bio.terra.tanagra.api.query.list.ListQueryRequest;
import bio.terra.tanagra.api.shared.BinaryOperator;
import bio.terra.tanagra.api.shared.Literal;
import bio.terra.tanagra.api.shared.NaryOperator;
import bio.terra.tanagra.api.shared.OrderByDirection;
import bio.terra.tanagra.api.shared.UnaryOperator;
import bio.terra.tanagra.exception.InvalidConfigException;
import bio.terra.tanagra.exception.InvalidQueryException;
import bio.terra.tanagra.exception.SystemException;
import bio.terra.tanagra.generated.model.ApiAttributeFilter;
import bio.terra.tanagra.generated.model.ApiBinaryOperator;
import bio.terra.tanagra.generated.model.ApiBooleanLogicFilter;
import bio.terra.tanagra.generated.model.ApiCriteria;
import bio.terra.tanagra.generated.model.ApiFilter;
import bio.terra.tanagra.generated.model.ApiGroupHasItemsFilter;
import bio.terra.tanagra.generated.model.ApiHierarchyFilter;
import bio.terra.tanagra.generated.model.ApiItemInGroupFilter;
import bio.terra.tanagra.generated.model.ApiLiteral;
import bio.terra.tanagra.generated.model.ApiOccurrenceForPrimaryFilter;
import bio.terra.tanagra.generated.model.ApiPrimaryWithCriteriaFilter;
import bio.terra.tanagra.generated.model.ApiQuery;
import bio.terra.tanagra.generated.model.ApiQueryIncludeHierarchyFields;
import bio.terra.tanagra.generated.model.ApiQueryIncludeRelationshipFields;
import bio.terra.tanagra.generated.model.ApiRelationshipFilter;
import bio.terra.tanagra.generated.model.ApiTextFilter;
import bio.terra.tanagra.service.artifact.model.Criteria;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.entitymodel.Attribute;
import bio.terra.tanagra.underlay.entitymodel.Entity;
import bio.terra.tanagra.underlay.entitymodel.Hierarchy;
import bio.terra.tanagra.underlay.entitymodel.Relationship;
import bio.terra.tanagra.underlay.entitymodel.entitygroup.CriteriaOccurrence;
import bio.terra.tanagra.underlay.entitymodel.entitygroup.EntityGroup;
import bio.terra.tanagra.underlay.entitymodel.entitygroup.GroupItems;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;

public final class FromApiUtils {
  private FromApiUtils() {}

  public static EntityFilter fromApiObject(ApiFilter apiFilter, Underlay underlay) {
    return fromApiObject(apiFilter, underlay.getPrimaryEntity(), underlay);
  }

  public static EntityFilter fromApiObject(ApiFilter apiFilter, Entity entity, Underlay underlay) {
    validateApiFilter(apiFilter);
    switch (apiFilter.getFilterType()) {
      case ATTRIBUTE:
        ApiAttributeFilter apiAttributeFilter = apiFilter.getFilterUnion().getAttributeFilter();

        Optional<UnaryOperator> unaryOperator =
            getEnumValueFromName(UnaryOperator.values(), apiAttributeFilter.getOperator().name());
        if (unaryOperator.isPresent()) {
          return new AttributeFilter(
              underlay,
              entity,
              entity.getAttribute(apiAttributeFilter.getAttribute()),
              unaryOperator.get());
        }

        Optional<BinaryOperator> binaryOperator =
            getEnumValueFromName(BinaryOperator.values(), apiAttributeFilter.getOperator().name());
        if (binaryOperator.isPresent()) {
          return new AttributeFilter(
              underlay,
              entity,
              entity.getAttribute(apiAttributeFilter.getAttribute()),
              binaryOperator.get(),
              fromApiObject(apiAttributeFilter.getValues().get(0)));
        }

        Optional<NaryOperator> naryOperator =
            getEnumValueFromName(NaryOperator.values(), apiAttributeFilter.getOperator().name());
        if (naryOperator.isPresent()) {
          return new AttributeFilter(
              underlay,
              entity,
              entity.getAttribute(apiAttributeFilter.getAttribute()),
              naryOperator.get(),
              apiAttributeFilter.getValues().stream()
                  .map(FromApiUtils::fromApiObject)
                  .collect(Collectors.toList()));
        }

        throw new InvalidQueryException(
            "Invalid operator specified for an AttributeFilter: "
                + apiAttributeFilter.getOperator());
      case TEXT:
        ApiTextFilter apiTextFilter = apiFilter.getFilterUnion().getTextFilter();
        return new TextSearchFilter(
            underlay,
            entity,
            FromApiUtils.fromApiObject(apiTextFilter.getMatchType()),
            apiTextFilter.getText(),
            apiTextFilter.getAttribute() == null
                ? null
                : entity.getAttribute(apiTextFilter.getAttribute()));
      case HIERARCHY:
        ApiHierarchyFilter apiHierarchyFilter = apiFilter.getFilterUnion().getHierarchyFilter();
        Hierarchy hierarchy = entity.getHierarchy(apiHierarchyFilter.getHierarchy());
        switch (apiHierarchyFilter.getOperator()) {
          case IS_ROOT:
            return new HierarchyIsRootFilter(underlay, entity, hierarchy);
          case IS_MEMBER:
            return new HierarchyIsMemberFilter(underlay, entity, hierarchy);
          case CHILD_OF:
            return new HierarchyHasParentFilter(
                underlay,
                entity,
                hierarchy,
                apiHierarchyFilter.getValues().stream()
                    .map(FromApiUtils::fromApiObject)
                    .collect(Collectors.toList()));
          case DESCENDANT_OF_INCLUSIVE:
            return new HierarchyHasAncestorFilter(
                underlay,
                entity,
                hierarchy,
                apiHierarchyFilter.getValues().stream()
                    .map(FromApiUtils::fromApiObject)
                    .collect(Collectors.toList()));
          default:
            throw new SystemException(
                "Unknown API hierarchy filter operator: " + apiHierarchyFilter.getOperator());
        }
      case RELATIONSHIP:
        ApiRelationshipFilter apiRelationshipFilter =
            apiFilter.getFilterUnion().getRelationshipFilter();
        Entity relatedEntity = underlay.getEntity(apiRelationshipFilter.getEntity());
        Pair<EntityGroup, Relationship> entityGroupAndRelationship =
            underlay.getRelationship(entity, relatedEntity);
        EntityFilter subFilter =
            apiRelationshipFilter.getSubfilter() == null
                ? null
                : fromApiObject(apiRelationshipFilter.getSubfilter(), relatedEntity, underlay);

        List<Attribute> groupByCountAttributes = new ArrayList<>();
        BinaryOperator groupByCountOperator = null;
        Integer groupByCountValue = null;
        if (apiRelationshipFilter.getGroupByCountOperator() != null
            && apiRelationshipFilter.getGroupByCountValue() != null) {
          groupByCountOperator = fromApiObject(apiRelationshipFilter.getGroupByCountOperator());
          groupByCountValue = apiRelationshipFilter.getGroupByCountValue();
          if (apiRelationshipFilter.getGroupByCountAttributes() != null) {
            apiRelationshipFilter.getGroupByCountAttributes().stream()
                .forEach(
                    groupByCountAttrName ->
                        groupByCountAttributes.add(
                            relatedEntity.getAttribute(groupByCountAttrName)));
          }
        }
        return new RelationshipFilter(
            underlay,
            entityGroupAndRelationship.getLeft(),
            entity,
            entityGroupAndRelationship.getRight(),
            subFilter,
            groupByCountAttributes,
            groupByCountOperator,
            groupByCountValue);
      case BOOLEAN_LOGIC:
        ApiBooleanLogicFilter apiBooleanLogicFilter =
            apiFilter.getFilterUnion().getBooleanLogicFilter();
        List<EntityFilter> subFilters =
            apiBooleanLogicFilter.getSubfilters().stream()
                .map(apiSubFilter -> fromApiObject(apiSubFilter, entity, underlay))
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
                BooleanAndOrFilter.LogicalOperator.valueOf(
                    apiBooleanLogicFilter.getOperator().name()),
                subFilters);
          default:
            throw new SystemException(
                "Unknown boolean logic operator: " + apiBooleanLogicFilter.getOperator());
        }
      case ITEM_IN_GROUP:
        ApiItemInGroupFilter apiItemInGroupFilter =
            apiFilter.getFilterUnion().getItemInGroupFilter();
        GroupItems groupItemsItemInGroup =
            (GroupItems) underlay.getEntityGroup(apiItemInGroupFilter.getEntityGroup());
        EntityFilter groupSubFilter =
            apiItemInGroupFilter.getGroupSubfilter() == null
                ? null
                : fromApiObject(apiItemInGroupFilter.getGroupSubfilter(), underlay);
        List<Attribute> groupByAttrsItemInGroup = new ArrayList<>();
        if (apiItemInGroupFilter.getGroupByCountAttributes() != null) {
          apiItemInGroupFilter.getGroupByCountAttributes().stream()
              .forEach(
                  groupByCountAttrName ->
                      groupByAttrsItemInGroup.add(
                          groupItemsItemInGroup
                              .getGroupEntity()
                              .getAttribute(groupByCountAttrName)));
        }
        return new ItemInGroupFilter(
            underlay,
            groupItemsItemInGroup,
            groupSubFilter,
            groupByAttrsItemInGroup,
            fromApiObject(apiItemInGroupFilter.getGroupByCountOperator()),
            apiItemInGroupFilter.getGroupByCountValue());
      case GROUP_HAS_ITEMS:
        ApiGroupHasItemsFilter apiGroupHasItemsFilter =
            apiFilter.getFilterUnion().getGroupHasItemsFilter();
        GroupItems groupItemsGroupHasItems =
            (GroupItems) underlay.getEntityGroup(apiGroupHasItemsFilter.getEntityGroup());
        EntityFilter itemsSubFilter =
            apiGroupHasItemsFilter.getItemsSubfilter() == null
                ? null
                : fromApiObject(apiGroupHasItemsFilter.getItemsSubfilter(), underlay);
        List<Attribute> groupByAttrsGroupHasItems = new ArrayList<>();
        if (apiGroupHasItemsFilter.getGroupByCountAttributes() != null) {
          apiGroupHasItemsFilter.getGroupByCountAttributes().stream()
              .forEach(
                  groupByCountAttrName ->
                      groupByAttrsGroupHasItems.add(
                          groupItemsGroupHasItems
                              .getItemsEntity()
                              .getAttribute(groupByCountAttrName)));
        }
        return new GroupHasItemsFilter(
            underlay,
            groupItemsGroupHasItems,
            itemsSubFilter,
            groupByAttrsGroupHasItems,
            fromApiObject(apiGroupHasItemsFilter.getGroupByCountOperator()),
            apiGroupHasItemsFilter.getGroupByCountValue());
      case OCCURRENCE_FOR_PRIMARY:
        ApiOccurrenceForPrimaryFilter apiOccurrenceForPrimaryFilter =
            apiFilter.getFilterUnion().getOccurrenceForPrimaryFilter();
        CriteriaOccurrence criteriaOccurrenceOccForPri =
            (CriteriaOccurrence)
                underlay.getEntityGroup(apiOccurrenceForPrimaryFilter.getEntityGroup());
        Entity occurrenceEntityOccForPri =
            underlay.getEntity(apiOccurrenceForPrimaryFilter.getOccurrenceEntity());
        EntityFilter ofpPrimarySubFilter =
            apiOccurrenceForPrimaryFilter.getPrimarySubfilter() == null
                ? null
                : fromApiObject(apiOccurrenceForPrimaryFilter.getPrimarySubfilter(), underlay);
        EntityFilter ofpCriteriaSubFilter =
            apiOccurrenceForPrimaryFilter.getCriteriaSubfilter() == null
                ? null
                : fromApiObject(apiOccurrenceForPrimaryFilter.getCriteriaSubfilter(), underlay);
        return new OccurrenceForPrimaryFilter(
            underlay,
            criteriaOccurrenceOccForPri,
            occurrenceEntityOccForPri,
            ofpPrimarySubFilter,
            ofpCriteriaSubFilter);
      case PRIMARY_WITH_CRITERIA:
        ApiPrimaryWithCriteriaFilter apiPrimaryWithCriteriaFilter =
            apiFilter.getFilterUnion().getPrimaryWithCriteriaFilter();
        CriteriaOccurrence criteriaOccurrencePriWithCri =
            (CriteriaOccurrence)
                underlay.getEntityGroup(apiPrimaryWithCriteriaFilter.getEntityGroup());
        EntityFilter criteriaSubFilter =
            apiPrimaryWithCriteriaFilter.getCriteriaSubfilter() == null
                ? null
                : fromApiObject(apiPrimaryWithCriteriaFilter.getCriteriaSubfilter(), underlay);
        Map<Entity, List<EntityFilter>> subFiltersPerOccurrenceEntity = new HashMap<>();
        Map<Entity, List<Attribute>> groupByAttributesPerOccurrenceEntity = new HashMap<>();
        if (apiPrimaryWithCriteriaFilter.getOccurrenceSubfiltersAndGroupByAttributes() != null) {
          apiPrimaryWithCriteriaFilter.getOccurrenceSubfiltersAndGroupByAttributes().entrySet()
              .stream()
              .forEach(
                  entry -> {
                    Entity occurrenceEntity = underlay.getEntity(entry.getKey());
                    List<EntityFilter> subFiltersForOcc =
                        entry.getValue().getSubfilters().stream()
                            .map(apiFilterForOcc -> fromApiObject(apiFilterForOcc, underlay))
                            .collect(Collectors.toList());
                    subFiltersPerOccurrenceEntity.put(occurrenceEntity, subFiltersForOcc);
                    List<Attribute> groupByAttributesForOcc =
                        entry.getValue().getGroupByCountAttributes().stream()
                            .map(occurrenceEntity::getAttribute)
                            .collect(Collectors.toList());
                    groupByAttributesPerOccurrenceEntity.put(
                        occurrenceEntity, groupByAttributesForOcc);
                  });
        }
        return new PrimaryWithCriteriaFilter(
            underlay,
            criteriaOccurrencePriWithCri,
            criteriaSubFilter,
            subFiltersPerOccurrenceEntity,
            groupByAttributesPerOccurrenceEntity,
            fromApiObject(apiPrimaryWithCriteriaFilter.getGroupByCountOperator()),
            apiPrimaryWithCriteriaFilter.getGroupByCountValue());
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

  public static ListQueryRequest fromApiObject(ApiQuery apiObj, Entity entity, Underlay underlay) {
    // Build the select fields for attributes, hierarchies, and relationships.
    List<ValueDisplayField> selectFields = new ArrayList<>();
    if (apiObj.getIncludeAttributes() != null) {
      apiObj.getIncludeAttributes().stream()
          .forEach(
              attributeName ->
                  selectFields.add(buildAttributeField(underlay, entity, attributeName, false)));
    }
    if (apiObj.getIncludeHierarchyFields() != null) {
      apiObj.getIncludeHierarchyFields().getHierarchies().stream()
          .forEach(
              hierarchyName ->
                  selectFields.addAll(
                      buildHierarchyFields(
                          underlay,
                          entity,
                          hierarchyName,
                          apiObj.getIncludeHierarchyFields().getFields())));
    }
    if (apiObj.getIncludeRelationshipFields() != null) {
      apiObj.getIncludeRelationshipFields().stream()
          .forEach(
              relationshipField ->
                  selectFields.addAll(
                      buildRelationshipFields(underlay, entity, relationshipField)));
    }

    // Build the entity filter.
    EntityFilter filter =
        apiObj.getFilter() == null ? null : fromApiObject(apiObj.getFilter(), entity, underlay);

    // Build the order by fields.
    List<ListQueryRequest.OrderBy> orderByFields = new ArrayList<>();
    if (apiObj.getOrderBys() != null) {
      apiObj.getOrderBys().stream()
          .forEach(
              orderByField -> {
                ValueDisplayField valueDisplayField =
                    orderByField.getRelationshipField() == null
                        ? buildAttributeField(underlay, entity, orderByField.getAttribute(), true)
                        : buildRelationshipField(
                            underlay,
                            entity,
                            underlay.getEntity(
                                orderByField.getRelationshipField().getRelatedEntity()),
                            orderByField.getRelationshipField().getHierarchy() == null
                                ? null
                                : entity.getHierarchy(
                                    orderByField.getRelationshipField().getHierarchy()));
                OrderByDirection direction =
                    OrderByDirection.valueOf(orderByField.getDirection().name());
                orderByFields.add(new ListQueryRequest.OrderBy(valueDisplayField, direction));
              });
    }

    return new ListQueryRequest(
        underlay,
        entity,
        selectFields,
        filter,
        orderByFields,
        apiObj.getLimit(),
        PageMarker.deserialize(apiObj.getPageMarker()),
        apiObj.getPageSize(),
        false);
  }

  public static AttributeField buildAttributeField(
      Underlay underlay, Entity entity, String attributeName, boolean excludeDisplay) {
    return new AttributeField(
        underlay, entity, entity.getAttribute(attributeName), excludeDisplay, false);
  }

  private static Set<ValueDisplayField> buildHierarchyFields(
      Underlay underlay,
      Entity entity,
      String hierarchyName,
      List<ApiQueryIncludeHierarchyFields.FieldsEnum> fieldTypes) {
    Hierarchy hierarchy = entity.getHierarchy(hierarchyName);
    Set<ValueDisplayField> hierarchyFields = new HashSet<>();
    if (fieldTypes.contains(ApiQueryIncludeHierarchyFields.FieldsEnum.PATH)) {
      hierarchyFields.add(new HierarchyPathField(underlay, entity, hierarchy));
    }
    if (fieldTypes.contains(ApiQueryIncludeHierarchyFields.FieldsEnum.NUM_CHILDREN)) {
      hierarchyFields.add(new HierarchyNumChildrenField(underlay, entity, hierarchy));
    }
    if (fieldTypes.contains(ApiQueryIncludeHierarchyFields.FieldsEnum.IS_ROOT)) {
      hierarchyFields.add(new HierarchyIsRootField(underlay, entity, hierarchy));
    }
    if (fieldTypes.contains(ApiQueryIncludeHierarchyFields.FieldsEnum.IS_MEMBER)) {
      hierarchyFields.add(new HierarchyIsMemberField(underlay, entity, hierarchy));
    }
    return hierarchyFields;
  }

  private static Set<ValueDisplayField> buildRelationshipFields(
      Underlay underlay, Entity entity, ApiQueryIncludeRelationshipFields apiObj) {
    Entity relatedEntity = underlay.getEntity(apiObj.getRelatedEntity());

    List<Hierarchy> hierarchies = new ArrayList<>();
    // Always return the NO_HIERARCHY rollups.
    // TODO: Use a constant here instead of special-casing null.
    hierarchies.add(null);
    if (apiObj.getHierarchies() != null) {
      apiObj.getHierarchies().stream()
          .forEach(hierarchyName -> hierarchies.add(entity.getHierarchy(hierarchyName)));
    }

    Set<ValueDisplayField> relationshipFields = new HashSet<>();
    hierarchies.stream()
        .forEach(
            hierarchy ->
                relationshipFields.add(
                    buildRelationshipField(underlay, entity, relatedEntity, hierarchy)));
    return relationshipFields;
  }

  private static ValueDisplayField buildRelationshipField(
      Underlay underlay, Entity entity, Entity relatedEntity, Hierarchy hierarchy) {
    return new RelatedEntityIdCountField(
        underlay,
        entity,
        relatedEntity,
        underlay.getRelationship(entity, relatedEntity).getLeft(),
        hierarchy);
  }

  public static Literal fromApiObject(ApiLiteral apiLiteral) {
    switch (apiLiteral.getDataType()) {
      case INT64:
        return Literal.forInt64(apiLiteral.getValueUnion().getInt64Val());
      case STRING:
        return Literal.forString(apiLiteral.getValueUnion().getStringVal());
      case BOOLEAN:
        return Literal.forBoolean(apiLiteral.getValueUnion().isBoolVal());
      case DATE:
        return Literal.forDate(apiLiteral.getValueUnion().getDateVal());
      default:
        throw new SystemException("Unknown API data type: " + apiLiteral.getDataType());
    }
  }

  public static BinaryOperator fromApiObject(ApiBinaryOperator apiOperator) {
    return apiOperator == null ? null : BinaryOperator.valueOf(apiOperator.name());
  }

  public static TextSearchFilter.TextSearchOperator fromApiObject(
      ApiTextFilter.MatchTypeEnum apiMatchType) {
    return TextSearchFilter.TextSearchOperator.valueOf(apiMatchType.name());
  }

  private static <ET extends Enum> Optional<ET> getEnumValueFromName(ET[] values, String name) {
    for (ET enumVal : values) {
      if (enumVal.name().equals(name)) {
        return Optional.of(enumVal);
      }
    }
    return Optional.empty();
  }

  public static Criteria fromApiObject(ApiCriteria apiObj) {
    return Criteria.builder()
        .id(apiObj.getId())
        .displayName(apiObj.getDisplayName())
        .pluginName(apiObj.getPluginName())
        .pluginVersion(apiObj.getPluginVersion() == null ? 0 : apiObj.getPluginVersion())
        .predefinedId(apiObj.getPredefinedId())
        .selectorOrModifierName(apiObj.getSelectorOrModifierName())
        .uiConfig(apiObj.getUiConfig())
        .selectionData(apiObj.getSelectionData())
        .tags(apiObj.getTags())
        .build();
  }
}

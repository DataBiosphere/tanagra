package bio.terra.tanagra.service;

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
import bio.terra.tanagra.service.artifact.AnnotationKey;
import bio.terra.tanagra.service.artifact.Cohort;
import bio.terra.tanagra.service.artifact.Criteria;
import bio.terra.tanagra.service.instances.EntityQueryOrderBy;
import bio.terra.tanagra.service.instances.EntityQueryRequest;
import bio.terra.tanagra.service.instances.ReviewQueryOrderBy;
import bio.terra.tanagra.service.instances.ReviewQueryRequest;
import bio.terra.tanagra.service.instances.filter.*;
import bio.terra.tanagra.underlay.*;
import com.google.common.base.Strings;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public final class FromApiConversionService {
  private final UnderlaysService underlaysService;
  private final CohortService cohortService;

  private final AnnotationService annotationService;

  @Autowired
  public FromApiConversionService(
      UnderlaysService underlaysService,
      CohortService cohortService,
      AnnotationService annotationService) {
    this.underlaysService = underlaysService;
    this.cohortService = cohortService;
    this.annotationService = annotationService;
  }

  public EntityFilter fromApiObject(ApiFilterV2 apiFilter, String studyId, String cohortId) {
    Cohort cohort = cohortService.getCohort(studyId, cohortId);
    return fromApiObject(apiFilter, cohort.getUnderlay());
  }

  public EntityFilter fromApiObject(ApiFilterV2 apiFilter, String underlayName) {
    Underlay underlay = underlaysService.getUnderlay(underlayName);
    return fromApiObject(apiFilter, underlay.getPrimaryEntity(), underlay.getName());
  }

  public EntityFilter fromApiObject(ApiFilterV2 apiFilter, Entity entity, String underlayName) {
    validateApiFilter(apiFilter);
    switch (apiFilter.getFilterType()) {
      case ATTRIBUTE:
        ApiAttributeFilterV2 apiAttributeFilter = apiFilter.getFilterUnion().getAttributeFilter();
        return new AttributeFilter(
            underlaysService.getAttribute(entity, apiAttributeFilter.getAttribute()),
            FromApiConversionService.fromApiObject(apiAttributeFilter.getOperator()),
            FromApiConversionService.fromApiObject(apiAttributeFilter.getValue()));
      case TEXT:
        ApiTextFilterV2 apiTextFilter = apiFilter.getFilterUnion().getTextFilter();
        TextFilter.Builder textFilterBuilder =
            new TextFilter.Builder()
                .textSearch(entity.getTextSearch())
                .functionTemplate(
                    FromApiConversionService.fromApiObject(apiTextFilter.getMatchType()))
                .text(apiTextFilter.getText());
        if (apiTextFilter.getAttribute() != null) {
          textFilterBuilder.attribute(
              underlaysService.getAttribute(entity, apiTextFilter.getAttribute()));
        }
        return textFilterBuilder.build();
      case HIERARCHY:
        ApiHierarchyFilterV2 apiHierarchyFilter = apiFilter.getFilterUnion().getHierarchyFilter();
        Hierarchy hierarchy =
            underlaysService.getHierarchy(entity, apiHierarchyFilter.getHierarchy());
        switch (apiHierarchyFilter.getOperator()) {
          case IS_ROOT:
            return new HierarchyRootFilter(hierarchy);
          case IS_MEMBER:
            return new HierarchyMemberFilter(hierarchy);
          case CHILD_OF:
            return new HierarchyParentFilter(
                hierarchy, FromApiConversionService.fromApiObject(apiHierarchyFilter.getValue()));
          case DESCENDANT_OF_INCLUSIVE:
            return new HierarchyAncestorFilter(
                hierarchy, FromApiConversionService.fromApiObject(apiHierarchyFilter.getValue()));
          default:
            throw new SystemException(
                "Unknown API hierarchy filter operator: " + apiHierarchyFilter.getOperator());
        }
      case RELATIONSHIP:
        ApiRelationshipFilterV2 apiRelationshipFilter =
            apiFilter.getFilterUnion().getRelationshipFilter();
        Collection<EntityGroup> entityGroups =
            underlaysService.getUnderlay(underlayName).getEntityGroups().values();
        Entity relatedEntity =
            underlaysService.getEntity(underlayName, apiRelationshipFilter.getEntity());
        Relationship relationship =
            underlaysService.getRelationship(entityGroups, entity, relatedEntity);
        EntityFilter subFilter =
            fromApiObject(apiRelationshipFilter.getSubfilter(), relatedEntity, underlayName);

        Attribute groupByCountAttribute = null;
        BinaryFilterVariable.BinaryOperator groupByCountOperator = null;
        Integer groupByCountValue = null;
        if (!Strings.isNullOrEmpty(apiRelationshipFilter.getGroupByCountAttribute())) {
          groupByCountAttribute =
              relatedEntity.getAttribute(apiRelationshipFilter.getGroupByCountAttribute());
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

  private static void validateApiFilter(ApiFilterV2 filter) {
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

  public EntityQueryRequest fromApiObject(
      ApiQueryV2 apiObj, String underlayName, String entityName) {
    Entity entity = underlaysService.getEntity(underlayName, entityName);
    List<Attribute> selectAttributes = selectAttributesFromRequest(apiObj, entity);
    List<HierarchyField> selectHierarchyFields = selectHierarchyFieldsFromRequest(apiObj, entity);
    List<RelationshipField> selectRelationshipFields =
        selectRelationshipFieldsFromRequest(apiObj, entity, underlayName);
    List<EntityQueryOrderBy> entityOrderBys =
        entityOrderBysFromRequest(apiObj, entity, underlayName);
    EntityFilter entityFilter = null;
    if (apiObj.getFilter() != null) {
      entityFilter = fromApiObject(apiObj.getFilter(), entity, underlayName);
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

  public ReviewQueryRequest fromApiObject(
      ApiReviewQueryV2 apiObj, String studyId, String cohortId) {
    validateApiFilter(apiObj.getEntityFilter());

    Cohort cohort = cohortService.getCohort(studyId, cohortId);
    Entity entity = underlaysService.getUnderlay(cohort.getUnderlay()).getPrimaryEntity();
    List<Attribute> attributes = new ArrayList<>();
    if (apiObj.getIncludeAttributes() != null) {
      attributes =
          apiObj.getIncludeAttributes().stream()
              .map(attrName -> underlaysService.getAttribute(entity, attrName))
              .collect(Collectors.toList());
    }

    EntityFilter entityFilter =
        (apiObj.getEntityFilter() != null)
            ? fromApiObject(apiObj.getEntityFilter(), entity, cohort.getUnderlay())
            : null;
    AnnotationFilter annotationFilter;
    if (apiObj.getAnnotationFilter() != null) {
      AnnotationKey annotation =
          annotationService.getAnnotationKey(
              studyId, cohortId, apiObj.getAnnotationFilter().getAnnotation());
      BinaryFilterVariable.BinaryOperator operator =
          BinaryFilterVariable.BinaryOperator.valueOf(
              apiObj.getAnnotationFilter().getOperator().name());
      annotationFilter =
          new AnnotationFilter(
              annotation, operator, fromApiObject(apiObj.getAnnotationFilter().getValue()));
    } else {
      annotationFilter = null;
    }

    List<ReviewQueryOrderBy> orderBys = new ArrayList<>();
    if (apiObj.getOrderBys() != null) {
      apiObj.getOrderBys().stream()
          .forEach(
              orderBy -> {
                OrderByDirection direction =
                    orderBy.getDirection() == null
                        ? OrderByDirection.ASCENDING
                        : OrderByDirection.valueOf(orderBy.getDirection().name());
                String attrName = orderBy.getAttribute();
                if (attrName != null) {
                  orderBys.add(
                      new ReviewQueryOrderBy(
                          underlaysService.getAttribute(entity, attrName), direction));
                } else {
                  orderBys.add(
                      new ReviewQueryOrderBy(
                          annotationService.getAnnotationKey(
                              studyId, cohortId, orderBy.getAnnotation()),
                          direction));
                }
              });
    }
    return ReviewQueryRequest.builder()
        .attributes(attributes)
        .entityFilter(entityFilter)
        .annotationFilter(annotationFilter)
        .orderBys(orderBys)
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

  public List<Attribute> selectAttributesFromRequest(ApiQueryV2 body, Entity entity) {
    List<Attribute> selectAttributes = new ArrayList<>();
    if (body.getIncludeAttributes() != null) {
      selectAttributes =
          body.getIncludeAttributes().stream()
              .map(attrName -> underlaysService.getAttribute(entity, attrName))
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

  public List<RelationshipField> selectRelationshipFieldsFromRequest(
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

  public List<EntityQueryOrderBy> entityOrderBysFromRequest(
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

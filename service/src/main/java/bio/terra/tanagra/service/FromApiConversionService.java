package bio.terra.tanagra.service;

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
import bio.terra.tanagra.service.instances.ReviewQueryOrderBy;
import bio.terra.tanagra.service.instances.ReviewQueryRequest;
import bio.terra.tanagra.service.instances.filter.*;
import bio.terra.tanagra.service.utils.ValidationUtils;
import bio.terra.tanagra.underlay.*;
import com.google.common.base.Strings;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
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
    Underlay underlay = underlaysService.getUnderlay(cohort.getUnderlay());
    return fromApiObject(apiFilter, underlay.getPrimaryEntity(), underlay.getName());
  }

  public EntityFilter fromApiObject(ApiFilterV2 apiFilter, Entity entity, String underlayName) {
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

  public ReviewQueryRequest fromApiObject(
      ApiReviewQueryV2 apiObj, String studyId, String cohortId) {
    ValidationUtils.validateApiFilter(apiObj.getEntityFilter());

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
}

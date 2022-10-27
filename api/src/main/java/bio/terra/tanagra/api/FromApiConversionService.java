package bio.terra.tanagra.api;

import bio.terra.tanagra.api.entityfilter.AttributeFilter;
import bio.terra.tanagra.api.entityfilter.BooleanAndOrFilter;
import bio.terra.tanagra.api.entityfilter.BooleanNotFilter;
import bio.terra.tanagra.api.entityfilter.HierarchyAncestorFilter;
import bio.terra.tanagra.api.entityfilter.HierarchyParentFilter;
import bio.terra.tanagra.api.entityfilter.HierarchyRootFilter;
import bio.terra.tanagra.api.entityfilter.RelationshipFilter;
import bio.terra.tanagra.api.entityfilter.TextFilter;
import bio.terra.tanagra.exception.InvalidQueryException;
import bio.terra.tanagra.exception.SystemException;
import bio.terra.tanagra.generated.model.ApiAttributeFilterV2;
import bio.terra.tanagra.generated.model.ApiBooleanLogicFilterV2;
import bio.terra.tanagra.generated.model.ApiFilterV2;
import bio.terra.tanagra.generated.model.ApiHierarchyFilterV2;
import bio.terra.tanagra.generated.model.ApiLiteralV2;
import bio.terra.tanagra.generated.model.ApiRelationshipFilterV2;
import bio.terra.tanagra.generated.model.ApiTextFilterV2;
import bio.terra.tanagra.query.Literal;
import bio.terra.tanagra.query.filtervariable.BinaryFilterVariable;
import bio.terra.tanagra.query.filtervariable.BooleanAndOrFilterVariable;
import bio.terra.tanagra.query.filtervariable.FunctionFilterVariable;
import bio.terra.tanagra.underlay.Entity;
import bio.terra.tanagra.underlay.EntityGroup;
import bio.terra.tanagra.underlay.Hierarchy;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public final class FromApiConversionService {
  private final UnderlaysService underlaysService;
  private final QuerysService querysService;

  @Autowired
  public FromApiConversionService(UnderlaysService underlaysService, QuerysService querysService) {
    this.underlaysService = underlaysService;
    this.querysService = querysService;
  }

  public EntityFilter fromApiObject(ApiFilterV2 apiFilter, Entity entity, String underlayName) {
    switch (apiFilter.getFilterType()) {
      case ATTRIBUTE:
        ApiAttributeFilterV2 apiAttributeFilter = apiFilter.getFilterUnion().getAttributeFilter();
        return new AttributeFilter(
            querysService.getAttribute(entity, apiAttributeFilter.getAttribute()),
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
              querysService.getAttribute(entity, apiTextFilter.getAttribute()));
        }
        return textFilterBuilder.build();
      case HIERARCHY:
        ApiHierarchyFilterV2 apiHierarchyFilter = apiFilter.getFilterUnion().getHierarchyFilter();
        Hierarchy hierarchy = querysService.getHierarchy(entity, apiHierarchyFilter.getHierarchy());
        switch (apiHierarchyFilter.getOperator()) {
          case IS_ROOT:
            return new HierarchyRootFilter(hierarchy);
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
        Entity relatedEntity =
            underlaysService.getEntity(underlayName, apiRelationshipFilter.getEntity());
        // TODO: Allow building queries against the source data mapping also.
        EntityFilter subFilter =
            fromApiObject(apiRelationshipFilter.getSubfilter(), relatedEntity, underlayName);

        Collection<EntityGroup> entityGroups =
            underlaysService.getUnderlay(underlayName).getEntityGroups().values();
        return new RelationshipFilter(
            entity, querysService.getRelationship(entityGroups, entity, relatedEntity), subFilter);
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

  public static BinaryFilterVariable.BinaryOperator fromApiObject(
      ApiAttributeFilterV2.OperatorEnum apiOperator) {
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
}

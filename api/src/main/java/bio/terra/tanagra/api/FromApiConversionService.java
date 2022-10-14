package bio.terra.tanagra.api;

import bio.terra.tanagra.api.entityfilter.AttributeFilter;
import bio.terra.tanagra.api.entityfilter.HierarchyAncestorFilter;
import bio.terra.tanagra.api.entityfilter.HierarchyParentFilter;
import bio.terra.tanagra.api.entityfilter.HierarchyRootFilter;
import bio.terra.tanagra.api.entityfilter.RelationshipFilter;
import bio.terra.tanagra.api.entityfilter.TextFilter;
import bio.terra.tanagra.exception.SystemException;
import bio.terra.tanagra.generated.model.ApiAttributeFilterV2;
import bio.terra.tanagra.generated.model.ApiFilterV2;
import bio.terra.tanagra.generated.model.ApiHierarchyFilterV2;
import bio.terra.tanagra.generated.model.ApiLiteralV2;
import bio.terra.tanagra.generated.model.ApiQueryV2IncludeHierarchyFields;
import bio.terra.tanagra.generated.model.ApiRelationshipFilterV2;
import bio.terra.tanagra.generated.model.ApiTextFilterV2;
import bio.terra.tanagra.query.filtervariable.BinaryFilterVariable;
import bio.terra.tanagra.query.filtervariable.FunctionFilterVariable;
import bio.terra.tanagra.underlay.Entity;
import bio.terra.tanagra.underlay.EntityGroup;
import bio.terra.tanagra.underlay.EntityMapping;
import bio.terra.tanagra.underlay.HierarchyField;
import bio.terra.tanagra.underlay.HierarchyMapping;
import bio.terra.tanagra.underlay.Literal;
import bio.terra.tanagra.underlay.RelationshipMapping;
import bio.terra.tanagra.underlay.hierarchyfield.IsMember;
import bio.terra.tanagra.underlay.hierarchyfield.IsRoot;
import bio.terra.tanagra.underlay.hierarchyfield.NumChildren;
import bio.terra.tanagra.underlay.hierarchyfield.Path;
import java.util.Collection;
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

  public EntityFilter fromApiObject(
      ApiFilterV2 apiFilter, Entity entity, EntityMapping entityMapping, String underlayName) {
    switch (apiFilter.getFilterType()) {
      case ATTRIBUTE:
        ApiAttributeFilterV2 apiAttributeFilter = apiFilter.getFilterUnion().getAttributeFilter();
        return new AttributeFilter(
            entity,
            entityMapping,
            querysService.getAttribute(entity, apiAttributeFilter.getAttribute()),
            FromApiConversionService.fromApiObject(apiAttributeFilter.getOperator()),
            FromApiConversionService.fromApiObject(apiAttributeFilter.getValue()));
      case TEXT:
        ApiTextFilterV2 apiTextFilter = apiFilter.getFilterUnion().getTextFilter();
        TextFilter.Builder textFilterBuilder =
            new TextFilter.Builder()
                .entity(entity)
                .entityMapping(entityMapping)
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
        HierarchyMapping hierarchyMapping =
            querysService.getHierarchy(entityMapping, apiHierarchyFilter.getHierarchy());
        switch (apiHierarchyFilter.getOperator()) {
          case IS_ROOT:
            return new HierarchyRootFilter(
                entity, entityMapping, hierarchyMapping, apiHierarchyFilter.getHierarchy());
          case CHILD_OF:
            return new HierarchyParentFilter(
                entity,
                entityMapping,
                hierarchyMapping,
                FromApiConversionService.fromApiObject(apiHierarchyFilter.getValue()));
          case DESCENDANT_OF_INCLUSIVE:
            return new HierarchyAncestorFilter(
                entity,
                entityMapping,
                hierarchyMapping,
                FromApiConversionService.fromApiObject(apiHierarchyFilter.getValue()));
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
            fromApiObject(
                apiRelationshipFilter.getSubfilter(),
                relatedEntity,
                relatedEntity.getIndexDataMapping(),
                underlayName);

        Collection<EntityGroup> entityGroups =
            underlaysService.getUnderlay(underlayName).getEntityGroups().values();
        RelationshipMapping relationshipMapping =
            querysService.getRelationshipMapping(entityGroups, entity, relatedEntity);
        return new RelationshipFilter(
            entity,
            entityMapping,
            relatedEntity.getIndexDataMapping(),
            relationshipMapping,
            subFilter);
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

  public static HierarchyField fromApiObject(
      String hierarchyName, ApiQueryV2IncludeHierarchyFields.FieldsEnum apiHierarchyField) {
    switch (apiHierarchyField) {
      case IS_MEMBER:
        return new IsMember(hierarchyName);
      case IS_ROOT:
        return new IsRoot(hierarchyName);
      case PATH:
        return new Path(hierarchyName);
      case NUM_CHILDREN:
        return new NumChildren(hierarchyName);
      default:
        throw new SystemException("Unknown API hierarchy field: " + apiHierarchyField);
    }
  }
}

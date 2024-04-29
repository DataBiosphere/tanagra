package bio.terra.tanagra.app.controller.objmapping;

import bio.terra.tanagra.api.field.AttributeField;
import bio.terra.tanagra.api.field.HierarchyIsMemberField;
import bio.terra.tanagra.api.field.HierarchyIsRootField;
import bio.terra.tanagra.api.field.HierarchyNumChildrenField;
import bio.terra.tanagra.api.field.HierarchyPathField;
import bio.terra.tanagra.api.field.RelatedEntityIdCountField;
import bio.terra.tanagra.api.field.ValueDisplayField;
import bio.terra.tanagra.api.query.count.CountInstance;
import bio.terra.tanagra.api.query.count.CountQueryResult;
import bio.terra.tanagra.api.query.list.ListInstance;
import bio.terra.tanagra.api.query.list.ListQueryResult;
import bio.terra.tanagra.api.shared.Literal;
import bio.terra.tanagra.api.shared.ValueDisplay;
import bio.terra.tanagra.exception.SystemException;
import bio.terra.tanagra.generated.model.ApiAnnotationValue;
import bio.terra.tanagra.generated.model.ApiAttribute;
import bio.terra.tanagra.generated.model.ApiCohort;
import bio.terra.tanagra.generated.model.ApiCriteria;
import bio.terra.tanagra.generated.model.ApiCriteriaGroup;
import bio.terra.tanagra.generated.model.ApiCriteriaGroupSection;
import bio.terra.tanagra.generated.model.ApiDataType;
import bio.terra.tanagra.generated.model.ApiInstance;
import bio.terra.tanagra.generated.model.ApiInstanceCount;
import bio.terra.tanagra.generated.model.ApiInstanceCountList;
import bio.terra.tanagra.generated.model.ApiInstanceHierarchyFields;
import bio.terra.tanagra.generated.model.ApiInstanceListResult;
import bio.terra.tanagra.generated.model.ApiInstanceRelationshipFields;
import bio.terra.tanagra.generated.model.ApiLiteral;
import bio.terra.tanagra.generated.model.ApiLiteralValueUnion;
import bio.terra.tanagra.generated.model.ApiProperties;
import bio.terra.tanagra.generated.model.ApiPropertyKeyValue;
import bio.terra.tanagra.generated.model.ApiStudy;
import bio.terra.tanagra.generated.model.ApiUnderlaySummary;
import bio.terra.tanagra.generated.model.ApiValueDisplay;
import bio.terra.tanagra.service.artifact.model.AnnotationValue;
import bio.terra.tanagra.service.artifact.model.Cohort;
import bio.terra.tanagra.service.artifact.model.CohortRevision;
import bio.terra.tanagra.service.artifact.model.Criteria;
import bio.terra.tanagra.service.artifact.model.Study;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.entitymodel.Attribute;
import bio.terra.tanagra.utils.SqlFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class ToApiUtils {
  private ToApiUtils() {}

  public static ApiAttribute toApiObject(Attribute attribute) {
    return new ApiAttribute()
        .name(attribute.getName())
        .type(
            attribute.isSimple()
                ? ApiAttribute.TypeEnum.SIMPLE
                : ApiAttribute.TypeEnum.KEY_AND_DISPLAY)
        .dataType(ApiDataType.fromValue(attribute.getDataType().name()));
  }

  public static ApiValueDisplay toApiObject(ValueDisplay valueDisplay) {
    ApiValueDisplay apiObject = new ApiValueDisplay();
    if (valueDisplay != null) {
      apiObject.value(toApiObject(valueDisplay.getValue())).display(valueDisplay.getDisplay());
    }
    return apiObject;
  }

  public static ApiLiteral toApiObject(Literal literal) {
    // TODO: Return a null value of the appropriate type once literals store type for nulls.
    if (literal.isNull()) {
      return new ApiLiteral().dataType(ApiDataType.STRING).valueUnion(new ApiLiteralValueUnion());
    }

    ApiLiteral apiLiteral =
        new ApiLiteral().dataType(ApiDataType.fromValue(literal.getDataType().name()));
    switch (literal.getDataType()) {
      case INT64:
        return apiLiteral.valueUnion(new ApiLiteralValueUnion().int64Val(literal.getInt64Val()));
      case STRING:
        return apiLiteral.valueUnion(new ApiLiteralValueUnion().stringVal(literal.getStringVal()));
      case BOOLEAN:
        return apiLiteral.valueUnion(new ApiLiteralValueUnion().boolVal(literal.getBooleanVal()));
      case DATE:
        return apiLiteral.valueUnion(
            new ApiLiteralValueUnion()
                .dateVal(literal.getDateVal() == null ? null : literal.getDateVal().toString()));
      case TIMESTAMP:
        return apiLiteral.valueUnion(
            new ApiLiteralValueUnion()
                .timestampVal(
                    literal.getTimestampVal() == null
                        ? null
                        : literal.getTimestampVal().toString()));
      case DOUBLE:
        return apiLiteral.valueUnion(new ApiLiteralValueUnion().doubleVal(literal.getDoubleVal()));
      default:
        throw new SystemException("Unknown data type: " + literal.getDataType());
    }
  }

  public static ApiCohort toApiObject(Cohort cohort) {
    return new ApiCohort()
        .id(cohort.getId())
        .underlayName(cohort.getUnderlay())
        .displayName(cohort.getDisplayName())
        .description(cohort.getDescription())
        .created(cohort.getCreated())
        .createdBy(cohort.getCreatedBy())
        .lastModified(cohort.getLastModified())
        .criteriaGroupSections(
            cohort.getMostRecentRevision().getSections().stream()
                .map(criteriaGroup -> toApiObject(criteriaGroup))
                .collect(Collectors.toList()));
  }

  public static ApiCriteriaGroupSection toApiObject(
      CohortRevision.CriteriaGroupSection criteriaGroupSection) {
    return new ApiCriteriaGroupSection()
        .id(criteriaGroupSection.getId())
        .displayName(criteriaGroupSection.getDisplayName())
        .operator(
            ApiCriteriaGroupSection.OperatorEnum.fromValue(
                criteriaGroupSection.getOperator().name()))
        .excluded(criteriaGroupSection.isExcluded())
        .criteriaGroups(
            criteriaGroupSection.getCriteriaGroups().stream()
                .map(criteriaGroup -> toApiObject(criteriaGroup))
                .collect(Collectors.toList()));
  }

  private static ApiCriteriaGroup toApiObject(CohortRevision.CriteriaGroup criteriaGroup) {
    return new ApiCriteriaGroup()
        .id(criteriaGroup.getId())
        .displayName(criteriaGroup.getDisplayName())
        .criteria(
            criteriaGroup.getCriteria().stream()
                .map(criteria -> toApiObject(criteria))
                .collect(Collectors.toList()));
  }

  public static ApiCriteria toApiObject(Criteria criteria) {
    return new ApiCriteria()
        .id(criteria.getId())
        .displayName(criteria.getDisplayName())
        .pluginName(criteria.getPluginName())
        .pluginVersion(criteria.getPluginVersion())
        .predefinedId(criteria.getPredefinedId())
        .selectorOrModifierName(criteria.getSelectorOrModifierName())
        .selectionData(criteria.getSelectionData())
        .uiConfig(criteria.getUiConfig())
        .tags(criteria.getTags());
  }

  public static ApiInstanceListResult toApiObject(ListQueryResult listQueryResult) {
    return new ApiInstanceListResult()
        .instances(
            listQueryResult.getListInstances().stream()
                .map(listInstance -> toApiObject(listInstance))
                .collect(Collectors.toList()))
        .sql(SqlFormatter.format(listQueryResult.getSqlNoParams()))
        .pageMarker(
            listQueryResult.getPageMarker() == null
                ? null
                : listQueryResult.getPageMarker().serialize())
        .numRowsAcrossAllPages(
            listQueryResult.getNumRowsAcrossAllPages() == null
                ? null
                : Math.toIntExact(listQueryResult.getNumRowsAcrossAllPages()));
  }

  private static ApiInstance toApiObject(ListInstance listInstance) {
    Map<String, ApiValueDisplay> attributes = new HashMap<>();
    Map<String, ApiInstanceHierarchyFields> hierarchyFieldSets = new HashMap<>();
    List<ApiInstanceRelationshipFields> relationshipFieldSets = new ArrayList<>();
    listInstance.getEntityFieldValues().entrySet().stream()
        .forEach(
            fieldValuePair -> {
              ValueDisplayField field = fieldValuePair.getKey();
              ValueDisplay value = fieldValuePair.getValue();

              if (field instanceof AttributeField) {
                attributes.put(
                    ((AttributeField) field).getAttribute().getName(),
                    ToApiUtils.toApiObject(value));
              } else if (field instanceof HierarchyPathField) {
                getHierarchyFieldSet(
                        hierarchyFieldSets, ((HierarchyPathField) field).getHierarchy().getName())
                    .setPath(value.getValue().getStringVal());
              } else if (field instanceof HierarchyNumChildrenField) {
                getHierarchyFieldSet(
                        hierarchyFieldSets,
                        ((HierarchyNumChildrenField) field).getHierarchy().getName())
                    .setNumChildren(
                        value.getValue().getInt64Val() == null
                            ? null
                            : Math.toIntExact(value.getValue().getInt64Val()));
              } else if (field instanceof HierarchyIsRootField) {
                getHierarchyFieldSet(
                        hierarchyFieldSets, ((HierarchyIsRootField) field).getHierarchy().getName())
                    .setIsRoot(value.getValue().getBooleanVal());
              } else if (field instanceof HierarchyIsMemberField) {
                getHierarchyFieldSet(
                        hierarchyFieldSets,
                        ((HierarchyIsMemberField) field).getHierarchy().getName())
                    .setIsMember(value.getValue().getBooleanVal());
              } else if (field instanceof RelatedEntityIdCountField) {
                RelatedEntityIdCountField countField = (RelatedEntityIdCountField) field;
                relationshipFieldSets.add(
                    new ApiInstanceRelationshipFields()
                        .relatedEntity(countField.getCountedEntity().getName())
                        .hierarchy(
                            countField.getHierarchy() == null
                                ? null
                                : countField.getHierarchy().getName())
                        .count(
                            value.getValue().getInt64Val() == null
                                ? null
                                : Math.toIntExact(value.getValue().getInt64Val())));
              }
            });
    return new ApiInstance()
        .attributes(attributes)
        .hierarchyFields(hierarchyFieldSets.values().stream().collect(Collectors.toList()))
        .relationshipFields(relationshipFieldSets);
  }

  private static ApiInstanceHierarchyFields getHierarchyFieldSet(
      Map<String, ApiInstanceHierarchyFields> hierarchyFieldSets, String hierarchyName) {
    if (!hierarchyFieldSets.containsKey(hierarchyName)) {
      hierarchyFieldSets.put(hierarchyName, new ApiInstanceHierarchyFields());
    }
    return hierarchyFieldSets.get(hierarchyName);
  }

  public static ApiInstanceCountList toApiObject(CountQueryResult countQueryResult) {
    return new ApiInstanceCountList()
        .instanceCounts(
            countQueryResult.getCountInstances().stream()
                .map(ToApiUtils::toApiObject)
                .collect(Collectors.toList()))
        .sql(SqlFormatter.format(countQueryResult.getSql()))
        .pageMarker(
            countQueryResult.getPageMarker() == null
                ? null
                : countQueryResult.getPageMarker().serialize())
        .numRowsAcrossAllPages(
            countQueryResult.getNumRowsAcrossAllPages() == null
                ? null
                : Math.toIntExact(countQueryResult.getNumRowsAcrossAllPages()));
  }

  public static ApiInstanceCount toApiObject(CountInstance countInstance) {
    Map<String, ApiValueDisplay> attributes = new HashMap<>();
    countInstance.getEntityFieldValues().entrySet().stream()
        .forEach(
            fieldValuePair -> {
              ValueDisplayField field = fieldValuePair.getKey();
              ValueDisplay value = fieldValuePair.getValue();

              if (field instanceof AttributeField) {
                attributes.put(
                    ((AttributeField) field).getAttribute().getName(),
                    ToApiUtils.toApiObject(value));
              }
            });
    return new ApiInstanceCount()
        .attributes(attributes)
        .count(Math.toIntExact(countInstance.getCount()));
  }

  public static ApiAnnotationValue toApiObject(AnnotationValue annotationValue) {
    return new ApiAnnotationValue()
        .instanceId(annotationValue.getInstanceId())
        .value(toApiObject(annotationValue.getLiteral()))
        .isMostRecent(annotationValue.isMostRecent())
        .isPartOfSelectedReview(annotationValue.isPartOfSelectedReview());
  }

  public static ApiStudy toApiObject(Study study) {
    ApiProperties apiProperties = new ApiProperties();
    study
        .getProperties()
        .forEach(
            (key, value) -> apiProperties.add(new ApiPropertyKeyValue().key(key).value(value)));
    return new ApiStudy()
        .id(study.getId())
        .displayName(study.getDisplayName())
        .description(study.getDescription())
        .properties(apiProperties)
        .created(study.getCreated())
        .createdBy(study.getCreatedBy())
        .lastModified(study.getLastModified())
        .isDeleted(study.isDeleted());
  }

  public static ApiUnderlaySummary toApiObject(Underlay underlay) {
    return new ApiUnderlaySummary()
        .name(underlay.getName())
        .displayName(underlay.getDisplayName())
        .description(underlay.getDescription())
        .primaryEntity(underlay.getPrimaryEntity().getName());
  }
}

package bio.terra.tanagra.app.controller.objmapping;

import bio.terra.tanagra.api.query.EntityInstanceCount;
import bio.terra.tanagra.exception.SystemException;
import bio.terra.tanagra.generated.model.*;
import bio.terra.tanagra.query.Literal;
import bio.terra.tanagra.service.artifact.model.*;
import bio.terra.tanagra.underlay.Attribute;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.ValueDisplay;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public final class ToApiUtils {
  private ToApiUtils() {}

  public static ApiAttribute toApiObject(Attribute attribute) {
    return new ApiAttribute()
        .name(attribute.getName())
        .type(ApiAttribute.TypeEnum.fromValue(attribute.getType().name()))
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
            new ApiLiteralValueUnion().dateVal(literal.getDateValAsString()));
      case TIMESTAMP:
        return apiLiteral.valueUnion(
            new ApiLiteralValueUnion().timestampVal(literal.getTimestampValAsString()));
      case DOUBLE:
        return apiLiteral.valueUnion(new ApiLiteralValueUnion().doubleVal(literal.getDoubleVal()));
      default:
        throw new SystemException("Unknown literal data type: " + literal.getDataType());
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
        .entity(criteriaGroup.getEntity())
        .groupByCountOperator(
            criteriaGroup.getGroupByCountOperator() == null
                ? null
                : ApiBinaryOperator.valueOf(criteriaGroup.getGroupByCountOperator().name()))
        .groupByCountValue(criteriaGroup.getGroupByCountValue())
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
        .selectionData(criteria.getSelectionData())
        .uiConfig(criteria.getUiConfig())
        .tags(criteria.getTags());
  }

  public static ApiInstanceCount toApiObject(EntityInstanceCount entityInstanceCount) {
    ApiInstanceCount instanceCount = new ApiInstanceCount();
    Map<String, ApiValueDisplay> attributes = new HashMap<>();
    for (Map.Entry<Attribute, ValueDisplay> attributeValue :
        entityInstanceCount.getAttributeValues().entrySet()) {
      attributes.put(attributeValue.getKey().getName(), toApiObject(attributeValue.getValue()));
    }

    return instanceCount
        .count(Math.toIntExact(entityInstanceCount.getCount()))
        .attributes(attributes);
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

  public static ApiUnderlay toApiObject(Underlay underlay) {
    return new ApiUnderlay()
        .name(underlay.getName())
        // TODO: Add display name to underlay config files.
        .displayName(underlay.getName())
        .primaryEntity(underlay.getPrimaryEntity().getName())
        .uiConfiguration(underlay.getUIConfig());
  }
}

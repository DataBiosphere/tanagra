package bio.terra.tanagra.service.utils;

import bio.terra.tanagra.exception.SystemException;
import bio.terra.tanagra.generated.model.*;
import bio.terra.tanagra.query.Literal;
import bio.terra.tanagra.service.artifact.AnnotationValueV1;
import bio.terra.tanagra.service.artifact.CohortV1;
import bio.terra.tanagra.service.artifact.CriteriaGroupV1;
import bio.terra.tanagra.service.artifact.CriteriaV1;
import bio.terra.tanagra.service.instances.EntityInstanceCount;
import bio.terra.tanagra.service.model.Cohort;
import bio.terra.tanagra.service.model.CohortRevision;
import bio.terra.tanagra.service.model.Criteria;
import bio.terra.tanagra.underlay.Attribute;
import bio.terra.tanagra.underlay.ValueDisplay;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public final class ToApiConversionUtils {
  private ToApiConversionUtils() {}

  public static ApiAttributeV2 toApiObject(Attribute attribute) {
    return new ApiAttributeV2()
        .name(attribute.getName())
        .type(ApiAttributeV2.TypeEnum.fromValue(attribute.getType().name()))
        .dataType(ApiDataTypeV2.fromValue(attribute.getDataType().name()));
  }

  public static ApiValueDisplayV2 toApiObject(ValueDisplay valueDisplay) {
    ApiValueDisplayV2 apiObject = new ApiValueDisplayV2();
    if (valueDisplay != null) {
      apiObject.value(toApiObject(valueDisplay.getValue())).display(valueDisplay.getDisplay());
    }
    return apiObject;
  }

  public static ApiLiteralV2 toApiObject(Literal literal) {
    ApiLiteralV2 apiLiteral =
        new ApiLiteralV2().dataType(ApiDataTypeV2.fromValue(literal.getDataType().name()));
    switch (literal.getDataType()) {
      case INT64:
        return apiLiteral.valueUnion(new ApiLiteralV2ValueUnion().int64Val(literal.getInt64Val()));
      case STRING:
        return apiLiteral.valueUnion(
            new ApiLiteralV2ValueUnion().stringVal(literal.getStringVal()));
      case BOOLEAN:
        return apiLiteral.valueUnion(new ApiLiteralV2ValueUnion().boolVal(literal.getBooleanVal()));
      case DATE:
        return apiLiteral.valueUnion(
            new ApiLiteralV2ValueUnion().dateVal(literal.getDateValAsString()));
      default:
        throw new SystemException("Unknown literal data type: " + literal.getDataType());
    }
  }

  public static ApiCohortV2 toApiObject(CohortV1 cohort) {
    return new ApiCohortV2()
        .id(cohort.getCohortId())
        .underlayName(cohort.getUnderlayName())
        .displayName(cohort.getDisplayName())
        .description(cohort.getDescription())
        .created(cohort.getCreated())
        .createdBy(cohort.getCreatedBy())
        .lastModified(cohort.getLastModified());
  }

  private static ApiCriteriaGroupV2 toApiObject(CriteriaGroupV1 criteriaGroup) {
    return new ApiCriteriaGroupV2()
        .id(criteriaGroup.getUserFacingCriteriaGroupId())
        .displayName(criteriaGroup.getDisplayName())
        .operator(ApiCriteriaGroupV2.OperatorEnum.fromValue(criteriaGroup.getOperator().name()))
        .excluded(criteriaGroup.isExcluded())
        .criteria(
            criteriaGroup.getCriterias().stream()
                .map(criteria -> toApiObject(criteria))
                .collect(Collectors.toList()));
  }

  public static ApiCriteriaV2 toApiObject(CriteriaV1 criteria) {
    return new ApiCriteriaV2()
        .id(criteria.getUserFacingCriteriaId())
        .displayName(criteria.getDisplayName())
        .pluginName(criteria.getPluginName())
        .selectionData(criteria.getSelectionData())
        .uiConfig(criteria.getUiConfig());
  }

  public static ApiCohortV2 toApiObject(Cohort cohort) {
    return new ApiCohortV2()
        .id(cohort.getId())
        .underlayName(cohort.getUnderlayName())
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

  private static ApiCriteriaGroupSectionV3 toApiObject(
      CohortRevision.CriteriaGroupSection criteriaGroupSection) {
    return new ApiCriteriaGroupSectionV3()
        .id(criteriaGroupSection.getId())
        .displayName(criteriaGroupSection.getDisplayName())
        .operator(
            ApiCriteriaGroupSectionV3.OperatorEnum.fromValue(
                criteriaGroupSection.getOperator().name()))
        .excluded(criteriaGroupSection.isExcluded())
        .criteriaGroups(
            criteriaGroupSection.getCriteriaGroups().stream()
                .map(criteriaGroup -> toApiObject(criteriaGroup))
                .collect(Collectors.toList()));
  }

  private static ApiCriteriaGroupV3 toApiObject(CohortRevision.CriteriaGroup criteriaGroup) {
    return new ApiCriteriaGroupV3()
        .id(criteriaGroup.getId())
        .displayName(criteriaGroup.getDisplayName())
        .entity(criteriaGroup.getEntity())
        .groupByCountOperator(
            ApiBinaryOperatorV2.valueOf(criteriaGroup.getGroupByCountOperator().name()))
        .groupByCountValue(criteriaGroup.getGroupByCountValue())
        .criteria(
            criteriaGroup.getCriteria().stream()
                .map(criteria -> toApiObject(criteria))
                .collect(Collectors.toList()));
  }

  public static ApiCriteriaV2 toApiObject(Criteria criteria) {
    return new ApiCriteriaV2()
        .id(criteria.getId())
        .displayName(criteria.getDisplayName())
        .pluginName(criteria.getPluginName())
        .selectionData(criteria.getSelectionData())
        .uiConfig(criteria.getUiConfig());
  }

  public static ApiInstanceCountV2 toApiObject(EntityInstanceCount entityInstanceCount) {
    ApiInstanceCountV2 instanceCount = new ApiInstanceCountV2();
    Map<String, ApiValueDisplayV2> attributes = new HashMap<>();
    for (Map.Entry<Attribute, ValueDisplay> attributeValue :
        entityInstanceCount.getAttributeValues().entrySet()) {
      attributes.put(attributeValue.getKey().getName(), toApiObject(attributeValue.getValue()));
    }

    return instanceCount
        .count(Math.toIntExact(entityInstanceCount.getCount()))
        .attributes(attributes);
  }

  public static ApiAnnotationValueV2 toApiObject(AnnotationValueV1 annotationValue) {
    return new ApiAnnotationValueV2()
        .id(annotationValue.getAnnotationValueId())
        .review(annotationValue.getReviewId())
        .value(toApiObject(annotationValue.getLiteral()));
  }
}

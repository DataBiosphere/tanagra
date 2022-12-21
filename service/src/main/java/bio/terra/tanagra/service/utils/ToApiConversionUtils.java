package bio.terra.tanagra.service.utils;

import bio.terra.tanagra.exception.SystemException;
import bio.terra.tanagra.generated.model.ApiAnnotationValueV2;
import bio.terra.tanagra.generated.model.ApiAttributeV2;
import bio.terra.tanagra.generated.model.ApiCohortV2;
import bio.terra.tanagra.generated.model.ApiConceptSetV2;
import bio.terra.tanagra.generated.model.ApiCriteriaGroupV2;
import bio.terra.tanagra.generated.model.ApiCriteriaV2;
import bio.terra.tanagra.generated.model.ApiDataTypeV2;
import bio.terra.tanagra.generated.model.ApiInstanceCountV2;
import bio.terra.tanagra.generated.model.ApiLiteralV2;
import bio.terra.tanagra.generated.model.ApiLiteralV2ValueUnion;
import bio.terra.tanagra.generated.model.ApiValueDisplayV2;
import bio.terra.tanagra.query.Literal;
import bio.terra.tanagra.service.artifact.AnnotationValue;
import bio.terra.tanagra.service.artifact.Cohort;
import bio.terra.tanagra.service.artifact.ConceptSet;
import bio.terra.tanagra.service.artifact.Criteria;
import bio.terra.tanagra.service.artifact.CriteriaGroup;
import bio.terra.tanagra.service.instances.EntityInstanceCount;
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
    return new ApiValueDisplayV2()
        .value(toApiObject(valueDisplay.getValue()))
        .display(valueDisplay.getDisplay());
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

  /**
   * Convert the internal Cohort object to an API Cohort object.
   *
   * <p>In the backend code, a Cohort = a filter on the primary entity, and a CohortRevisionGroup =
   * all past versions and the current version of a filter on the primary entity.
   */
  public static ApiCohortV2 toApiObject(Cohort cohort) {
    return new ApiCohortV2()
        .id(cohort.getCohortRevisionGroupId())
        .underlayName(cohort.getUnderlayName())
        .displayName(cohort.getDisplayName())
        .description(cohort.getDescription())
        .created(cohort.getCreated())
        .createdBy(cohort.getCreatedBy())
        .lastModified(cohort.getLastModified())
        .criteriaGroups(
            cohort.getCriteriaGroups().stream()
                .map(criteriaGroup -> toApiObject(criteriaGroup))
                .collect(Collectors.toList()));
  }

  private static ApiCriteriaGroupV2 toApiObject(CriteriaGroup criteriaGroup) {
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

  public static ApiCriteriaV2 toApiObject(Criteria criteria) {
    return new ApiCriteriaV2()
        .id(criteria.getUserFacingCriteriaId())
        .displayName(criteria.getDisplayName())
        .pluginName(criteria.getPluginName())
        .selectionData(criteria.getSelectionData())
        .uiConfig(criteria.getUiConfig());
  }

  public static ApiConceptSetV2 toApiObject(ConceptSet conceptSet) {
    return new ApiConceptSetV2()
        .id(conceptSet.getConceptSetId())
        .underlayName(conceptSet.getUnderlayName())
        .entity(conceptSet.getEntityName())
        .displayName(conceptSet.getDisplayName())
        .description(conceptSet.getDescription())
        .created(conceptSet.getCreated())
        .createdBy(conceptSet.getCreatedBy())
        .lastModified(conceptSet.getLastModified())
        .criteria(
            conceptSet.getCriteria() == null
                ? null
                : ToApiConversionUtils.toApiObject(conceptSet.getCriteria()));
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

  public static ApiAnnotationValueV2 toApiObject(AnnotationValue annotationValue) {
    return new ApiAnnotationValueV2()
        .id(annotationValue.getAnnotationValueId())
        .review(annotationValue.getReviewId())
        .value(toApiObject(annotationValue.getLiteral()));
  }
}

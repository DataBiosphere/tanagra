package bio.terra.tanagra.service.artifact.reviewquery;

import bio.terra.tanagra.api.shared.BinaryOperator;
import bio.terra.tanagra.api.shared.Literal;
import bio.terra.tanagra.exception.SystemException;
import bio.terra.tanagra.service.artifact.model.AnnotationKey;
import bio.terra.tanagra.service.artifact.model.AnnotationValue;
import java.util.List;

public class AnnotationFilter {
  private final AnnotationKey annotationKey;
  private final BinaryOperator operator;
  private final Literal value;

  public AnnotationFilter(AnnotationKey annotationKey, BinaryOperator operator, Literal value) {
    this.annotationKey = annotationKey;
    this.operator = operator;
    this.value = value;
  }

  public boolean isMatch(List<AnnotationValue> annotationValues) {
    return annotationValues.stream()
        .anyMatch(
            av -> {
              if (!av.getAnnotationKeyId().equals(annotationKey.getId())) {
                return false;
              }
              int comparison = av.getLiteral().compareTo(value);
              switch (operator) {
                case EQUALS:
                  return comparison == 0;
                case NOT_EQUALS:
                  return comparison != 0;
                case LESS_THAN:
                  return comparison == -1;
                case GREATER_THAN:
                  return comparison == 1;
                case LESS_THAN_OR_EQUAL:
                  return comparison <= 0;
                case GREATER_THAN_OR_EQUAL:
                  return comparison >= 0;
                default:
                  throw new SystemException("Unsupported annotation filter operator: " + operator);
              }
            });
  }
}

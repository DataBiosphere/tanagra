package bio.terra.tanagra.service.query;

import bio.terra.tanagra.query.OrderByDirection;
import bio.terra.tanagra.service.artifact.AnnotationKey;
import bio.terra.tanagra.service.artifact.AnnotationValue;
import bio.terra.tanagra.underlay.Attribute;
import bio.terra.tanagra.underlay.ValueDisplay;
import java.util.Comparator;
import java.util.Optional;

public class ReviewQueryOrderBy implements Comparator<ReviewInstance> {
  private final Attribute attribute;
  private final AnnotationKey annotationKey;
  private final OrderByDirection direction;

  public ReviewQueryOrderBy(Attribute attribute, OrderByDirection direction) {
    this.attribute = attribute;
    this.annotationKey = null;
    this.direction = direction;
  }

  public ReviewQueryOrderBy(AnnotationKey annotationKey, OrderByDirection direction) {
    this.attribute = null;
    this.annotationKey = annotationKey;
    this.direction = direction;
  }

  public Attribute getAttribute() {
    return attribute;
  }

  public AnnotationKey getAnnotationKey() {
    return annotationKey;
  }

  public OrderByDirection getDirection() {
    return direction;
  }

  public boolean isByAttribute() {
    return attribute != null;
  }

  @Override
  public int compare(ReviewInstance o1, ReviewInstance o2) {
    int returnVal;

    if (isByAttribute()) {
      ValueDisplay valueDisplay1 = o1.getAttributeValues().get(getAttribute());
      ValueDisplay valueDisplay2 = o2.getAttributeValues().get(getAttribute());

      if (getAttribute().getType().equals(Attribute.Type.KEY_AND_DISPLAY)) {
        returnVal = valueDisplay1.getDisplay().compareTo(valueDisplay2.getDisplay());
      } else {
        returnVal = valueDisplay1.getValue().compareTo(valueDisplay2.getValue());
      }
    } else {
      Optional<AnnotationValue> annotationValue1 =
          o1.getAnnotationValues().stream()
              .filter(av -> av.getAnnotationKeyId().equals(getAnnotationKey().getId()))
              .findFirst();
      Optional<AnnotationValue> annotationValue2 =
          o2.getAnnotationValues().stream()
              .filter(av -> av.getAnnotationKeyId().equals(getAnnotationKey().getId()))
              .findFirst();

      if (annotationValue1.isEmpty() && annotationValue2.isEmpty()) {
        returnVal = 0;
      } else if (annotationValue1.isEmpty() && annotationValue2.isPresent()) {
        returnVal = -1;
      } else if (annotationValue1.isPresent() && annotationValue2.isEmpty()) {
        returnVal = 1;
      } else {
        returnVal =
            annotationValue1.get().getLiteral().compareTo(annotationValue2.get().getLiteral());
      }
    }

    return getDirection().equals(OrderByDirection.ASCENDING) ? returnVal : (-1 * returnVal);
  }
}

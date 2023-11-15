package bio.terra.tanagra.service.artifact.reviewquery;

import bio.terra.tanagra.api.query.ValueDisplay;
import bio.terra.tanagra.service.artifact.model.AnnotationValue;
import bio.terra.tanagra.underlay.entitymodel.Attribute;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class ReviewInstance {
  private final int stableIndex;
  private final Map<Attribute, ValueDisplay> attributeValues;
  private final List<AnnotationValue> annotationValues;

  public ReviewInstance(
      int stableIndex,
      Map<Attribute, ValueDisplay> attributeValues,
      List<AnnotationValue> annotationValues) {
    this.stableIndex = stableIndex;
    this.attributeValues = attributeValues;
    this.annotationValues = annotationValues;
  }

  public int getStableIndex() {
    return stableIndex;
  }

  public Map<Attribute, ValueDisplay> getAttributeValues() {
    return Collections.unmodifiableMap(attributeValues);
  }

  public List<AnnotationValue> getAnnotationValues() {
    return Collections.unmodifiableList(annotationValues);
  }

  public void addAnnotationValue(AnnotationValue annotationValue) {
    annotationValues.add(annotationValue);
  }
}

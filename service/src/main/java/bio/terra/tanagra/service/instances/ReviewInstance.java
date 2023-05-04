package bio.terra.tanagra.service.instances;

import bio.terra.tanagra.service.artifact.AnnotationValue;
import bio.terra.tanagra.underlay.Attribute;
import bio.terra.tanagra.underlay.ValueDisplay;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class ReviewInstance {
  private final Map<Attribute, ValueDisplay> attributeValues;
  private final List<AnnotationValue> annotationValues;

  public ReviewInstance(
      Map<Attribute, ValueDisplay> attributeValues, List<AnnotationValue> annotationValues) {
    this.attributeValues = attributeValues;
    this.annotationValues = annotationValues;
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

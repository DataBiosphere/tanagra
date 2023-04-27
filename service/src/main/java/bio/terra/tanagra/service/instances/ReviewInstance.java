package bio.terra.tanagra.service.instances;

import bio.terra.tanagra.service.artifact.AnnotationValueV1;
import bio.terra.tanagra.underlay.Attribute;
import bio.terra.tanagra.underlay.ValueDisplay;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class ReviewInstance {
  private final Map<Attribute, ValueDisplay> attributeValues;
  private final List<AnnotationValueV1> annotationValues;

  public ReviewInstance(
      Map<Attribute, ValueDisplay> attributeValues, List<AnnotationValueV1> annotationValues) {
    this.attributeValues = attributeValues;
    this.annotationValues = annotationValues;
  }

  public Map<Attribute, ValueDisplay> getAttributeValues() {
    return Collections.unmodifiableMap(attributeValues);
  }

  public List<AnnotationValueV1> getAnnotationValues() {
    return Collections.unmodifiableList(annotationValues);
  }

  public void addAnnotationValue(AnnotationValueV1 annotationValue) {
    annotationValues.add(annotationValue);
  }
}

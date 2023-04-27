package bio.terra.tanagra.service.model;

import bio.terra.tanagra.service.instances.EntityInstance;
import java.util.List;

public class ReviewInstance {
  private final EntityInstance entityInstance;
  private final List<AnnotationValue> annotationValues;

  public ReviewInstance(EntityInstance entityInstance, List<AnnotationValue> annotationValues) {
    this.entityInstance = entityInstance;
    this.annotationValues = annotationValues;
  }

  public EntityInstance getEntityInstance() {
    return entityInstance;
  }

  public List<AnnotationValue> getAnnotationValues() {
    return annotationValues;
  }
}

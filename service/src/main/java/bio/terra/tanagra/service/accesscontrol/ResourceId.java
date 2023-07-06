package bio.terra.tanagra.service.accesscontrol;

import static bio.terra.tanagra.service.accesscontrol.ResourceType.*;

import bio.terra.tanagra.exception.SystemException;
import java.util.List;
import java.util.Objects;
import org.apache.logging.log4j.util.Strings;

public final class ResourceId {
  private static final char COMPOSITE_ID_SEPARATOR = '-';
  private final ResourceType type;
  private final String underlay;
  private final String study;
  private final String cohort;
  private final String conceptSet;
  private final String review;
  private final String annotationKey;
  private final boolean isNull;

  private ResourceId(Builder builder) {
    this.type = builder.type;
    this.underlay = builder.underlay;
    this.study = builder.study;
    this.cohort = builder.cohort;
    this.conceptSet = builder.conceptSet;
    this.review = builder.review;
    this.annotationKey = builder.annotationKey;
    this.isNull = builder.isNull;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static ResourceId forUnderlay(String underlay) {
    return builder().type(UNDERLAY).underlay(underlay).build();
  }

  public static ResourceId forStudy(String study) {
    return builder().type(STUDY).study(study).build();
  }

  public static ResourceId forCohort(String study, String cohort) {
    return builder().type(COHORT).study(study).cohort(cohort).build();
  }

  public static ResourceId forConceptSet(String study, String conceptSet) {
    return builder().type(CONCEPT_SET).study(study).conceptSet(conceptSet).build();
  }

  public static ResourceId forReview(String study, String cohort, String review) {
    return builder().type(REVIEW).study(study).cohort(cohort).review(review).build();
  }

  public static ResourceId forAnnotationKey(String study, String cohort, String annotationKey) {
    return builder()
        .type(ANNOTATION_KEY)
        .study(study)
        .cohort(cohort)
        .annotationKey(annotationKey)
        .build();
  }

  public ResourceType getType() {
    return type;
  }

  public boolean isNull() {
    return isNull;
  }

  public ResourceId getParent() {
    switch (type) {
      case COHORT:
      case CONCEPT_SET:
        return forStudy(study);
      case REVIEW:
      case ANNOTATION_KEY:
        return forCohort(study, cohort);
      default:
        return null;
    }
  }

  public String getId() {
    if (isNull) {
      return "NULL_" + type;
    }
    switch (type) {
      case UNDERLAY:
        return underlay;
      case STUDY:
        return study;
      case COHORT:
        return buildCompositeId(List.of(study, cohort));
      case CONCEPT_SET:
        return buildCompositeId(List.of(study, conceptSet));
      case REVIEW:
        return buildCompositeId(List.of(study, cohort, review));
      case ANNOTATION_KEY:
        return buildCompositeId(List.of(study, cohort, annotationKey));
      default:
        throw new IllegalArgumentException("Unknown resource type: " + type);
    }
  }

  private static String buildCompositeId(List<String> ids) {
    return Strings.join(ids, COMPOSITE_ID_SEPARATOR);
  }

  public String getUnderlay() {
    if (type != UNDERLAY) {
      throw new SystemException("Underlay id is not set for resource type: " + type);
    }
    return underlay;
  }

  public String getStudy() {
    if (type == UNDERLAY) {
      throw new SystemException("Study id is not set for resource type: " + type);
    }
    return study;
  }

  public String getCohort() {
    if (!List.of(COHORT, REVIEW, ANNOTATION_KEY).contains(type)) {
      throw new SystemException("Cohort id is not set for resource type: " + type);
    }
    return cohort;
  }

  public String getConceptSet() {
    if (type != CONCEPT_SET) {
      throw new SystemException("Concept set id is not set for resource type: " + type);
    }
    return conceptSet;
  }

  public String getReview() {
    if (type != REVIEW) {
      throw new SystemException("Review id is not set for resource type: " + type);
    }
    return review;
  }

  public String getAnnotationKey() {
    if (type != ANNOTATION_KEY) {
      throw new SystemException("Annotation key id is not set for resource type: " + type);
    }
    return annotationKey;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ResourceId that = (ResourceId) o;
    return isNull == that.isNull
        && type == that.type
        && Objects.equals(underlay, that.underlay)
        && Objects.equals(study, that.study)
        && Objects.equals(cohort, that.cohort)
        && Objects.equals(conceptSet, that.conceptSet)
        && Objects.equals(review, that.review)
        && Objects.equals(annotationKey, that.annotationKey);
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, underlay, study, cohort, conceptSet, review, annotationKey, isNull);
  }

  public static class Builder {
    private ResourceType type;
    private String underlay;
    private String study;
    private String cohort;
    private String conceptSet;
    private String review;
    private String annotationKey;
    private boolean isNull;

    public Builder type(ResourceType type) {
      this.type = type;
      return this;
    }

    public Builder underlay(String underlay) {
      this.underlay = underlay;
      return this;
    }

    public Builder study(String study) {
      this.study = study;
      return this;
    }

    public Builder cohort(String cohort) {
      this.cohort = cohort;
      return this;
    }

    public Builder conceptSet(String conceptSet) {
      this.conceptSet = conceptSet;
      return this;
    }

    public Builder review(String review) {
      this.review = review;
      return this;
    }

    public Builder annotationKey(String annotationKey) {
      this.annotationKey = annotationKey;
      return this;
    }

    public Builder isNull(boolean isNull) {
      this.isNull = isNull;
      return this;
    }

    public ResourceId build() {
      return new ResourceId(this);
    }
  }
}

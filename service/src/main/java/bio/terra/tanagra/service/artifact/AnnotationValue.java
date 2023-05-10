package bio.terra.tanagra.service.artifact;

import bio.terra.tanagra.query.Literal;
import java.util.Objects;

public class AnnotationValue {
  private final Literal literal;
  private final int cohortRevisionVersion;
  private final String annotationKeyId;
  private final String instanceId;
  private final boolean isMostRecent;
  private final boolean isPartOfSelectedReview;

  private AnnotationValue(
      Literal literal,
      int cohortRevisionVersion,
      String annotationKeyId,
      String instanceId,
      boolean isMostRecent,
      boolean isPartOfSelectedReview) {
    this.literal = literal;
    this.cohortRevisionVersion = cohortRevisionVersion;
    this.annotationKeyId = annotationKeyId;
    this.instanceId = instanceId;
    this.isMostRecent = isMostRecent;
    this.isPartOfSelectedReview = isPartOfSelectedReview;
  }

  public static Builder builder() {
    return new Builder();
  }

  public Literal getLiteral() {
    return literal;
  }

  public int getCohortRevisionVersion() {
    return cohortRevisionVersion;
  }

  public String getAnnotationKeyId() {
    return annotationKeyId;
  }

  public String getInstanceId() {
    return instanceId;
  }

  public boolean isMostRecent() {
    return isMostRecent;
  }

  public boolean isPartOfSelectedReview() {
    return isPartOfSelectedReview;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AnnotationValue that = (AnnotationValue) o;
    return cohortRevisionVersion == that.cohortRevisionVersion
        && isMostRecent == that.isMostRecent
        && isPartOfSelectedReview == that.isPartOfSelectedReview
        && literal.equals(that.literal)
        && annotationKeyId.equals(that.annotationKeyId)
        && instanceId.equals(that.instanceId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        literal,
        cohortRevisionVersion,
        annotationKeyId,
        instanceId,
        isMostRecent,
        isPartOfSelectedReview);
  }

  public static class Builder {
    private Literal literal;
    private int cohortRevisionVersion;
    private String annotationKeyId;
    private String instanceId;
    private boolean isMostRecent;
    private boolean isPartOfSelectedReview;

    public Builder literal(Literal literal) {
      this.literal = literal;
      return this;
    }

    public Builder cohortRevisionVersion(int cohortRevisionVersion) {
      this.cohortRevisionVersion = cohortRevisionVersion;
      return this;
    }

    public Builder annotationKeyId(String annotationKeyId) {
      this.annotationKeyId = annotationKeyId;
      return this;
    }

    public Builder instanceId(String instanceId) {
      this.instanceId = instanceId;
      return this;
    }

    public Builder isMostRecent(boolean isMostRecent) {
      this.isMostRecent = isMostRecent;
      return this;
    }

    public Builder isPartOfSelectedReview(boolean isPartOfSelectedReview) {
      this.isPartOfSelectedReview = isPartOfSelectedReview;
      return this;
    }

    public AnnotationValue build() {
      return new AnnotationValue(
          literal,
          cohortRevisionVersion,
          annotationKeyId,
          instanceId,
          isMostRecent,
          isPartOfSelectedReview);
    }

    public int getCohortRevisionVersion() {
      return cohortRevisionVersion;
    }

    public String getAnnotationKeyId() {
      return annotationKeyId;
    }

    public String getInstanceId() {
      return instanceId;
    }
  }
}

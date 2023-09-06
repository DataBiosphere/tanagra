package bio.terra.tanagra.service.artifact;

import java.util.Objects;

public class ActivityLogResource {
  public enum Type {
    STUDY,
    COHORT,
    REVIEW
  }

  private final Type type;
  private final String studyId;
  private final String cohortId;
  private final String cohortRevisionId;
  private final String reviewId;
  private final String studyDisplayName;
  private final String cohortDisplayName;
  private final String reviewDisplayName;

  private ActivityLogResource(Builder builder) {
    this.type = builder.type;
    this.studyId = builder.studyId;
    this.cohortId = builder.cohortId;
    this.cohortRevisionId = builder.cohortRevisionId;
    this.reviewId = builder.reviewId;
    this.studyDisplayName = builder.studyDisplayName;
    this.cohortDisplayName = builder.cohortDisplayName;
    this.reviewDisplayName = builder.reviewDisplayName;
  }

  public Type getType() {
    return type;
  }

  public String getStudyId() {
    return studyId;
  }

  public String getCohortId() {
    return cohortId;
  }

  public String getCohortRevisionId() {
    return cohortRevisionId;
  }

  public String getReviewId() {
    return reviewId;
  }

  public String getStudyDisplayName() {
    return studyDisplayName;
  }

  public String getCohortDisplayName() {
    return cohortDisplayName;
  }

  public String getReviewDisplayName() {
    return reviewDisplayName;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private Type type;
    private String studyId;
    private String cohortId;
    private String cohortRevisionId;
    private String reviewId;
    private String studyDisplayName;
    private String cohortDisplayName;
    private String reviewDisplayName;

    public Builder type(Type type) {
      this.type = type;
      return this;
    }

    public Builder studyId(String studyId) {
      this.studyId = studyId;
      return this;
    }

    public Builder cohortId(String cohortId) {
      this.cohortId = cohortId;
      return this;
    }

    public Builder cohortRevisionId(String cohortRevisionId) {
      this.cohortRevisionId = cohortRevisionId;
      return this;
    }

    public Builder reviewId(String reviewId) {
      this.reviewId = reviewId;
      return this;
    }

    public Builder studyDisplayName(String studyDisplayName) {
      this.studyDisplayName = studyDisplayName;
      return this;
    }

    public Builder cohortDisplayName(String cohortDisplayName) {
      this.cohortDisplayName = cohortDisplayName;
      return this;
    }

    public Builder reviewDisplayName(String reviewDisplayName) {
      this.reviewDisplayName = reviewDisplayName;
      return this;
    }

    public ActivityLogResource build() {
      return new ActivityLogResource(this);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ActivityLogResource that = (ActivityLogResource) o;
    return type == that.type
        && Objects.equals(studyId, that.studyId)
        && Objects.equals(cohortId, that.cohortId)
        && Objects.equals(cohortRevisionId, that.cohortRevisionId)
        && Objects.equals(reviewId, that.reviewId)
        && Objects.equals(studyDisplayName, that.studyDisplayName)
        && Objects.equals(cohortDisplayName, that.cohortDisplayName)
        && Objects.equals(reviewDisplayName, that.reviewDisplayName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        type,
        studyId,
        cohortId,
        cohortRevisionId,
        reviewId,
        studyDisplayName,
        cohortDisplayName,
        reviewDisplayName);
  }
}

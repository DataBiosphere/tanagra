package bio.terra.tanagra.service.artifact;

import java.time.OffsetDateTime;
import javax.annotation.Nullable;

public class Review {
  private final String cohortId;
  private final String reviewId;
  private final @Nullable String displayName;
  private final @Nullable String description;
  private final int size;
  private final OffsetDateTime created;
  private final String createdBy;
  private final OffsetDateTime lastModified;
  private final Cohort cohort;

  private Review(Builder builder) {
    this.cohortId = builder.cohortId;
    this.reviewId = builder.reviewId;
    this.displayName = builder.displayName;
    this.description = builder.description;
    this.size = builder.size;
    this.created = builder.created;
    this.createdBy = builder.createdBy;
    this.lastModified = builder.lastModified;
    this.cohort = builder.cohort;
  }

  public static Builder builder() {
    return new Builder();
  }

  /** Unique (per study) identifier of the cohort this review belongs to. */
  public String getCohortId() {
    return cohortId;
  }

  /** Unique (per cohort) identifier of this review. */
  public String getReviewId() {
    return reviewId;
  }

  /** Optional display name of this review. */
  public String getDisplayName() {
    return displayName;
  }

  /** Optional description of this review. */
  public String getDescription() {
    return description;
  }

  /** Size of the review, number of primary entity instances. */
  public int getSize() {
    return size;
  }

  public OffsetDateTime getCreated() {
    return created;
  }

  public String getCreatedBy() {
    return createdBy;
  }

  public OffsetDateTime getLastModified() {
    return lastModified;
  }

  /** Cohort revision that this review is pinned to. */
  public Cohort getCohort() {
    return cohort;
  }

  public static class Builder {
    private String cohortId;
    private String reviewId;
    private @Nullable String displayName;
    private @Nullable String description;
    private int size;
    private OffsetDateTime created;
    private String createdBy;
    private OffsetDateTime lastModified;
    private Cohort cohort;

    public Builder cohortId(String cohortId) {
      this.cohortId = cohortId;
      return this;
    }

    public Builder reviewId(String reviewId) {
      this.reviewId = reviewId;
      return this;
    }

    public Builder displayName(String displayName) {
      this.displayName = displayName;
      return this;
    }

    public Builder description(String description) {
      this.description = description;
      return this;
    }

    public Builder size(int size) {
      this.size = size;
      return this;
    }

    public Builder created(OffsetDateTime created) {
      this.created = created;
      return this;
    }

    public Builder createdBy(String createdBy) {
      this.createdBy = createdBy;
      return this;
    }

    public Builder lastModified(OffsetDateTime lastModified) {
      this.lastModified = lastModified;
      return this;
    }

    public Builder cohort(Cohort cohort) {
      this.cohort = cohort;
      return this;
    }

    public String getCohortId() {
      return cohortId;
    }

    public Review build() {
      return new Review(this);
    }
  }
}

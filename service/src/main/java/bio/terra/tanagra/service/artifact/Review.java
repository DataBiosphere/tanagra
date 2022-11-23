package bio.terra.tanagra.service.artifact;

import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import javax.annotation.Nullable;

public class Review {
  private final String cohortId;
  private final String reviewId;
  private final @Nullable String displayName;
  private final @Nullable String description;
  private final int size;
  private final Timestamp created;
  private final Cohort cohort;

  private Review(Builder builder) {
    this.cohortId = builder.cohortId;
    this.reviewId = builder.reviewId;
    this.displayName = builder.displayName;
    this.description = builder.description;
    this.size = builder.size;
    this.created = builder.created;
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

  /** Timestamp of when this cohort was last modified. */
  public OffsetDateTime getCreatedUTC() {
    return created.toInstant().atOffset(ZoneOffset.UTC);
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
    private Timestamp created;
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

    public Builder created(Timestamp created) {
      this.created = (Timestamp) created.clone();
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

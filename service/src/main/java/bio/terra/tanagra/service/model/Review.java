package bio.terra.tanagra.service.model;

import java.time.OffsetDateTime;
import javax.annotation.Nullable;

public class Review {
  private final String id;
  private final int size;
  private final CohortRevision cohortRevision;
  private final OffsetDateTime created;
  private final String createdBy;
  private final OffsetDateTime lastModified;
  private final @Nullable String displayName;
  private final @Nullable String description;

  public Review(
      String id,
      int size,
      CohortRevision cohortRevision,
      OffsetDateTime created,
      String createdBy,
      OffsetDateTime lastModified,
      @Nullable String displayName,
      @Nullable String description) {
    this.id = id;
    this.size = size;
    this.cohortRevision = cohortRevision;
    this.created = created;
    this.createdBy = createdBy;
    this.lastModified = lastModified;
    this.displayName = displayName;
    this.description = description;
  }

  public String getId() {
    return id;
  }

  public int getSize() {
    return size;
  }

  public CohortRevision getCohortRevision() {
    return cohortRevision;
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

  @Nullable
  public String getDisplayName() {
    return displayName;
  }

  @Nullable
  public String getDescription() {
    return description;
  }
}

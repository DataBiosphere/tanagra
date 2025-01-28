package bio.terra.tanagra.service.artifact.model;

import static bio.terra.tanagra.service.artifact.model.Study.MAX_DISPLAY_NAME_LENGTH;

import bio.terra.common.exception.BadRequestException;
import bio.terra.tanagra.exception.SystemException;
import jakarta.annotation.Nullable;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class Cohort {
  private final String id;
  private final String underlay;
  private final OffsetDateTime created;
  private final String createdBy;
  private final OffsetDateTime lastModified;
  private final String lastModifiedBy;
  private final @Nullable String displayName;
  private final @Nullable String description;
  private final List<CohortRevision> revisions;
  private final boolean isDeleted;

  private Cohort(Builder builder) {
    this.id = builder.id;
    this.underlay = builder.underlay;
    this.created = builder.created;
    this.createdBy = builder.createdBy;
    this.lastModified = builder.lastModified;
    this.lastModifiedBy = builder.lastModifiedBy;
    this.displayName = builder.displayName;
    this.description = builder.description;
    this.revisions = builder.revisions;
    this.isDeleted = builder.isDeleted;
  }

  public static Builder builder() {
    return new Builder();
  }

  public String getId() {
    return id;
  }

  public String getUnderlay() {
    return underlay;
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

  public String getLastModifiedBy() {
    return lastModifiedBy;
  }

  @Nullable
  public String getDisplayName() {
    return displayName;
  }

  @Nullable
  public String getDescription() {
    return description;
  }

  public List<CohortRevision> getRevisions() {
    return Collections.unmodifiableList(revisions);
  }

  public CohortRevision getMostRecentRevision() {
    Optional<CohortRevision> mostRecentRevision =
        revisions.stream().filter(CohortRevision::isMostRecent).findFirst();
    if (mostRecentRevision.isEmpty()) {
      throw new SystemException("Most recent cohort revision not found " + id);
    }
    return mostRecentRevision.get();
  }

  public boolean isDeleted() {
    return isDeleted;
  }

  public static class Builder {
    private String id;
    private String underlay;
    private OffsetDateTime created;
    private String createdBy;
    private OffsetDateTime lastModified;
    private String lastModifiedBy;
    private @Nullable String displayName;
    private @Nullable String description;
    private List<CohortRevision> revisions;
    private boolean isDeleted;

    public Builder id(String id) {
      this.id = id;
      return this;
    }

    public Builder underlay(String underlay) {
      this.underlay = underlay;
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

    public Builder lastModifiedBy(String lastModifiedBy) {
      this.lastModifiedBy = lastModifiedBy;
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

    public Builder revisions(List<CohortRevision> revisions) {
      this.revisions = revisions;
      return this;
    }

    public Builder isDeleted(boolean isDeleted) {
      this.isDeleted = isDeleted;
      return this;
    }

    public Cohort build() {
      if (id == null) {
        id = ArtifactUtils.newId();
      }
      if (displayName != null && displayName.length() > MAX_DISPLAY_NAME_LENGTH) {
        throw new BadRequestException(
            "Cohort name cannot be greater than " + MAX_DISPLAY_NAME_LENGTH + " characters");
      }
      if (lastModifiedBy == null) {
        lastModifiedBy = createdBy;
      }
      revisions = new ArrayList<>(revisions);
      revisions.sort(Comparator.comparing(CohortRevision::getVersion));
      return new Cohort(this);
    }

    public String getId() {
      return id;
    }

    public String getUnderlay() {
      return underlay;
    }

    public String getCreatedBy() {
      return createdBy;
    }

    public void addRevision(CohortRevision cohortRevision) {
      if (revisions == null) {
        revisions = new ArrayList<>();
      }
      revisions.add(cohortRevision);
    }
  }
}

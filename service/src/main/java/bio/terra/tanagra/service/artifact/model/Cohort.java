package bio.terra.tanagra.service.artifact.model;

import bio.terra.tanagra.exception.SystemException;
import java.time.OffsetDateTime;
import java.util.*;
import javax.annotation.Nullable;
import org.apache.commons.lang3.RandomStringUtils;

public class Cohort {
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

  public String getDisplayName() {
    return displayName;
  }

  public String getDescription() {
    return description;
  }

  public List<CohortRevision> getRevisions() {
    return Collections.unmodifiableList(revisions);
  }

  public CohortRevision getMostRecentRevision() {
    Optional<CohortRevision> mostRecentRevision =
        revisions.stream().filter(r -> r.isMostRecent()).findFirst();
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
        id = RandomStringUtils.randomAlphanumeric(10);
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

    public void addRevision(CohortRevision cohortRevision) {
      if (revisions == null) {
        revisions = new ArrayList<>();
      }
      revisions.add(cohortRevision);
    }
  }
}

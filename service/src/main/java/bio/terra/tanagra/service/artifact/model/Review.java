package bio.terra.tanagra.service.artifact.model;

import static bio.terra.tanagra.service.artifact.model.Study.MAX_DISPLAY_NAME_LENGTH;

import bio.terra.common.exception.*;
import jakarta.annotation.Nullable;
import java.time.OffsetDateTime;
import org.apache.commons.lang3.RandomStringUtils;

public final class Review {
  private final String id;
  private final @Nullable String displayName;
  private final @Nullable String description;
  private final int size;
  private final CohortRevision revision;
  private final OffsetDateTime created;
  private final String createdBy;
  private final OffsetDateTime lastModified;
  private final String lastModifiedBy;
  private final boolean isDeleted;

  private Review(Builder builder) {
    this.id = builder.id;
    this.displayName = builder.displayName;
    this.description = builder.description;
    this.size = builder.size;
    this.revision = builder.revision;
    this.created = builder.created;
    this.createdBy = builder.createdBy;
    this.lastModified = builder.lastModified;
    this.lastModifiedBy = builder.lastModifiedBy;
    this.isDeleted = builder.isDeleted;
  }

  public static Builder builder() {
    return new Builder();
  }

  public String getId() {
    return id;
  }

  public String getDisplayName() {
    return displayName;
  }

  public String getDescription() {
    return description;
  }

  public int getSize() {
    return size;
  }

  public CohortRevision getRevision() {
    return revision;
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

  public boolean isDeleted() {
    return isDeleted;
  }

  public static class Builder {
    private String id;
    private String displayName;
    private String description;
    private int size;
    private CohortRevision revision;
    private OffsetDateTime created;
    private String createdBy;
    private OffsetDateTime lastModified;
    private String lastModifiedBy;
    private boolean isDeleted;

    public Builder id(String id) {
      this.id = id;
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

    public Builder revision(CohortRevision revision) {
      this.revision = revision;
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

    public Builder isDeleted(boolean isDeleted) {
      this.isDeleted = isDeleted;
      return this;
    }

    public Review build() {
      if (id == null) {
        id = RandomStringUtils.randomAlphanumeric(10);
      }
      if (displayName != null && displayName.length() > MAX_DISPLAY_NAME_LENGTH) {
        throw new BadRequestException(
            "Review name cannot be greater than " + MAX_DISPLAY_NAME_LENGTH + " characters");
      }
      return new Review(this);
    }

    public String getId() {
      return id;
    }

    public int getSize() {
      return size;
    }
  }
}

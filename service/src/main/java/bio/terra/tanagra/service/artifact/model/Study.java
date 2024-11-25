package bio.terra.tanagra.service.artifact.model;

import bio.terra.common.exception.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import jakarta.annotation.Nullable;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * Internal representation of a Study.
 *
 * <p>A study is a collection of cohorts, feature sets, datasets, and cohort reviews.
 */
@JsonDeserialize(builder = Study.Builder.class)
public class Study {
  public static final int MAX_DISPLAY_NAME_LENGTH = 50;
  private final String id;
  private final @Nullable String displayName;
  private final @Nullable String description;
  private final Map<String, String> properties;
  private final OffsetDateTime created;
  private final String createdBy;
  private final OffsetDateTime lastModified;
  private final String lastModifiedBy;
  private final boolean isDeleted;

  public Study(Builder builder) {
    this.id = builder.id;
    this.displayName = builder.displayName;
    this.description = builder.description;
    this.properties = builder.properties;
    this.created = builder.created;
    this.createdBy = builder.createdBy;
    this.lastModified = builder.lastModified;
    this.lastModifiedBy = builder.lastModifiedBy;
    this.isDeleted = builder.isDeleted;
  }

  /** The globally unique identifier of this study. */
  public String getId() {
    return id;
  }

  /** Optional display name for the study. */
  public String getDisplayName() {
    return displayName;
  }

  /** Optional description of the study. */
  public String getDescription() {
    return description;
  }

  /** Caller-specified set of key-value pairs. Used to store generic study metadata. */
  public Map<String, String> getProperties() {
    return properties;
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    Study study = (Study) o;

    return new EqualsBuilder()
        .append(id, study.id)
        .append(displayName, study.displayName)
        .append(description, study.description)
        .append(properties, study.properties)
        .append(created, study.created)
        .append(createdBy, study.createdBy)
        .append(lastModified, study.lastModified)
        .append(lastModifiedBy, study.lastModifiedBy)
        .append(isDeleted, study.isDeleted)
        .isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(15, 79)
        .append(id)
        .append(displayName)
        .append(description)
        .append(properties)
        .append(created)
        .append(createdBy)
        .append(lastModified)
        .append(lastModifiedBy)
        .append(isDeleted)
        .toHashCode();
  }

  public static Builder builder() {
    return new Builder();
  }

  @JsonPOJOBuilder(withPrefix = "")
  public static class Builder {
    private String id;
    private @Nullable String displayName;
    private String description;
    private Map<String, String> properties;
    private OffsetDateTime created;
    private String createdBy;
    private OffsetDateTime lastModified;
    private String lastModifiedBy;
    private boolean isDeleted;

    public Builder id(String id) {
      this.id = id;
      return this;
    }

    public Builder displayName(@Nullable String displayName) {
      this.displayName = displayName;
      return this;
    }

    public Builder description(@Nullable String description) {
      this.description = description;
      return this;
    }

    public Builder properties(Map<String, String> properties) {
      this.properties = properties;
      return this;
    }

    public void addProperty(String key, String value) {
      if (properties == null) {
        properties = new HashMap<>();
      }
      properties.put(key, value);
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

    public Study build() {
      // true if the id is empty or null
      if (StringUtils.isEmpty(id)) {
        id = ArtifactUtils.newId();
      }
      // Always have a map, even if it is empty
      if (properties == null) {
        properties = new HashMap<>();
      }
      if (displayName != null && displayName.length() > MAX_DISPLAY_NAME_LENGTH) {
        throw new BadRequestException(
            "Study name cannot be greater than " + MAX_DISPLAY_NAME_LENGTH + " characters");
      }
      if (lastModifiedBy == null) {
        lastModifiedBy = createdBy;
      }
      return new Study(this);
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

    public String getCreatedBy() {
      return createdBy;
    }

    public Map<String, String> getProperties() {
      return properties == null ? null : Collections.unmodifiableMap(properties);
    }
  }
}

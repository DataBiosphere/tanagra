package bio.terra.tanagra.service.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * Internal representation of a Study.
 *
 * <p>A study is a collection of cohorts, concept sets, datasets, and cohort reviews.
 */
@JsonDeserialize(builder = Study.Builder.class)
public class Study {
  private final String id;
  private final @Nullable String displayName;
  private final @Nullable String description;
  private final Map<String, String> properties;
  private final OffsetDateTime created;
  private final String createdBy;
  private final OffsetDateTime lastModified;
  private final String lastModifiedBy;

  public Study(
      String id,
      @Nullable String displayName,
      @Nullable String description,
      Map<String, String> properties,
      OffsetDateTime created,
      String createdBy,
      OffsetDateTime lastModified,
      String lastModifiedBy) {
    this.id = id;
    this.displayName = displayName;
    this.description = description;
    this.properties = properties;
    this.created = created;
    this.createdBy = createdBy;
    this.lastModified = lastModified;
    this.lastModifiedBy = lastModifiedBy;
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

    public Study build() {
      // Always have a map, even if it is empty
      if (properties == null) {
        properties = new HashMap<>();
      }
      if (id == null) {
        id = RandomStringUtils.randomAlphanumeric(10);
      }
      return new Study(
          id,
          displayName,
          description,
          properties,
          created,
          createdBy,
          lastModified,
          lastModifiedBy);
    }

    public String getId() {
      return id;
    }
  }
}

package bio.terra.tanagra.service.artifact;

import bio.terra.tanagra.exception.SystemException;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * Internal representation of a Study.
 *
 * <p>A study is a collection of cohorts, concept sets, datasets, and cohort reviews.
 */
@JsonDeserialize(builder = Study.Builder.class)
public class Study {
  private final String studyId;
  private final @Nullable String displayName;
  private final @Nullable String description;
  private final Map<String, String> properties;
  private final OffsetDateTime created;
  private final OffsetDateTime lastModified;

  public Study(
      String studyId,
      @Nullable String displayName,
      @Nullable String description,
      Map<String, String> properties,
      OffsetDateTime created,
      OffsetDateTime lastModified) {
    this.studyId = studyId;
    this.displayName = displayName;
    this.description = description;
    this.properties = properties;
    this.created = created;
    this.lastModified = lastModified;
  }

  /** The globally unique identifier of this study. */
  public String getStudyId() {
    return studyId;
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

  public OffsetDateTime getLastModified() {
    return lastModified;
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
        .append(studyId, study.studyId)
        .append(displayName, study.displayName)
        .append(description, study.description)
        .append(properties, study.properties)
        .append(created, study.created)
        .append(lastModified, study.lastModified)
        .isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(15, 79)
        .append(studyId)
        .append(displayName)
        .append(description)
        .append(properties)
        .append(created)
        .append(lastModified)
        .toHashCode();
  }

  public static Builder builder() {
    return new Builder();
  }

  @JsonPOJOBuilder(withPrefix = "")
  public static class Builder {
    private String studyId;
    private @Nullable String displayName;
    private String description;
    private Map<String, String> properties;
    private OffsetDateTime created;
    private OffsetDateTime lastModified;

    public Builder studyId(String studyId) {
      this.studyId = studyId;
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

    public Builder lastModified(OffsetDateTime lastModified) {
      this.lastModified = lastModified;
      return this;
    }

    public Study build() {
      // Always have a map, even if it is empty
      if (properties == null) {
        properties = new HashMap<>();
      }
      if (studyId == null) {
        throw new SystemException("Study requires id");
      }
      return new Study(studyId, displayName, description, properties, created, lastModified);
    }
  }
}

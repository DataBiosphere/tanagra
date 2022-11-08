package bio.terra.tanagra.api;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.util.UUID;
import javax.annotation.Nullable;

/**
 * Internal representation of a Cohort.
 *
 * <p>A cohort is a filter on the primary entity.
 */
@JsonDeserialize(builder = Cohort.Builder.class)
public class Cohort {
  private final UUID studyId;
  private final UUID cohortId;
  private final String displayName;
  private final String description;
  private final EntityFilter filter;
  private final String uiData;

  private Cohort(Builder builder) {
    this.studyId = builder.studyId;
    this.cohortId = builder.cohortId;
    this.displayName = builder.displayName;
    this.description = builder.description;
    this.filter = builder.filter;
    this.uiData = builder.uiData;
  }

  /** The globally unique identifier of the study this cohort belongs to. */
  public UUID getStudyId() {
    return studyId;
  }

  /** The globally unique identifier of this cohort. */
  public UUID getCohortId() {
    return cohortId;
  }

  /** Optional display name for the cohort. */
  public String getDisplayName() {
    return displayName;
  }

  /** Optional description of the cohort. */
  public String getDescription() {
    return description;
  }

  /** Filter definition on the primary entity. */
  public EntityFilter getFilter() {
    return filter;
  }

  /** Serialized UI data that represents this cohort definition. */
  public String getUiData() {
    return uiData;
  }

  public static Builder builder() {
    return new Builder();
  }

  @JsonPOJOBuilder(withPrefix = "")
  public static class Builder {
    private UUID studyId;
    private UUID cohortId;
    private @Nullable String displayName;
    private @Nullable String description;
    private EntityFilter filter;
    private String uiData;

    public Builder studyId(UUID studyId) {
      this.studyId = studyId;
      return this;
    }

    public Builder cohortId(UUID cohortId) {
      this.cohortId = cohortId;
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

    public Builder filter(EntityFilter filter) {
      this.filter = filter;
      return this;
    }

    public Builder uiData(String uiData) {
      this.uiData = uiData;
      return this;
    }

    public Cohort build() {
      return new Cohort(this);
    }
  }
}

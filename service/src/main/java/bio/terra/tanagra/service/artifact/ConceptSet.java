package bio.terra.tanagra.service.artifact;

import bio.terra.tanagra.exception.SystemException;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import javax.annotation.Nullable;

public class ConceptSet {
  private final String studyId;
  private final String conceptSetId;
  private final String underlayName;
  private final String entityName;
  private final Timestamp lastModified;
  private final @Nullable String displayName;
  private final @Nullable String description;
  private final Criteria criteria;

  private ConceptSet(Builder builder) {
    this.studyId = builder.studyId;
    this.conceptSetId = builder.conceptSetId;
    this.underlayName = builder.underlayName;
    this.entityName = builder.entityName;
    this.lastModified = builder.lastModified;
    this.displayName = builder.displayName;
    this.description = builder.description;
    this.criteria = builder.criteria;
  }

  public static Builder builder() {
    return new Builder();
  }

  public Builder toBuilder() {
    return builder()
        .studyId(studyId)
        .conceptSetId(conceptSetId)
        .underlayName(underlayName)
        .entityName(entityName)
        .lastModified(lastModified)
        .displayName(displayName)
        .description(description)
        .criteria(criteria);
  }

  /** Globally unique identifier of the study this concept set belongs to. */
  public String getStudyId() {
    return studyId;
  }

  /** Unique (per study) identifier of this concept set. */
  public String getConceptSetId() {
    return conceptSetId;
  }

  /** Globally unique name of the underlay this concept set is pinned to. */
  public String getUnderlayName() {
    return underlayName;
  }

  /** Name of the entity this concept set is pinned to. */
  public String getEntityName() {
    return entityName;
  }

  /** Timestamp of when this concept set was last modified. */
  public OffsetDateTime getLastModifiedUTC() {
    return lastModified.toInstant().atOffset(ZoneOffset.UTC);
  }

  /** Optional display name for the concept set. */
  public String getDisplayName() {
    return displayName;
  }

  /** Optional description for the concept set. */
  public String getDescription() {
    return description;
  }

  /** Criteria that defines the entity filter. */
  public Criteria getCriteria() {
    return criteria;
  }

  public static class Builder {
    private String studyId;
    private String conceptSetId;
    private String underlayName;
    private String entityName;
    private Timestamp lastModified;
    private @Nullable String displayName;
    private @Nullable String description;
    private Criteria criteria;

    public Builder studyId(String studyId) {
      this.studyId = studyId;
      return this;
    }

    public Builder conceptSetId(String conceptSetId) {
      this.conceptSetId = conceptSetId;
      return this;
    }

    public Builder underlayName(String underlayName) {
      this.underlayName = underlayName;
      return this;
    }

    public Builder entityName(String entityName) {
      this.entityName = entityName;
      return this;
    }

    public Builder lastModified(Timestamp lastModified) {
      this.lastModified = (Timestamp) lastModified.clone();
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

    public Builder criteria(Criteria criteria) {
      this.criteria = criteria;
      return this;
    }

    public String getConceptSetId() {
      return conceptSetId;
    }

    public ConceptSet build() {
      if (conceptSetId == null) {
        throw new SystemException("Concept set requires id");
      }
      return new ConceptSet(this);
    }
  }
}

package bio.terra.tanagra.service.model;

import java.time.OffsetDateTime;
import java.util.List;
import javax.annotation.Nullable;

public class ConceptSet {
  private final String conceptSetId;
  private final String underlayName;
  private final String entity;
  private final List<Criteria> criteria;
  private final OffsetDateTime created;
  private final String createdBy;
  private final OffsetDateTime lastModified;
  private final @Nullable String displayName;
  private final @Nullable String description;

  public ConceptSet(
      String conceptSetId,
      String underlayName,
      String entity,
      List<Criteria> criteria,
      OffsetDateTime created,
      String createdBy,
      OffsetDateTime lastModified,
      @Nullable String displayName,
      @Nullable String description) {
    this.conceptSetId = conceptSetId;
    this.underlayName = underlayName;
    this.entity = entity;
    this.criteria = criteria;
    this.created = created;
    this.createdBy = createdBy;
    this.lastModified = lastModified;
    this.displayName = displayName;
    this.description = description;
  }

  public String getConceptSetId() {
    return conceptSetId;
  }

  public String getUnderlayName() {
    return underlayName;
  }

  public String getEntity() {
    return entity;
  }

  public List<Criteria> getCriteria() {
    return criteria;
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

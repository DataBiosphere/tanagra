package bio.terra.tanagra.service.model;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.annotation.Nullable;
import org.apache.commons.lang3.RandomStringUtils;

public class ConceptSet {
  private final String id;
  private final String underlay;
  private final String entity;
  private final List<Criteria> criteria;
  private final @Nullable String displayName;
  private final @Nullable String description;
  private final OffsetDateTime created;
  private final String createdBy;
  private final OffsetDateTime lastModified;
  private final String lastModifiedBy;

  private ConceptSet(Builder builder) {
    this.id = builder.id;
    this.underlay = builder.underlay;
    this.entity = builder.entity;
    this.criteria = builder.criteria;
    this.displayName = builder.displayName;
    this.description = builder.description;
    this.created = builder.created;
    this.createdBy = builder.createdBy;
    this.lastModified = builder.lastModified;
    this.lastModifiedBy = builder.lastModifiedBy;
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

  public static class Builder {
    private String id;
    private String underlay;
    private String entity;
    private List<Criteria> criteria = new ArrayList<>();
    private String displayName;
    private String description;
    private OffsetDateTime created;
    private String createdBy;
    private OffsetDateTime lastModified;
    private String lastModifiedBy;

    public Builder id(String id) {
      this.id = id;
      return this;
    }

    public Builder underlay(String underlay) {
      this.underlay = underlay;
      return this;
    }

    public Builder entity(String entity) {
      this.entity = entity;
      return this;
    }

    public Builder criteria(List<Criteria> criteria) {
      this.criteria = criteria;
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

    public ConceptSet build() {
      if (id == null) {
        id = RandomStringUtils.randomAlphanumeric(10);
      }
      criteria = new ArrayList<>(criteria);
      criteria.sort(Comparator.comparing(Criteria::getId));
      return new ConceptSet(this);
    }

    public String getId() {
      return id;
    }

    public String getUnderlay() {
      return underlay;
    }

    public String getEntity() {
      return entity;
    }

    public void addCriteria(Criteria newCriteria) {
      criteria.add(newCriteria);
    }
  }
}

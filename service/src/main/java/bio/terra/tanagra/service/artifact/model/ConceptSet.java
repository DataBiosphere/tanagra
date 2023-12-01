package bio.terra.tanagra.service.artifact.model;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.apache.commons.lang3.RandomStringUtils;

public class ConceptSet {
  private final String id;
  private final String underlay;
  private final List<Criteria> criteria;
  private final Map<String, List<String>> excludeOutputAttributesPerEntity;
  private final @Nullable String displayName;
  private final @Nullable String description;
  private final OffsetDateTime created;
  private final String createdBy;
  private final OffsetDateTime lastModified;
  private final String lastModifiedBy;
  private final boolean isDeleted;

  private ConceptSet(Builder builder) {
    this.id = builder.id;
    this.underlay = builder.underlay;
    this.criteria = builder.criteria;
    this.excludeOutputAttributesPerEntity = builder.excludeOutputAttributesPerEntity;
    this.displayName = builder.displayName;
    this.description = builder.description;
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

  public String getUnderlay() {
    return underlay;
  }

  public List<Criteria> getCriteria() {
    return criteria;
  }

  public Map<String, List<String>> getExcludeOutputAttributesPerEntity() {
    return excludeOutputAttributesPerEntity;
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

  public boolean isDeleted() {
    return isDeleted;
  }

  public String getDisplayNameOrDefault() {
    if (displayName != null && !displayName.isEmpty()) {
      return displayName;
    } else {
      DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy, hh:mm:ss a");
      return "Untitled " + outputFormatter.format(created);
    }
  }

  public static class Builder {
    private String id;
    private String underlay;
    private List<Criteria> criteria = new ArrayList<>();
    private Map<String, List<String>> excludeOutputAttributesPerEntity = new HashMap<>();
    private String displayName;
    private String description;
    private OffsetDateTime created;
    private String createdBy;
    private OffsetDateTime lastModified;
    private String lastModifiedBy;
    private boolean isDeleted;

    public Builder id(String id) {
      this.id = id;
      return this;
    }

    public Builder underlay(String underlay) {
      this.underlay = underlay;
      return this;
    }

    public Builder criteria(List<Criteria> criteria) {
      this.criteria = criteria;
      return this;
    }

    public Builder excludeOutputAttributesPerEntity(
        Map<String, List<String>> excludeOutputAttributesPerEntity) {
      this.excludeOutputAttributesPerEntity = excludeOutputAttributesPerEntity;
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

    public Builder isDeleted(boolean isDeleted) {
      this.isDeleted = isDeleted;
      return this;
    }

    public ConceptSet build() {
      if (id == null) {
        id = RandomStringUtils.randomAlphanumeric(10);
      }
      criteria = new ArrayList<>(criteria);
      criteria.sort(Comparator.comparing(Criteria::getId));
      excludeOutputAttributesPerEntity =
          excludeOutputAttributesPerEntity.entrySet().stream()
              .collect(
                  Collectors.toMap(
                      entry -> entry.getKey(),
                      entry -> entry.getValue().stream().sorted().collect(Collectors.toList())));
      return new ConceptSet(this);
    }

    public String getId() {
      return id;
    }

    public String getUnderlay() {
      return underlay;
    }

    public Map<String, List<String>> getExcludeOutputAttributesPerEntity() {
      return excludeOutputAttributesPerEntity;
    }

    public void addCriteria(Criteria newCriteria) {
      criteria.add(newCriteria);
    }

    public void addExcludeOutputAttribute(String entity, String attribute) {
      List<String> outputAttributes =
          excludeOutputAttributesPerEntity.containsKey(entity)
              ? excludeOutputAttributesPerEntity.get(entity)
              : new ArrayList<>();
      outputAttributes.add(attribute);
      excludeOutputAttributesPerEntity.put(entity, outputAttributes);
    }
  }
}

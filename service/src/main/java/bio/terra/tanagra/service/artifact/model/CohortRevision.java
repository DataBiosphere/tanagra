package bio.terra.tanagra.service.artifact.model;

import bio.terra.common.exception.NotFoundException;
import bio.terra.tanagra.api.filter.BooleanAndOrFilter;
import bio.terra.tanagra.api.shared.*;
import jakarta.annotation.Nullable;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.*;
import org.apache.commons.lang3.RandomStringUtils;

@SuppressWarnings("PMD.ExcessivePublicCount")
public final class CohortRevision {
  private final String id;
  private final List<CriteriaGroupSection> sections;
  private final int version;
  private final boolean isMostRecent;
  private final boolean isEditable;
  private final OffsetDateTime created;
  private final String createdBy;
  private final OffsetDateTime lastModified;
  private final String lastModifiedBy;
  private final Long recordsCount;

  private CohortRevision(Builder builder) {
    this.id = builder.id;
    this.sections = builder.sections;
    this.version = builder.version;
    this.isMostRecent = builder.isMostRecent;
    this.isEditable = builder.isEditable;
    this.created = builder.created;
    this.createdBy = builder.createdBy;
    this.lastModified = builder.lastModified;
    this.lastModifiedBy = builder.lastModifiedBy;
    this.recordsCount = builder.recordsCount;
  }

  public static Builder builder() {
    return new Builder();
  }

  public Builder toBuilder() {
    return builder()
        .id(id)
        .sections(sections)
        .version(version)
        .setIsMostRecent(isMostRecent)
        .setIsEditable(isEditable)
        .created(created)
        .createdBy(createdBy)
        .lastModified(lastModified)
        .lastModifiedBy(lastModifiedBy)
        .recordsCount(recordsCount);
  }

  public String getId() {
    return id;
  }

  public List<CriteriaGroupSection> getSections() {
    return Collections.unmodifiableList(sections);
  }

  public CriteriaGroupSection getSection(String id) {
    return sections.stream()
        .filter(section -> id.equals(section.getId()))
        .findFirst()
        .orElseThrow(() -> new NotFoundException("Criteria group section not found for id: " + id));
  }

  public int getVersion() {
    return version;
  }

  public boolean isMostRecent() {
    return isMostRecent;
  }

  public boolean isEditable() {
    return isEditable;
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

  public Long getRecordsCount() {
    return recordsCount;
  }

  public static class Builder {
    private String id;
    private List<CriteriaGroupSection> sections = new ArrayList<>();
    private int version;
    private boolean isMostRecent;
    private boolean isEditable;
    private OffsetDateTime created;
    private String createdBy;
    private OffsetDateTime lastModified;
    private String lastModifiedBy;
    private Long recordsCount;

    public Builder id(String id) {
      this.id = id;
      return this;
    }

    public Builder sections(List<CriteriaGroupSection> sections) {
      this.sections = sections;
      return this;
    }

    public Builder version(int version) {
      this.version = version;
      return this;
    }

    public Builder setIsMostRecent(boolean isMostRecent) {
      this.isMostRecent = isMostRecent;
      return this;
    }

    public Builder setIsEditable(boolean isEditable) {
      this.isEditable = isEditable;
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

    public Builder recordsCount(Long recordsCount) {
      this.recordsCount = recordsCount;
      return this;
    }

    public CohortRevision build() {
      if (id == null) {
        id = RandomStringUtils.randomAlphanumeric(10);
      }
      return new CohortRevision(this);
    }

    public String getId() {
      return id;
    }

    public void addCriteriaGroupSection(CriteriaGroupSection criteriaGroupSection) {
      if (sections == null) {
        sections = new ArrayList<>();
      }
      sections.add(criteriaGroupSection);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CohortRevision that = (CohortRevision) o;
    return version == that.version
        && isMostRecent == that.isMostRecent
        && isEditable == that.isEditable
        && id.equals(that.id)
        && sections.equals(that.sections)
        && created.equals(that.created)
        && createdBy.equals(that.createdBy)
        && lastModified.equals(that.lastModified)
        && lastModifiedBy.equals(that.lastModifiedBy)
        && recordsCount.equals(that.recordsCount);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        id,
        sections,
        version,
        isMostRecent,
        isEditable,
        created,
        createdBy,
        lastModified,
        lastModifiedBy,
        recordsCount);
  }

  public static class CriteriaGroupSection {
    private final String id;
    private final @Nullable String displayName;
    private final List<CriteriaGroup> criteriaGroups;
    private final List<CriteriaGroup> secondConditionCriteriaGroups;
    private final BooleanAndOrFilter.LogicalOperator operator;
    private final ReducingOperator firstConditionReducingOperator;
    private final ReducingOperator secondConditionRedcuingOperator;
    private final JoinOperator joinOperator;
    private final Integer joinOperatorValue;
    private final boolean isExcluded;

    @SuppressWarnings("checkstyle:ParameterNumber")
    private CriteriaGroupSection(
        String id,
        String displayName,
        List<CriteriaGroup> criteriaGroups,
        List<CriteriaGroup> secondConditionCriteriaGroups,
        BooleanAndOrFilter.LogicalOperator operator,
        ReducingOperator firstConditionReducingOperator,
        ReducingOperator secondConditionRedcuingOperator,
        JoinOperator joinOperator,
        Integer joinOperatorValue,
        boolean isExcluded) {
      this.id = id;
      this.displayName = displayName;
      this.criteriaGroups = criteriaGroups;
      this.secondConditionCriteriaGroups = secondConditionCriteriaGroups;
      this.operator = operator;
      this.firstConditionReducingOperator = firstConditionReducingOperator;
      this.secondConditionRedcuingOperator = secondConditionRedcuingOperator;
      this.joinOperator = joinOperator;
      this.joinOperatorValue = joinOperatorValue;
      this.isExcluded = isExcluded;
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

    public List<CriteriaGroup> getCriteriaGroups() {
      return Collections.unmodifiableList(criteriaGroups);
    }

    public List<CriteriaGroup> getSecondConditionCriteriaGroups() {
      return Collections.unmodifiableList(secondConditionCriteriaGroups);
    }

    public CriteriaGroup getCriteriaGroup(String id) {
      return Stream.concat(criteriaGroups.stream(), secondConditionCriteriaGroups.stream())
          .filter(group -> id.equals(group.getId()))
          .findFirst()
          .orElseThrow(() -> new NotFoundException("Criteria group not found for id: " + id));
    }

    public BooleanAndOrFilter.LogicalOperator getOperator() {
      return operator;
    }

    public ReducingOperator getFirstConditionReducingOperator() {
      return firstConditionReducingOperator;
    }

    public ReducingOperator getSecondConditionRedcuingOperator() {
      return secondConditionRedcuingOperator;
    }

    public JoinOperator getJoinOperator() {
      return joinOperator;
    }

    public Integer getJoinOperatorValue() {
      return joinOperatorValue;
    }

    public boolean isExcluded() {
      return isExcluded;
    }

    public static class Builder {
      private String id;
      private String displayName;
      private List<CriteriaGroup> criteriaGroups = new ArrayList<>();
      private List<CriteriaGroup> secondConditionCriteriaGroups = new ArrayList<>();
      private BooleanAndOrFilter.LogicalOperator operator = BooleanAndOrFilter.LogicalOperator.OR;
      private ReducingOperator firstConditionReducingOperator;
      private ReducingOperator secondConditionReducingOperator;
      private JoinOperator joinOperator;
      private Integer joinOperatorValue;
      private boolean isExcluded;

      public Builder id(String id) {
        this.id = id;
        return this;
      }

      public Builder displayName(String displayName) {
        this.displayName = displayName;
        return this;
      }

      public Builder criteriaGroups(List<CriteriaGroup> criteriaGroups) {
        this.criteriaGroups = criteriaGroups;
        return this;
      }

      public Builder secondConditionCriteriaGroups(
          List<CriteriaGroup> secondConditionCriteriaGroups) {
        this.secondConditionCriteriaGroups = secondConditionCriteriaGroups;
        return this;
      }

      public Builder operator(BooleanAndOrFilter.LogicalOperator operator) {
        this.operator = operator;
        return this;
      }

      public Builder firstConditionReducingOperator(
          ReducingOperator firstConditionReducingOperator) {
        this.firstConditionReducingOperator = firstConditionReducingOperator;
        return this;
      }

      public Builder secondConditionReducingOperator(
          ReducingOperator secondConditionReducingOperator) {
        this.secondConditionReducingOperator = secondConditionReducingOperator;
        return this;
      }

      public Builder joinOperator(JoinOperator joinOperator) {
        this.joinOperator = joinOperator;
        return this;
      }

      public Builder joinOperatorValue(Integer joinOperatorValue) {
        this.joinOperatorValue = joinOperatorValue;
        return this;
      }

      public Builder setIsExcluded(boolean excluded) {
        this.isExcluded = excluded;
        return this;
      }

      public CriteriaGroupSection build() {
        if (id == null) {
          id = RandomStringUtils.randomAlphanumeric(10);
        }
        return new CriteriaGroupSection(
            id,
            displayName,
            criteriaGroups,
            secondConditionCriteriaGroups,
            operator,
            firstConditionReducingOperator,
            secondConditionReducingOperator,
            joinOperator,
            joinOperatorValue,
            isExcluded);
      }

      public String getId() {
        return id;
      }

      public void addCriteriaGroup(CriteriaGroup criteriaGroup) {
        criteriaGroups.add(criteriaGroup);
      }

      public void addSecondConditionCriteriaGroup(CriteriaGroup criteriaGroup) {
        secondConditionCriteriaGroups.add(criteriaGroup);
      }
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      CriteriaGroupSection that = (CriteriaGroupSection) o;
      return isExcluded == that.isExcluded
          && id.equals(that.id)
          && Objects.equals(displayName, that.displayName)
          && criteriaGroups.equals(that.criteriaGroups)
          && secondConditionCriteriaGroups.equals(that.secondConditionCriteriaGroups)
          && operator == that.operator
          && firstConditionReducingOperator == that.firstConditionReducingOperator
          && secondConditionRedcuingOperator == that.secondConditionRedcuingOperator
          && joinOperator == that.joinOperator
          && Objects.equals(joinOperatorValue, that.joinOperatorValue);
    }

    @Override
    public int hashCode() {
      return Objects.hash(
          id,
          displayName,
          criteriaGroups,
          secondConditionCriteriaGroups,
          operator,
          firstConditionReducingOperator,
          secondConditionRedcuingOperator,
          joinOperator,
          joinOperatorValue,
          isExcluded);
    }
  }

  public static class CriteriaGroup {
    private final String id;
    private final String displayName;
    private final List<Criteria> criteria;

    private CriteriaGroup(String id, String displayName, List<Criteria> criteria) {
      this.id = id;
      this.displayName = displayName;
      this.criteria = criteria;
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

    public List<Criteria> getCriteria() {
      return Collections.unmodifiableList(criteria);
    }

    public static class Builder {
      private String id;
      private String displayName;
      private List<Criteria> criteria = new ArrayList<>();

      public Builder id(String id) {
        this.id = id;
        return this;
      }

      public Builder displayName(String displayName) {
        this.displayName = displayName;
        return this;
      }

      public Builder criteria(List<Criteria> criteria) {
        this.criteria = criteria;
        return this;
      }

      public CriteriaGroup build() {
        if (id == null) {
          id = RandomStringUtils.randomAlphanumeric(10);
        }
        return new CriteriaGroup(id, displayName, criteria);
      }

      public String getId() {
        return id;
      }

      public void addCriteria(Criteria newCriteria) {
        criteria.add(newCriteria);
      }
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      CriteriaGroup that = (CriteriaGroup) o;
      return id.equals(that.id)
          && displayName.equals(that.displayName)
          && criteria.equals(that.criteria);
    }

    @Override
    public int hashCode() {
      return Objects.hash(id, displayName, criteria);
    }
  }
}

package bio.terra.tanagra.service.model;

import bio.terra.tanagra.query.filtervariable.BinaryFilterVariable;
import bio.terra.tanagra.query.filtervariable.BooleanAndOrFilterVariable;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import org.apache.commons.lang3.RandomStringUtils;

public class CohortRevision {
  private final String id;
  private final List<CriteriaGroupSection> sections;
  private final int version;
  private final boolean isMostRecent;
  private final boolean isEditable;
  private final OffsetDateTime created;
  private final String createdBy;
  private final OffsetDateTime lastModified;
  private final String lastModifiedBy;

  public CohortRevision(
      String id,
      List<CriteriaGroupSection> sections,
      int version,
      boolean isMostRecent,
      boolean isEditable,
      OffsetDateTime created,
      String createdBy,
      OffsetDateTime lastModified,
      String lastModifiedBy) {
    this.id = id;
    this.sections = sections;
    this.version = version;
    this.isMostRecent = isMostRecent;
    this.isEditable = isEditable;
    this.created = created;
    this.createdBy = createdBy;
    this.lastModified = lastModified;
    this.lastModifiedBy = lastModifiedBy;
  }

  public static Builder builder() {
    return new Builder();
  }

  public String getId() {
    return id;
  }

  public List<CriteriaGroupSection> getSections() {
    return Collections.unmodifiableList(sections);
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

  public static class Builder {
    private String id;
    private List<CriteriaGroupSection> sections;
    private int version;
    private boolean isMostRecent;
    private boolean isEditable;
    private OffsetDateTime created;
    private String createdBy;
    private OffsetDateTime lastModified;
    private String lastModifiedBy;

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

    public CohortRevision build() {
      if (id == null) {
        id = RandomStringUtils.randomAlphanumeric(10);
      }
      return new CohortRevision(
          id,
          sections,
          version,
          isMostRecent,
          isEditable,
          created,
          createdBy,
          lastModified,
          lastModifiedBy);
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

  public static class CriteriaGroupSection {
    private final String id;
    private final @Nullable String displayName;
    private final List<CriteriaGroup> criteriaGroups;
    private final BooleanAndOrFilterVariable.LogicalOperator operator;
    private final boolean isExcluded;

    public CriteriaGroupSection(
        String id,
        String displayName,
        List<CriteriaGroup> criteriaGroups,
        BooleanAndOrFilterVariable.LogicalOperator operator,
        boolean isExcluded) {
      this.id = id;
      this.displayName = displayName;
      this.criteriaGroups = criteriaGroups;
      this.operator = operator;
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

    public BooleanAndOrFilterVariable.LogicalOperator getOperator() {
      return operator;
    }

    public boolean isExcluded() {
      return isExcluded;
    }

    public static class Builder {
      private String id;
      private String displayName;
      private List<CriteriaGroup> criteriaGroups;
      private BooleanAndOrFilterVariable.LogicalOperator operator;
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

      public Builder operator(BooleanAndOrFilterVariable.LogicalOperator operator) {
        this.operator = operator;
        return this;
      }

      public Builder setIsExcluded(boolean excluded) {
        this.isExcluded = excluded;
        return this;
      }

      public CriteriaGroupSection build() {
        return new CriteriaGroupSection(id, displayName, criteriaGroups, operator, isExcluded);
      }

      public String getId() {
        return id;
      }

      public void addCriteriaGroup(CriteriaGroup criteriaGroup) {
        if (criteriaGroups == null) {
          criteriaGroups = new ArrayList<>();
        }
        criteriaGroups.add(criteriaGroup);
      }
    }
  }

  public static class CriteriaGroup {
    private final String id;
    private final @Nullable String displayName;
    private final List<Criteria> criteria;
    private final String entity;
    private final @Nullable BinaryFilterVariable.BinaryOperator groupByCountOperator;
    private final @Nullable Integer groupByCountValue;

    public CriteriaGroup(
        String id,
        String displayName,
        List<Criteria> criteria,
        String entity,
        BinaryFilterVariable.BinaryOperator groupByCountOperator,
        Integer groupByCountValue) {
      this.id = id;
      this.displayName = displayName;
      this.criteria = criteria;
      this.entity = entity;
      this.groupByCountOperator = groupByCountOperator;
      this.groupByCountValue = groupByCountValue;
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

    public String getEntity() {
      return entity;
    }

    public BinaryFilterVariable.BinaryOperator getGroupByCountOperator() {
      return groupByCountOperator;
    }

    public Integer getGroupByCountValue() {
      return groupByCountValue;
    }

    public static class Builder {
      private String id;
      private String displayName;
      private List<Criteria> criteria;
      private String entity;
      private BinaryFilterVariable.BinaryOperator groupByCountOperator;
      private Integer groupByCountValue;

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

      public Builder entity(String entity) {
        this.entity = entity;
        return this;
      }

      public Builder groupByCountOperator(
          BinaryFilterVariable.BinaryOperator groupByCountOperator) {
        this.groupByCountOperator = groupByCountOperator;
        return this;
      }

      public Builder groupByCountValue(Integer groupByCountValue) {
        this.groupByCountValue = groupByCountValue;
        return this;
      }

      public CriteriaGroup build() {
        return new CriteriaGroup(
            id, displayName, criteria, entity, groupByCountOperator, groupByCountValue);
      }

      public String getId() {
        return id;
      }

      public void addCriteria(Criteria newCriteria) {
        if (criteria == null) {
          criteria = new ArrayList<>();
        }
        criteria.add(newCriteria);
      }
    }
  }
}

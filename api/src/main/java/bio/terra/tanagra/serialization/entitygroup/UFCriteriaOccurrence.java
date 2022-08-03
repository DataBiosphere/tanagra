package bio.terra.tanagra.serialization.entitygroup;

import bio.terra.tanagra.serialization.UFEntityGroup;
import bio.terra.tanagra.serialization.UFRelationshipMapping;
import bio.terra.tanagra.serialization.UFTablePointer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

/**
 * External representation of a criteria occurrence entity group (e.g.
 * condition-condition_occurrence-person).
 *
 * <p>This is a POJO class intended for serialization. This JSON format is user-facing.
 */
@JsonDeserialize(builder = UFCriteriaOccurrence.Builder.class)
public class UFCriteriaOccurrence extends UFEntityGroup {
  private final String criteriaEntity;
  private final String occurrenceEntity;
  private final UFRelationshipMapping occurrenceCriteriaRelationship;
  private final UFRelationshipMapping occurrencePrimaryRelationship;
  private final UFTablePointer staticCountTablePointer;

  /** Constructor for Jackson deserialization during testing. */
  private UFCriteriaOccurrence(Builder builder) {
    super(builder);
    this.criteriaEntity = builder.criteriaEntity;
    this.occurrenceEntity = builder.occurrenceEntity;
    this.occurrenceCriteriaRelationship = builder.occurrenceCriteriaRelationship;
    this.occurrencePrimaryRelationship = builder.occurrencePrimaryRelationship;
    this.staticCountTablePointer = builder.staticCountTablePointer;
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder extends UFEntityGroup.Builder {
    private String criteriaEntity;
    private String occurrenceEntity;
    private UFRelationshipMapping occurrenceCriteriaRelationship;
    private UFRelationshipMapping occurrencePrimaryRelationship;
    private UFTablePointer staticCountTablePointer;

    public Builder criteriaEntity(String criteriaEntity) {
      this.criteriaEntity = criteriaEntity;
      return this;
    }

    public Builder occurrenceEntity(String occurrenceEntity) {
      this.occurrenceEntity = occurrenceEntity;
      return this;
    }

    public Builder occurrenceCriteriaRelationship(
        UFRelationshipMapping occurrenceCriteriaRelationship) {
      this.occurrenceCriteriaRelationship = occurrenceCriteriaRelationship;
      return this;
    }

    public Builder occurrencePrimaryRelationship(
        UFRelationshipMapping occurrencePrimaryRelationship) {
      this.occurrencePrimaryRelationship = occurrencePrimaryRelationship;
      return this;
    }

    public Builder staticCountTablePointer(UFTablePointer staticCountTablePointer) {
      this.staticCountTablePointer = staticCountTablePointer;
      return this;
    }

    /** Call the private constructor. */
    public UFCriteriaOccurrence build() {
      return new UFCriteriaOccurrence(this);
    }

    /** Default constructor for Jackson. */
    public Builder() {}
  }

  public String getCriteriaEntity() {
    return criteriaEntity;
  }

  public String getOccurrenceEntity() {
    return occurrenceEntity;
  }

  public UFRelationshipMapping getOccurrenceCriteriaRelationship() {
    return occurrenceCriteriaRelationship;
  }

  public UFRelationshipMapping getOccurrencePrimaryRelationship() {
    return occurrencePrimaryRelationship;
  }

  public UFTablePointer getStaticCountTablePointer() {
    return staticCountTablePointer;
  }
}

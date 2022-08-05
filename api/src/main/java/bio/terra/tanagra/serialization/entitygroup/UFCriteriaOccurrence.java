package bio.terra.tanagra.serialization.entitygroup;

import bio.terra.tanagra.serialization.UFEntityGroup;
import bio.terra.tanagra.serialization.UFRelationshipMapping;
import bio.terra.tanagra.underlay.DataPointer;
import bio.terra.tanagra.underlay.Entity;
import bio.terra.tanagra.underlay.entitygroup.CriteriaOccurrence;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.util.Map;

/**
 * External representation of a criteria-occurrence entity group (e.g.
 * condition-person-condition_occurrence).
 *
 * <p>This is a POJO class intended for serialization. This JSON format is user-facing.
 */
@JsonDeserialize(builder = UFCriteriaOccurrence.Builder.class)
public class UFCriteriaOccurrence extends UFEntityGroup {
  private final String criteriaEntity;
  private final String occurrenceEntity;
  private final UFRelationshipMapping occurrenceToCriteriaRelationship;
  private final UFRelationshipMapping occurrenceToPrimaryRelationship;
  private final String rollupCountTablePointer;

  public UFCriteriaOccurrence(CriteriaOccurrence entityGroup) {
    super(entityGroup);
    this.criteriaEntity = entityGroup.getCriteriaEntity().getName();
    this.occurrenceEntity = entityGroup.getOccurrenceEntity().getName();
    this.occurrenceToCriteriaRelationship =
        entityGroup.getOccurrenceToCriteriaRelationship().serialize();
    this.occurrenceToPrimaryRelationship =
        entityGroup.getOccurrenceToPrimaryRelationship().serialize();
    this.rollupCountTablePointer = entityGroup.getRollupCountTablePointer().getTableName();
  }

  private UFCriteriaOccurrence(Builder builder) {
    super(builder);
    this.criteriaEntity = builder.criteriaEntity;
    this.occurrenceEntity = builder.occurrenceEntity;
    this.occurrenceToCriteriaRelationship = builder.occurrenceToCriteriaRelationship;
    this.occurrenceToPrimaryRelationship = builder.occurrenceToPrimaryRelationship;
    this.rollupCountTablePointer = builder.rollupCountTablePointer;
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder extends UFEntityGroup.Builder {
    private String criteriaEntity;
    private String occurrenceEntity;
    private UFRelationshipMapping occurrenceToCriteriaRelationship;
    private UFRelationshipMapping occurrenceToPrimaryRelationship;
    private String rollupCountTablePointer;

    public Builder criteriaEntity(String criteriaEntity) {
      this.criteriaEntity = criteriaEntity;
      return this;
    }

    public Builder occurrenceEntity(String occurrenceEntity) {
      this.occurrenceEntity = occurrenceEntity;
      return this;
    }

    public Builder occurrenceToCriteriaRelationship(
        UFRelationshipMapping occurrenceToCriteriaRelationship) {
      this.occurrenceToCriteriaRelationship = occurrenceToCriteriaRelationship;
      return this;
    }

    public Builder occurrenceToPrimaryRelationship(
        UFRelationshipMapping occurrenceToPrimaryRelationship) {
      this.occurrenceToPrimaryRelationship = occurrenceToPrimaryRelationship;
      return this;
    }

    public Builder rollupCountTablePointer(String rollupCountTablePointer) {
      this.rollupCountTablePointer = rollupCountTablePointer;
      return this;
    }

    /** Call the private constructor. */
    public UFCriteriaOccurrence build() {
      return new UFCriteriaOccurrence(this);
    }

    /** Default constructor for Jackson. */
    public Builder() {}
  }

  @Override
  public CriteriaOccurrence deserializeToInternal(
      Map<String, DataPointer> dataPointers,
      Map<String, Entity> entities,
      String primaryEntityName) {
    return CriteriaOccurrence.fromSerialized(this, dataPointers, entities, primaryEntityName);
  }

  public String getCriteriaEntity() {
    return criteriaEntity;
  }

  public String getOccurrenceEntity() {
    return occurrenceEntity;
  }

  public UFRelationshipMapping getOccurrenceToCriteriaRelationship() {
    return occurrenceToCriteriaRelationship;
  }

  public UFRelationshipMapping getOccurrenceToPrimaryRelationship() {
    return occurrenceToPrimaryRelationship;
  }

  public String getRollupCountTablePointer() {
    return rollupCountTablePointer;
  }
}

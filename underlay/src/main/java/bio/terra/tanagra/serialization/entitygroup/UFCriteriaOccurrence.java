package bio.terra.tanagra.serialization.entitygroup;

import bio.terra.tanagra.query.DataPointer;
import bio.terra.tanagra.serialization.UFEntityGroup;
import bio.terra.tanagra.underlay.Attribute;
import bio.terra.tanagra.underlay.Entity;
import bio.terra.tanagra.underlay.entitygroup.CriteriaOccurrence;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * External representation of a criteria occurrence entity group.
 *
 * <p>This is a POJO class intended for serialization. This JSON format is user-facing.
 */
@JsonDeserialize(builder = UFCriteriaOccurrence.Builder.class)
public class UFCriteriaOccurrence extends UFEntityGroup {
  private final String criteriaEntity;
  private final List<String> occurrenceEntities;
  private final List<String> modifierAttributes;

  public UFCriteriaOccurrence(CriteriaOccurrence criteriaOccurrence) {
    super(criteriaOccurrence);
    this.criteriaEntity = criteriaOccurrence.getCriteriaEntity().getName();
    this.occurrenceEntities =
        criteriaOccurrence.getOccurrenceEntities().stream()
            .map(e -> e.getName())
            .collect(Collectors.toList());
    this.modifierAttributes =
        criteriaOccurrence.getModifierAttributes().stream()
            .map(Attribute::getName)
            .collect(Collectors.toList());
  }

  private UFCriteriaOccurrence(Builder builder) {
    super(builder);
    this.criteriaEntity = builder.criteriaEntity;
    this.occurrenceEntities = builder.occurrenceEntities;
    this.modifierAttributes = builder.modifierAttributes;
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder extends UFEntityGroup.Builder {
    private String criteriaEntity;
    private List<String> occurrenceEntities;
    private List<String> modifierAttributes;

    public Builder criteriaEntity(String criteriaEntity) {
      this.criteriaEntity = criteriaEntity;
      return this;
    }

    public Builder occurrenceEntities(List<String> occurrenceEntities) {
      this.occurrenceEntities = occurrenceEntities;
      return this;
    }

    public Builder modifierAttributes(List<String> modifierAttributes) {
      this.modifierAttributes = modifierAttributes;
      return this;
    }

    /** Call the private constructor. */
    @Override
    public UFCriteriaOccurrence build() {
      return new UFCriteriaOccurrence(this);
    }
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

  public List<String> getOccurrenceEntities() {
    return Collections.unmodifiableList(occurrenceEntities);
  }

  public List<String> getModifierAttributes() {
    return modifierAttributes;
  }
}

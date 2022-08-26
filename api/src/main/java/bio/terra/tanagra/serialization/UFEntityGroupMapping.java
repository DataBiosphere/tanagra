package bio.terra.tanagra.serialization;

import bio.terra.tanagra.underlay.EntityGroupMapping;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.util.HashMap;
import java.util.Map;

/**
 * External representation of the data mapped to an entity group.
 *
 * <p>This is a POJO class intended for serialization. This JSON format is user-facing.
 */
@JsonDeserialize(builder = UFEntityGroupMapping.Builder.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UFEntityGroupMapping {
  private final String dataPointer;
  private final Map<String, UFRelationshipMapping> relationshipMappings;
  private final Map<String, UFAuxiliaryDataMapping> auxiliaryDataMappings;

  public UFEntityGroupMapping(EntityGroupMapping entityGroupMapping) {
    this.dataPointer = entityGroupMapping.getDataPointer().getName();

    Map<String, UFRelationshipMapping> relationshipMappings = new HashMap<>();
    entityGroupMapping.getRelationshipMappings().entrySet().stream()
        .forEach(
            rMpg -> {
              relationshipMappings.put(rMpg.getKey(), new UFRelationshipMapping(rMpg.getValue()));
            });
    this.relationshipMappings = relationshipMappings;

    Map<String, UFAuxiliaryDataMapping> auxiliaryDataMappings = new HashMap<>();
    entityGroupMapping.getAuxiliaryDataMappings().entrySet().stream()
        .forEach(
            adMpg -> {
              auxiliaryDataMappings.put(
                  adMpg.getKey(), new UFAuxiliaryDataMapping(adMpg.getValue()));
            });
    this.auxiliaryDataMappings = auxiliaryDataMappings;
  }

  private UFEntityGroupMapping(Builder builder) {
    this.dataPointer = builder.dataPointer;
    this.relationshipMappings = builder.relationshipMappings;
    this.auxiliaryDataMappings = builder.auxiliaryDataMappings;
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {
    private String dataPointer;
    private Map<String, UFRelationshipMapping> relationshipMappings;
    private Map<String, UFAuxiliaryDataMapping> auxiliaryDataMappings;

    public Builder dataPointer(String dataPointer) {
      this.dataPointer = dataPointer;
      return this;
    }

    public Builder relationshipMappings(Map<String, UFRelationshipMapping> relationshipMappings) {
      this.relationshipMappings = relationshipMappings;
      return this;
    }

    public Builder auxiliaryDataMappings(
        Map<String, UFAuxiliaryDataMapping> auxiliaryDataMappings) {
      this.auxiliaryDataMappings = auxiliaryDataMappings;
      return this;
    }

    /** Call the private constructor. */
    public UFEntityGroupMapping build() {
      return new UFEntityGroupMapping(this);
    }

    /** Default constructor for Jackson. */
    public Builder() {}
  }

  public String getDataPointer() {
    return dataPointer;
  }

  public Map<String, UFRelationshipMapping> getRelationshipMappings() {
    return relationshipMappings;
  }

  public Map<String, UFAuxiliaryDataMapping> getAuxiliaryDataMappings() {
    return auxiliaryDataMappings;
  }
}

package bio.terra.tanagra.serialization;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.util.Map;

/**
 * External representation of the source data mapped to an entity.
 *
 * <p>This is a POJO class intended for serialization. This JSON format is user-facing.
 */
@JsonDeserialize(builder = UFEntityMapping.Builder.class)
public class UFEntityMapping {
  public final String dataPointer;
  public final UFTablePointer tablePointer;
  public final Map<String, UFAttributeMapping> attributeMappings;
  public final UFTextSearchMapping textSearchMapping;

  /** Constructor for Jackson deserialization during testing. */
  private UFEntityMapping(Builder builder) {
    this.dataPointer = builder.dataPointer;
    this.tablePointer = builder.tablePointer;
    this.attributeMappings = builder.attributeMappings;
    this.textSearchMapping = builder.textSearchMapping;
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {
    private String dataPointer;
    private UFTablePointer tablePointer;
    private Map<String, UFAttributeMapping> attributeMappings;
    private UFTextSearchMapping textSearchMapping;

    public Builder dataPointer(String dataPointer) {
      this.dataPointer = dataPointer;
      return this;
    }

    public Builder tablePointer(UFTablePointer tablePointer) {
      this.tablePointer = tablePointer;
      return this;
    }

    public Builder attributeMappings(Map<String, UFAttributeMapping> attributeMappings) {
      this.attributeMappings = attributeMappings;
      return this;
    }

    public Builder textSearchMapping(UFTextSearchMapping textSearchMapping) {
      this.textSearchMapping = textSearchMapping;
      return this;
    }

    /** Call the private constructor. */
    public UFEntityMapping build() {
      return new UFEntityMapping(this);
    }

    /** Default constructor for Jackson. */
    public Builder() {}
  }
}

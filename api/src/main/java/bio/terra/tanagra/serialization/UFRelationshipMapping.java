package bio.terra.tanagra.serialization;

import bio.terra.tanagra.serialization.relationshipmapping.UFForeignKey;
import bio.terra.tanagra.serialization.relationshipmapping.UFIntermediateTable;
import bio.terra.tanagra.underlay.Relationship;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

/**
 * External representation of a data pointer configuration.
 *
 * <p>This is a POJO class intended for serialization. This JSON format is user-facing.
 */
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "type")
@JsonSubTypes({
  @JsonSubTypes.Type(value = UFForeignKey.class, name = "FOREIGN_KEY"),
  @JsonSubTypes.Type(value = UFIntermediateTable.class, name = "INTERMEDIATE_TABLE")
})
@JsonDeserialize(builder = UFRelationshipMapping.Builder.class)
public abstract class UFRelationshipMapping {
  private final Relationship.Type type;
  private final String name;

  /** Constructor for Jackson deserialization during testing. */
  protected UFRelationshipMapping(Builder builder) {
    this.type = builder.type;
    this.name = builder.name;
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public abstract static class Builder {
    private Relationship.Type type;
    private String name;

    public Builder type(Relationship.Type type) {
      this.type = type;
      return this;
    }

    public Builder name(String name) {
      this.name = name;
      return this;
    }

    /** Call the private constructor. */
    public abstract UFRelationshipMapping build();

    /** Default constructor for Jackson. */
    public Builder() {}
  }

  public Relationship.Type getType() {
    return type;
  }

  public String getName() {
    return name;
  }
}

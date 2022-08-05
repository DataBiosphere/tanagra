package bio.terra.tanagra.serialization;

import bio.terra.tanagra.serialization.relationshipmapping.UFForeignKey;
import bio.terra.tanagra.serialization.relationshipmapping.UFIntermediateTable;
import bio.terra.tanagra.underlay.DataPointer;
import bio.terra.tanagra.underlay.Entity;
import bio.terra.tanagra.underlay.RelationshipMapping;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.util.Map;

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
  private final RelationshipMapping.Type type;

  protected UFRelationshipMapping(RelationshipMapping relationshipMapping) {
    this.type = relationshipMapping.getType();
  }

  protected UFRelationshipMapping(Builder builder) {
    this.type = builder.type;
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public abstract static class Builder {
    private RelationshipMapping.Type type;

    public Builder type(RelationshipMapping.Type type) {
      this.type = type;
      return this;
    }

    /** Call the private constructor. */
    public abstract UFRelationshipMapping build();

    /** Default constructor for Jackson. */
    public Builder() {}
  }

  /** Deserialize to the internal representation of the relationship mapping. */
  public abstract RelationshipMapping deserializeToInternal(
      Entity entityA, Entity entityB, Map<String, DataPointer> dataPointers);

  public RelationshipMapping.Type getType() {
    return type;
  }
}

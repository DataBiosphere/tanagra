package bio.terra.tanagra.serialization;

import bio.terra.tanagra.underlay.RelationshipMapping;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

/**
 * External representation of a relationship mapping.
 *
 * <p>This is a POJO class intended for serialization. This JSON format is user-facing.
 */
@JsonDeserialize(builder = UFRelationshipMapping.Builder.class)
public class UFRelationshipMapping {
  private final UFTablePointer tablePointer;
  private final UFFieldPointer fromEntityId;
  private final UFFieldPointer toEntityId;

  public UFRelationshipMapping(RelationshipMapping relationshipMapping) {
    this.tablePointer = new UFTablePointer(relationshipMapping.getTablePointer());
    this.fromEntityId = new UFFieldPointer(relationshipMapping.getFromEntityId());
    this.toEntityId = new UFFieldPointer(relationshipMapping.getToEntityId());
  }

  private UFRelationshipMapping(Builder builder) {
    this.tablePointer = builder.tablePointer;
    this.fromEntityId = builder.fromEntityId;
    this.toEntityId = builder.toEntityId;
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {
    private UFTablePointer tablePointer;
    private UFFieldPointer fromEntityId;
    private UFFieldPointer toEntityId;

    public Builder tablePointer(UFTablePointer tablePointer) {
      this.tablePointer = tablePointer;
      return this;
    }

    public Builder fromEntityId(UFFieldPointer fromEntityId) {
      this.fromEntityId = fromEntityId;
      return this;
    }

    public Builder toEntityId(UFFieldPointer toEntityId) {
      this.toEntityId = toEntityId;
      return this;
    }

    /** Call the private constructor. */
    public UFRelationshipMapping build() {
      return new UFRelationshipMapping(this);
    }

    /** Default constructor for Jackson. */
    public Builder() {}
  }

  public UFTablePointer getTablePointer() {
    return tablePointer;
  }

  public UFFieldPointer getFromEntityId() {
    return fromEntityId;
  }

  public UFFieldPointer getToEntityId() {
    return toEntityId;
  }
}

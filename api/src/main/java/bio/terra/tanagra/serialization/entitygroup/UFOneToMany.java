package bio.terra.tanagra.serialization.entitygroup;

import bio.terra.tanagra.serialization.UFEntityGroup;
import bio.terra.tanagra.serialization.UFRelationshipMapping;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

/**
 * External representation of a one-to-many entity group (e.g. brand-ingredient).
 *
 * <p>This is a POJO class intended for serialization. This JSON format is user-facing.
 */
@JsonDeserialize(builder = UFOneToMany.Builder.class)
public class UFOneToMany extends UFEntityGroup {
  public final String entity1;
  public final String entityM;
  public final UFRelationshipMapping relationship;

  /** Constructor for Jackson deserialization during testing. */
  private UFOneToMany(Builder builder) {
    super(builder);
    this.entity1 = builder.entity1;
    this.entityM = builder.entityM;
    this.relationship = builder.relationship;
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder extends UFEntityGroup.Builder {
    private String entity1;
    private String entityM;
    private UFRelationshipMapping relationship;

    public Builder entity1(String entity1) {
      this.entity1 = entity1;
      return this;
    }

    public Builder entityM(String entityM) {
      this.entityM = entityM;
      return this;
    }

    public Builder relationship(UFRelationshipMapping relationship) {
      this.relationship = relationship;
      return this;
    }

    /** Call the private constructor. */
    public UFOneToMany build() {
      return new UFOneToMany(this);
    }

    /** Default constructor for Jackson. */
    public Builder() {}
  }
}

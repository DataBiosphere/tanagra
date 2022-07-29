package bio.terra.tanagra.serialization;

import bio.terra.tanagra.underlay.Attribute;
import bio.terra.tanagra.underlay.Literal;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

/**
 * External representation of an entity attribute.
 *
 * <p>This is a POJO class intended for serialization. This JSON format is user-facing.
 */
@JsonDeserialize(builder = UFAttribute.Builder.class)
public class UFAttribute {
  public final Attribute.Type type;
  public final String name;
  public final Literal.DataType dataType;

  /** Constructor for Jackson deserialization during testing. */
  protected UFAttribute(Builder builder) {
    this.type = builder.type;
    this.name = builder.name;
    this.dataType = builder.dataType;
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {
    private Attribute.Type type;
    private String name;
    private Literal.DataType dataType;

    public Builder type(Attribute.Type type) {
      this.type = type;
      return this;
    }

    public Builder name(String name) {
      this.name = name;
      return this;
    }

    public Builder dataType(Literal.DataType dataType) {
      this.dataType = dataType;
      return this;
    }

    /** Call the private constructor. */
    public UFAttribute build() {
      return new UFAttribute(this);
    }

    /** Default constructor for Jackson. */
    public Builder() {}
  }
}

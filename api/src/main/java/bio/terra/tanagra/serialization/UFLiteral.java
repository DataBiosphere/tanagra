package bio.terra.tanagra.serialization;

import bio.terra.tanagra.underlay.Literal;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

/**
 * External representation of a literal value.
 *
 * <p>This is a POJO class intended for serialization. This JSON format is user-facing.
 */
@JsonDeserialize(builder = UFLiteral.Builder.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UFLiteral {
  public final String stringVal;
  public final Long int64Val;
  public final Boolean booleanVal;

  public UFLiteral(Literal literal) {
    this.stringVal = literal.getStringVal();
    this.int64Val = literal.getInt64Val();
    this.booleanVal = literal.getBooleanVal();
  }

  private UFLiteral(Builder builder) {
    this.stringVal = builder.stringVal;
    this.int64Val = builder.int64Val;
    this.booleanVal = builder.booleanVal;
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {
    private String stringVal;
    private Long int64Val;
    private Boolean booleanVal;

    public Builder stringVal(String stringVal) {
      this.stringVal = stringVal;
      return this;
    }

    public Builder int64Val(Long int64Val) {
      this.int64Val = int64Val;
      return this;
    }

    public Builder booleanVal(Boolean booleanVal) {
      this.booleanVal = booleanVal;
      return this;
    }

    /** Call the private constructor. */
    public UFLiteral build() {
      return new UFLiteral(this);
    }

    /** Default constructor for Jackson. */
    public Builder() {}
  }
}

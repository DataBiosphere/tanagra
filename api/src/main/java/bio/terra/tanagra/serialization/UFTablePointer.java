package bio.terra.tanagra.serialization;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

/**
 * External representation of a pointer to a table in the underlying data.
 *
 * <p>This is a POJO class intended for serialization. This JSON format is user-facing.
 */
@JsonDeserialize(builder = UFTablePointer.Builder.class)
public class UFTablePointer {
  public final String table;
  public final UFTableFilter filter;

  /** Constructor for Jackson deserialization during testing. */
  protected UFTablePointer(Builder builder) {
    this.table = builder.table;
    this.filter = builder.filter;
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {
    private String table;
    private UFTableFilter filter;

    public Builder table(String table) {
      this.table = table;
      return this;
    }

    public Builder filter(UFTableFilter filter) {
      this.filter = filter;
      return this;
    }

    /** Call the private constructor. */
    public UFTablePointer build() {
      return new UFTablePointer(this);
    }

    /** Default constructor for Jackson. */
    public Builder() {}
  }
}

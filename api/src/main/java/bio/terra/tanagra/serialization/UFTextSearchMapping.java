package bio.terra.tanagra.serialization;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.util.List;

/**
 * External representation of a mapping between an entity attribute and the underlying data for a
 * text search.
 *
 * <p>This is a POJO class intended for serialization. This JSON format is user-facing.
 */
@JsonDeserialize(builder = UFTextSearchMapping.Builder.class)
public class UFTextSearchMapping {
  public final List<String> attributes;
  public final UFFieldPointer searchString;

  /** Constructor for Jackson deserialization during testing. */
  private UFTextSearchMapping(Builder builder) {
    this.attributes = builder.attributes;
    this.searchString = builder.searchString;
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {
    private List<String> attributes;
    private UFFieldPointer searchString;

    public Builder attributes(List<String> attributes) {
      this.attributes = attributes;
      return this;
    }

    public Builder searchString(UFFieldPointer searchString) {
      this.searchString = searchString;
      return this;
    }

    /** Call the private constructor. */
    public UFTextSearchMapping build() {
      return new UFTextSearchMapping(this);
    }

    /** Default constructor for Jackson. */
    public Builder() {}
  }
}

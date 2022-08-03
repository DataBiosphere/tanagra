package bio.terra.tanagra.serialization;

import bio.terra.tanagra.underlay.TextSearchMapping;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.util.List;
import java.util.stream.Collectors;

/**
 * External representation of a mapping between an entity attribute and the underlying data for a
 * text search.
 *
 * <p>This is a POJO class intended for serialization. This JSON format is user-facing.
 */
@JsonDeserialize(builder = UFTextSearchMapping.Builder.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UFTextSearchMapping {
  private final List<String> attributes;
  private final UFFieldPointer searchString;

  public UFTextSearchMapping(TextSearchMapping textSearchMapping) {
    this.attributes =
        textSearchMapping.definedByAttributes()
            ? textSearchMapping.getAttributes().stream()
                .map(attr -> attr.getName())
                .collect(Collectors.toList())
            : null;
    this.searchString =
        textSearchMapping.defiendBySearchString()
            ? new UFFieldPointer(textSearchMapping.getSearchString())
            : null;
  }

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

  public List<String> getAttributes() {
    return attributes;
  }

  public UFFieldPointer getSearchString() {
    return searchString;
  }
}

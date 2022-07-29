package bio.terra.tanagra.serialization;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.util.List;

/**
 * External representation of an underlay configuration.
 *
 * <p>This is a POJO class intended for serialization. This JSON format is user-facing.
 */
@JsonDeserialize(builder = UFUnderlay.Builder.class)
public class UFUnderlay {
  public final String name;
  public final List<UFDataPointer> dataPointers;
  public final List<String> entities;
  public final List<String> entityGroups;
  public final String primaryEntity;
  public final boolean skipGeneratingIndexData;
  public final boolean useSourceDataForSearch;

  /** Constructor for Jackson deserialization during testing. */
  private UFUnderlay(Builder builder) {
    this.name = builder.name;
    this.dataPointers = builder.dataPointers;
    this.entities = builder.entities;
    this.entityGroups = builder.entityGroups;
    this.primaryEntity = builder.primaryEntity;
    this.skipGeneratingIndexData = builder.skipGeneratingIndexData;
    this.useSourceDataForSearch = builder.useSourceDataForSearch;
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {
    private String name;
    private List<UFDataPointer> dataPointers;
    private List<String> entities;
    private List<String> entityGroups;
    private String primaryEntity;
    private boolean skipGeneratingIndexData;
    private boolean useSourceDataForSearch;

    public Builder name(String name) {
      this.name = name;
      return this;
    }

    public Builder dataPointers(List<UFDataPointer> dataPointers) {
      this.dataPointers = dataPointers;
      return this;
    }

    public Builder entities(List<String> entities) {
      this.entities = entities;
      return this;
    }

    public Builder entityGroups(List<String> entityGroups) {
      this.entityGroups = entityGroups;
      return this;
    }

    public Builder primaryEntity(String primaryEntity) {
      this.primaryEntity = primaryEntity;
      return this;
    }

    public Builder skipGeneratingIndexData(boolean skipGeneratingIndexData) {
      this.skipGeneratingIndexData = skipGeneratingIndexData;
      return this;
    }

    public Builder useSourceDataForSearch(boolean useSourceDataForSearch) {
      this.useSourceDataForSearch = useSourceDataForSearch;
      return this;
    }

    /** Call the private constructor. */
    public UFUnderlay build() {
      return new UFUnderlay(this);
    }

    /** Default constructor for Jackson. */
    public Builder() {}
  }
}

package bio.terra.tanagra.serialization;

import static bio.terra.tanagra.indexing.Indexer.OUTPUT_UNDERLAY_FILE_EXTENSION;

import bio.terra.tanagra.underlay.Underlay;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.util.List;
import java.util.stream.Collectors;

/**
 * External representation of an underlay configuration.
 *
 * <p>This is a POJO class intended for serialization. This JSON format is user-facing.
 */
@JsonDeserialize(builder = UFUnderlay.Builder.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UFUnderlay {
  public final String name;
  public final List<UFDataPointer> dataPointers;
  public final List<String> entities;
  public final List<String> entityGroups;
  public final String primaryEntity;

  public UFUnderlay(Underlay underlay) {
    this.name = underlay.getName();
    this.dataPointers =
        underlay.getDataPointers().values().stream()
            .map(dp -> dp.serialize())
            .collect(Collectors.toList());
    this.entities =
        underlay.getEntities().keySet().stream()
            .map(entityName -> entityName + OUTPUT_UNDERLAY_FILE_EXTENSION)
            .collect(Collectors.toList());
    this.entityGroups = null;
    this.primaryEntity = underlay.getPrimaryEntity().getName();
  }

  private UFUnderlay(Builder builder) {
    this.name = builder.name;
    this.dataPointers = builder.dataPointers;
    this.entities = builder.entities;
    this.entityGroups = builder.entityGroups;
    this.primaryEntity = builder.primaryEntity;
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {
    private String name;
    private List<UFDataPointer> dataPointers;
    private List<String> entities;
    private List<String> entityGroups;
    private String primaryEntity;

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

    /** Call the private constructor. */
    public UFUnderlay build() {
      return new UFUnderlay(this);
    }

    /** Default constructor for Jackson. */
    public Builder() {}
  }
}

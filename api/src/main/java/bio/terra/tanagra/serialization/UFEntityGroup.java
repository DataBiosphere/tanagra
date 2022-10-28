package bio.terra.tanagra.serialization;

import bio.terra.tanagra.underlay.EntityGroup;
import bio.terra.tanagra.underlay.Underlay;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.util.HashMap;
import java.util.Map;

/**
 * External representation of an entity group configuration.
 *
 * <p>This is a POJO class intended for serialization. This JSON format is user-facing.
 */
@JsonDeserialize(builder = UFEntityGroup.Builder.class)
public class UFEntityGroup {
  private final EntityGroup.Type type;
  private final String name;
  private final Map<String, String> entities;
  private final UFEntityGroupMapping sourceDataMapping;
  private final UFEntityGroupMapping indexDataMapping;

  public UFEntityGroup(EntityGroup entityGroup) {
    this.type = entityGroup.getType();
    this.name = entityGroup.getName();
    Map<String, String> entities = new HashMap<>();
    entityGroup.getEntityMap().entrySet().stream()
        .forEach(e -> entities.put(e.getKey(), e.getValue().getName()));
    this.entities = entities;
    this.sourceDataMapping =
        new UFEntityGroupMapping(entityGroup.getMapping(Underlay.MappingType.SOURCE));
    this.indexDataMapping =
        new UFEntityGroupMapping(entityGroup.getMapping(Underlay.MappingType.INDEX));
  }

  private UFEntityGroup(Builder builder) {
    this.type = builder.type;
    this.name = builder.name;
    this.entities = builder.entities;
    this.sourceDataMapping = builder.sourceDataMapping;
    this.indexDataMapping = builder.indexDataMapping;
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {
    private EntityGroup.Type type;
    private String name;
    private Map<String, String> entities;
    private UFEntityGroupMapping sourceDataMapping;
    private UFEntityGroupMapping indexDataMapping;

    public Builder type(EntityGroup.Type type) {
      this.type = type;
      return this;
    }

    public Builder name(String name) {
      this.name = name;
      return this;
    }

    public Builder entities(Map<String, String> entities) {
      this.entities = entities;
      return this;
    }

    public Builder sourceDataMapping(UFEntityGroupMapping sourceDataMapping) {
      this.sourceDataMapping = sourceDataMapping;
      return this;
    }

    public Builder indexDataMapping(UFEntityGroupMapping indexDataMapping) {
      this.indexDataMapping = indexDataMapping;
      return this;
    }

    /** Call the private constructor. */
    public UFEntityGroup build() {
      return new UFEntityGroup(this);
    }
  }

  public EntityGroup.Type getType() {
    return type;
  }

  public String getName() {
    return name;
  }

  public Map<String, String> getEntities() {
    return entities;
  }

  public UFEntityGroupMapping getSourceDataMapping() {
    return sourceDataMapping;
  }

  public UFEntityGroupMapping getIndexDataMapping() {
    return indexDataMapping;
  }
}

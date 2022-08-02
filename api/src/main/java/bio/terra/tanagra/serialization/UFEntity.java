package bio.terra.tanagra.serialization;

import bio.terra.tanagra.underlay.Entity;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.util.List;
import java.util.stream.Collectors;

/**
 * External representation of an entity.
 *
 * <p>This is a POJO class intended for serialization. This JSON format is user-facing.
 */
@JsonDeserialize(builder = UFEntity.Builder.class)
public class UFEntity {
  public final String name;
  public final String idAttribute;
  public final List<UFAttribute> attributes;
  public final UFEntityMapping sourceDataMapping;
  public final UFEntityMapping indexDataMapping;

  public UFEntity(Entity entity) {
    this.name = entity.getName();
    this.idAttribute = entity.getIdAttribute().getName();
    this.attributes =
        entity.getAttributes().stream()
            .map(attr -> new UFAttribute(attr))
            .collect(Collectors.toList());
    this.sourceDataMapping = new UFEntityMapping(entity.getSourceDataMapping());
    this.indexDataMapping = new UFEntityMapping(entity.getIndexDataMapping());
  }

  private UFEntity(Builder builder) {
    this.name = builder.name;
    this.idAttribute = builder.idAttribute;
    this.attributes = builder.attributes;
    this.sourceDataMapping = builder.sourceDataMapping;
    this.indexDataMapping = builder.indexDataMapping;
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {
    private String name;
    private String idAttribute;
    private List<UFAttribute> attributes;
    private UFEntityMapping sourceDataMapping;
    private UFEntityMapping indexDataMapping;

    public Builder name(String name) {
      this.name = name;
      return this;
    }

    public Builder idAttribute(String idAttribute) {
      this.idAttribute = idAttribute;
      return this;
    }

    public Builder attributes(List<UFAttribute> attributes) {
      this.attributes = attributes;
      return this;
    }

    public Builder sourceDataMapping(UFEntityMapping sourceDataMapping) {
      this.sourceDataMapping = sourceDataMapping;
      return this;
    }

    public Builder indexDataMapping(UFEntityMapping indexDataMapping) {
      this.indexDataMapping = indexDataMapping;
      return this;
    }

    /** Call the private constructor. */
    public UFEntity build() {
      return new UFEntity(this);
    }

    /** Default constructor for Jackson. */
    public Builder() {}
  }
}

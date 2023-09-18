package bio.terra.tanagra.serialization;

import bio.terra.tanagra.underlay.Attribute;
import bio.terra.tanagra.underlay.Entity;
import bio.terra.tanagra.underlay.Underlay;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * External representation of an entity.
 *
 * <p>This is a POJO class intended for serialization. This JSON format is user-facing.
 */
@JsonDeserialize(builder = UFEntity.Builder.class)
public class UFEntity {
  private final String name;
  private final String idAttribute;
  private final List<UFAttribute> attributes;
  private final UFEntityMapping sourceDataMapping;
  private final UFEntityMapping indexDataMapping;
  private final List<String> frequentFilterAttributes;

  public UFEntity(Entity entity) {
    this.name = entity.getName();
    this.idAttribute = entity.getIdAttribute().getName();
    this.attributes =
        entity.getAttributes().stream()
            .map(attr -> new UFAttribute(attr))
            .collect(Collectors.toList());
    this.sourceDataMapping = new UFEntityMapping(entity.getMapping(Underlay.MappingType.SOURCE));
    this.indexDataMapping = new UFEntityMapping(entity.getMapping(Underlay.MappingType.INDEX));
    this.frequentFilterAttributes =
        entity.getFrequentFilterAttributes().stream()
            .map(Attribute::getName)
            .collect(Collectors.toList());
  }

  private UFEntity(Builder builder) {
    this.name = builder.name;
    this.idAttribute = builder.idAttribute;
    this.attributes = builder.attributes;
    this.sourceDataMapping = builder.sourceDataMapping;
    this.indexDataMapping = builder.indexDataMapping;
    this.frequentFilterAttributes = builder.frequentFilterAttributes;
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {
    private String name;
    private String idAttribute;
    private List<UFAttribute> attributes;
    private UFEntityMapping sourceDataMapping;
    private UFEntityMapping indexDataMapping;
    private List<String> frequentFilterAttributes;

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

    public Builder frequentFilterAttributes(List<String> frequentFilterAttributes) {
      this.frequentFilterAttributes = frequentFilterAttributes;
      return this;
    }

    /** Call the private constructor. */
    public UFEntity build() {
      return new UFEntity(this);
    }
  }

  public String getName() {
    return name;
  }

  public String getIdAttribute() {
    return idAttribute;
  }

  public List<UFAttribute> getAttributes() {
    return attributes;
  }

  public UFEntityMapping getSourceDataMapping() {
    return sourceDataMapping;
  }

  public UFEntityMapping getIndexDataMapping() {
    return indexDataMapping;
  }

  public List<String> getFrequentFilterAttributes() {
    return frequentFilterAttributes == null ? Collections.EMPTY_LIST : frequentFilterAttributes;
  }
}

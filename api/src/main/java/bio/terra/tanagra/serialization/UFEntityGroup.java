package bio.terra.tanagra.serialization;

import bio.terra.tanagra.serialization.entitygroup.UFCriteriaOccurrence;
import bio.terra.tanagra.serialization.entitygroup.UFOneToMany;
import bio.terra.tanagra.underlay.DataPointer;
import bio.terra.tanagra.underlay.Entity;
import bio.terra.tanagra.underlay.EntityGroup;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.util.Map;

/**
 * External representation of a data pointer configuration.
 *
 * <p>This is a POJO class intended for serialization. This JSON format is user-facing.
 */
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "type")
@JsonSubTypes({
  @JsonSubTypes.Type(value = UFOneToMany.class, name = "ONE_TO_MANY"),
  @JsonSubTypes.Type(value = UFCriteriaOccurrence.class, name = "CRITERIA_OCCURRENCE")
})
@JsonDeserialize(builder = UFEntityGroup.Builder.class)
public abstract class UFEntityGroup {
  private final EntityGroup.Type type;
  private final String name;
  private final String indexDataPointer;

  protected UFEntityGroup(EntityGroup entityGroup) {
    this.type = entityGroup.getType();
    this.name = entityGroup.getName();
    this.indexDataPointer = entityGroup.getIndexDataPointer().getName();
  }

  protected UFEntityGroup(Builder builder) {
    this.type = builder.type;
    this.name = builder.name;
    this.indexDataPointer = builder.indexDataPointer;
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public abstract static class Builder {
    private EntityGroup.Type type;
    private String name;
    private String indexDataPointer;

    public Builder type(EntityGroup.Type type) {
      this.type = type;
      return this;
    }

    public Builder name(String name) {
      this.name = name;
      return this;
    }

    public Builder indexDataPointer(String indexDataPointer) {
      this.indexDataPointer = indexDataPointer;
      return this;
    }

    /** Call the private constructor. */
    public abstract UFEntityGroup build();

    /** Default constructor for Jackson. */
    public Builder() {}
  }

  /** Deserialize to the internal representation of the entity group. */
  public abstract EntityGroup deserializeToInternal(
      Map<String, DataPointer> dataPointers,
      Map<String, Entity> entities,
      String primaryEntityName);

  public EntityGroup.Type getType() {
    return type;
  }

  public String getName() {
    return name;
  }

  public String getIndexDataPointer() {
    return indexDataPointer;
  }
}

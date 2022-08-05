package bio.terra.tanagra.serialization.relationshipmapping;

import bio.terra.tanagra.serialization.UFRelationshipMapping;
import bio.terra.tanagra.underlay.DataPointer;
import bio.terra.tanagra.underlay.Entity;
import bio.terra.tanagra.underlay.RelationshipMapping;
import bio.terra.tanagra.underlay.relationshipmapping.ForeignKey;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.util.Map;

/**
 * External representation of a relationship defined by an intermediate table.
 *
 * <p>This is a POJO class intended for serialization. This JSON format is user-facing.
 */
@JsonDeserialize(builder = UFForeignKey.Builder.class)
public class UFForeignKey extends UFRelationshipMapping {
  private final String keyAttribute;

  public UFForeignKey(ForeignKey relationshipMapping) {
    super(relationshipMapping);
    this.keyAttribute = relationshipMapping.getKeyAttributeA().getName();
  }

  protected UFForeignKey(Builder builder) {
    super(builder);
    this.keyAttribute = builder.keyAttribute;
  }

  @Override
  public RelationshipMapping deserializeToInternal(
      Entity entityA, Entity entityB, Map<String, DataPointer> dataPointers) {
    return ForeignKey.fromSerialized(this, entityA, entityB);
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder extends UFRelationshipMapping.Builder {
    private String keyAttribute;

    public Builder keyAttribute(String keyAttribute) {
      this.keyAttribute = keyAttribute;
      return this;
    }

    /** Call the private constructor. */
    public UFForeignKey build() {
      return new UFForeignKey(this);
    }

    /** Default constructor for Jackson. */
    public Builder() {}
  }

  public String getKeyAttribute() {
    return keyAttribute;
  }
}

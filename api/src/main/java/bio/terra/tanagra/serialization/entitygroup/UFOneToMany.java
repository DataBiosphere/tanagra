package bio.terra.tanagra.serialization.entitygroup;

import bio.terra.tanagra.serialization.UFEntityGroup;
import bio.terra.tanagra.serialization.UFRelationshipMapping;
import bio.terra.tanagra.underlay.DataPointer;
import bio.terra.tanagra.underlay.Entity;
import bio.terra.tanagra.underlay.entitygroup.OneToMany;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.util.Map;

/**
 * External representation of a one-to-many entity group (e.g. brand-ingredient).
 *
 * <p>This is a POJO class intended for serialization. This JSON format is user-facing.
 */
@JsonDeserialize(builder = UFOneToMany.Builder.class)
public class UFOneToMany extends UFEntityGroup {
  private final String entity1;
  private final String entityM;
  private final UFRelationshipMapping oneToManyRelationship;

  public UFOneToMany(OneToMany entityGroup) {
    super(entityGroup);
    this.entity1 = entityGroup.getEntity1().getName();
    this.entityM = entityGroup.getEntityM().getName();
    this.oneToManyRelationship = entityGroup.getOneToManyRelationship().serialize();
  }

  private UFOneToMany(Builder builder) {
    super(builder);
    this.entity1 = builder.entity1;
    this.entityM = builder.entityM;
    this.oneToManyRelationship = builder.oneToManyRelationship;
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder extends UFEntityGroup.Builder {
    private String entity1;
    private String entityM;
    private UFRelationshipMapping oneToManyRelationship;

    public Builder entity1(String entity1) {
      this.entity1 = entity1;
      return this;
    }

    public Builder entityM(String entityM) {
      this.entityM = entityM;
      return this;
    }

    public Builder oneToManyRelationship(UFRelationshipMapping oneToManyRelationship) {
      this.oneToManyRelationship = oneToManyRelationship;
      return this;
    }

    /** Call the private constructor. */
    public UFOneToMany build() {
      return new UFOneToMany(this);
    }

    /** Default constructor for Jackson. */
    public Builder() {}
  }

  @Override
  public OneToMany deserializeToInternal(
      Map<String, DataPointer> dataPointers,
      Map<String, Entity> entities,
      String primaryEntityName) {
    return OneToMany.fromSerialized(this, dataPointers, entities);
  }

  public String getEntity1() {
    return entity1;
  }

  public String getEntityM() {
    return entityM;
  }

  public UFRelationshipMapping getOneToManyRelationship() {
    return oneToManyRelationship;
  }
}

package bio.terra.tanagra.serialization.relationshipmapping;

import bio.terra.tanagra.serialization.UFRelationshipMapping;
import bio.terra.tanagra.underlay.DataPointer;
import bio.terra.tanagra.underlay.Entity;
import bio.terra.tanagra.underlay.RelationshipMapping;
import bio.terra.tanagra.underlay.relationshipmapping.IntermediateTable;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.util.Map;

/**
 * External representation of a relationship defined by an intermediate table.
 *
 * <p>This is a POJO class intended for serialization. This JSON format is user-facing.
 */
@JsonDeserialize(builder = UFIntermediateTable.Builder.class)
public class UFIntermediateTable extends UFRelationshipMapping {
  private final String dataPointer;
  private final String tablePointer;
  private final String entityKeyFieldA;
  private final String entityKeyFieldB;

  public UFIntermediateTable(IntermediateTable relationshipMapping) {
    super(relationshipMapping);
    this.dataPointer = relationshipMapping.getDataPointer().getName();
    this.tablePointer = relationshipMapping.getTablePointer().getTableName();
    this.entityKeyFieldA = relationshipMapping.getEntityKeyA().getColumnName();
    this.entityKeyFieldB = relationshipMapping.getEntityKeyB().getColumnName();
  }

  protected UFIntermediateTable(Builder builder) {
    super(builder);
    this.dataPointer = builder.dataPointer;
    this.tablePointer = builder.tablePointer;
    this.entityKeyFieldA = builder.entityKeyFieldA;
    this.entityKeyFieldB = builder.entityKeyFieldB;
  }

  @Override
  public RelationshipMapping deserializeToInternal(
      Entity entityA, Entity entityB, Map<String, DataPointer> dataPointers) {
    return IntermediateTable.fromSerialized(this, entityA, entityB, dataPointers);
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder extends UFRelationshipMapping.Builder {
    private String dataPointer;
    private String tablePointer;
    private String entityKeyFieldA;
    private String entityKeyFieldB;

    public Builder dataPointer(String dataPointer) {
      this.dataPointer = dataPointer;
      return this;
    }

    public Builder tablePointer(String tablePointer) {
      this.tablePointer = tablePointer;
      return this;
    }

    public Builder entityKeyFieldA(String entityKeyFieldA) {
      this.entityKeyFieldA = entityKeyFieldA;
      return this;
    }

    public Builder entityKeyFieldB(String entityKeyFieldB) {
      this.entityKeyFieldB = entityKeyFieldB;
      return this;
    }

    /** Call the private constructor. */
    public UFIntermediateTable build() {
      return new UFIntermediateTable(this);
    }

    /** Default constructor for Jackson. */
    public Builder() {}
  }

  public String getDataPointer() {
    return dataPointer;
  }

  public String getTablePointer() {
    return tablePointer;
  }

  public String getEntityKeyFieldA() {
    return entityKeyFieldA;
  }

  public String getEntityKeyFieldB() {
    return entityKeyFieldB;
  }
}

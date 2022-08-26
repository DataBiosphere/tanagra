package bio.terra.tanagra.underlay.entitygroup;

import bio.terra.tanagra.indexing.WorkflowCommand;
import bio.terra.tanagra.serialization.UFEntityGroup;
import bio.terra.tanagra.underlay.AuxiliaryData;
import bio.terra.tanagra.underlay.DataPointer;
import bio.terra.tanagra.underlay.Entity;
import bio.terra.tanagra.underlay.EntityGroup;
import bio.terra.tanagra.underlay.EntityGroupMapping;
import bio.terra.tanagra.underlay.Relationship;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class OneToMany extends EntityGroup {
  private static final String ONE_ENTITY_NAME = "one";
  private static final String MANY_ENTITY_NAME = "many";
  private static final String ONE_TO_MANY_RELATIONSHIP_NAME = "oneToMany";

  private Entity entity1;
  private Entity entityM;

  private OneToMany(Builder builder) {
    super(builder);
    this.entity1 = builder.entity1;
    this.entityM = builder.entityM;
  }

  public static OneToMany fromSerialized(
      UFEntityGroup serialized,
      Map<String, DataPointer> dataPointers,
      Map<String, Entity> entities) {
    Entity entity1 = deserializeEntity(serialized, ONE_ENTITY_NAME, entities);
    Entity entityM = deserializeEntity(serialized, MANY_ENTITY_NAME, entities);

    Map<String, Relationship> relationships =
        Map.of(
            ONE_TO_MANY_RELATIONSHIP_NAME,
            new Relationship(ONE_TO_MANY_RELATIONSHIP_NAME, entity1, entityM));
    Map<String, AuxiliaryData> auxiliaryData = Collections.emptyMap();

    EntityGroupMapping sourceDataMapping =
        EntityGroupMapping.fromSerializedForSourceData(
            serialized.getSourceDataMapping(), dataPointers, relationships, auxiliaryData);
    EntityGroupMapping indexDataMapping =
        EntityGroupMapping.fromSerializedForIndexData(
            serialized.getIndexDataMapping(), dataPointers, relationships, auxiliaryData);

    Builder builder = new Builder();
    builder
        .name(serialized.getName())
        .relationships(relationships)
        .auxiliaryData(auxiliaryData)
        .sourceDataMapping(sourceDataMapping)
        .indexDataMapping(indexDataMapping);
    return builder.entity1(entity1).entityM(entityM).build();
  }

  @Override
  public EntityGroup.Type getType() {
    return Type.ONE_TO_MANY;
  }

  @Override
  public Map<String, Entity> getEntities() {
    return ImmutableMap.of(ONE_ENTITY_NAME, entity1, MANY_ENTITY_NAME, entityM);
  }

  @Override
  public List<WorkflowCommand> getIndexingCommands() {
    return ImmutableList.of(); // no indexing workflows for one-to-many relationships
  }

  private static class Builder extends EntityGroup.Builder {
    private Entity entity1;
    private Entity entityM;

    public Builder entity1(Entity entity1) {
      this.entity1 = entity1;
      return this;
    }

    public Builder entityM(Entity entityM) {
      this.entityM = entityM;
      return this;
    }

    public OneToMany build() {
      return new OneToMany(this);
    }
  }
}

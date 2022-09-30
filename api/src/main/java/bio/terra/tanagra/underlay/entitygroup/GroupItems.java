package bio.terra.tanagra.underlay.entitygroup;

import bio.terra.tanagra.indexing.IndexingJob;
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

public class GroupItems extends EntityGroup {
  private static final String GROUP_ENTITY_NAME = "group";
  private static final String ITEMS_ENTITY_NAME = "items";
  private static final String GROUP_ITEMS_RELATIONSHIP_NAME = "groupToItems";

  private final Entity groupEntity;
  private final Entity itemsEntity;

  private GroupItems(Builder builder) {
    super(builder);
    this.groupEntity = builder.groupEntity;
    this.itemsEntity = builder.itemsEntity;
  }

  public static GroupItems fromSerialized(
      UFEntityGroup serialized,
      Map<String, DataPointer> dataPointers,
      Map<String, Entity> entities) {
    Entity entity1 = getDeserializedEntity(serialized, GROUP_ENTITY_NAME, entities);
    Entity entityM = getDeserializedEntity(serialized, ITEMS_ENTITY_NAME, entities);

    Map<String, Relationship> relationships =
        Map.of(
            GROUP_ITEMS_RELATIONSHIP_NAME,
            new Relationship(GROUP_ITEMS_RELATIONSHIP_NAME, entity1, entityM));
    Map<String, AuxiliaryData> auxiliaryData = Collections.emptyMap();

    EntityGroupMapping sourceDataMapping =
        EntityGroupMapping.fromSerializedForSourceData(
            serialized.getSourceDataMapping(),
            dataPointers,
            relationships,
            auxiliaryData,
            serialized.getName());
    EntityGroupMapping indexDataMapping =
        EntityGroupMapping.fromSerializedForIndexData(
            serialized.getIndexDataMapping(),
            dataPointers,
            relationships,
            auxiliaryData,
            serialized.getName());

    Builder builder = new Builder();
    builder
        .name(serialized.getName())
        .relationships(relationships)
        .auxiliaryData(auxiliaryData)
        .sourceDataMapping(sourceDataMapping)
        .indexDataMapping(indexDataMapping);
    return builder.groupEntity(entity1).itemsEntity(entityM).build();
  }

  @Override
  public EntityGroup.Type getType() {
    return Type.GROUP_ITEMS;
  }

  @Override
  public Map<String, Entity> getEntities() {
    return ImmutableMap.of(GROUP_ENTITY_NAME, groupEntity, ITEMS_ENTITY_NAME, itemsEntity);
  }

  @Override
  public List<WorkflowCommand> getIndexingCommands() {
    return ImmutableList.of(); // no indexing workflows for one-to-many relationships
  }

  @Override
  public List<IndexingJob> getIndexingJobs() {
    // TODO: Add a new indexing job to write the group-item id pairs to a separate table, or a new
    // column in the group denormalized entity instances table.
    return Collections.emptyList();
  }

  private static class Builder extends EntityGroup.Builder {
    private Entity groupEntity;
    private Entity itemsEntity;

    public Builder groupEntity(Entity groupEntity) {
      this.groupEntity = groupEntity;
      return this;
    }

    public Builder itemsEntity(Entity itemsEntity) {
      this.itemsEntity = itemsEntity;
      return this;
    }

    @Override
    public GroupItems build() {
      return new GroupItems(this);
    }
  }
}

package bio.terra.tanagra.underlay.entitygroup;

import bio.terra.tanagra.serialization.UFEntityGroup;
import bio.terra.tanagra.underlay.AuxiliaryData;
import bio.terra.tanagra.underlay.DataPointer;
import bio.terra.tanagra.underlay.Entity;
import bio.terra.tanagra.underlay.EntityGroup;
import bio.terra.tanagra.underlay.EntityGroupMapping;
import bio.terra.tanagra.underlay.Relationship;
import bio.terra.tanagra.underlay.Underlay;
import com.google.common.collect.ImmutableMap;
import java.util.Collections;
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
    // Entities.
    Entity entity1 = getDeserializedEntity(serialized, GROUP_ENTITY_NAME, entities);
    Entity entityM = getDeserializedEntity(serialized, ITEMS_ENTITY_NAME, entities);

    // Relationships.
    Map<String, Relationship> relationships =
        Map.of(
            GROUP_ITEMS_RELATIONSHIP_NAME,
            new Relationship(
                GROUP_ITEMS_RELATIONSHIP_NAME, entity1, entityM, Collections.emptyList()));

    // Auxiliary data.
    Map<String, AuxiliaryData> auxiliaryData = Collections.emptyMap();

    // Source+index entity group mappings.
    EntityGroupMapping sourceDataMapping =
        EntityGroupMapping.fromSerialized(
            serialized.getSourceDataMapping(), dataPointers, Underlay.MappingType.SOURCE);
    EntityGroupMapping indexDataMapping =
        EntityGroupMapping.fromSerialized(
            serialized.getIndexDataMapping(), dataPointers, Underlay.MappingType.INDEX);

    Builder builder = new Builder();
    builder
        .name(serialized.getName())
        .relationships(relationships)
        .auxiliaryData(auxiliaryData)
        .sourceDataMapping(sourceDataMapping)
        .indexDataMapping(indexDataMapping);
    GroupItems groupItems = builder.groupEntity(entity1).itemsEntity(entityM).build();

    sourceDataMapping.initialize(groupItems);
    indexDataMapping.initialize(groupItems);

    // Source+index relationship, auxiliary data mappings.
    EntityGroup.deserializeRelationshipMappings(serialized, groupItems);

    return groupItems;
  }

  @Override
  public EntityGroup.Type getType() {
    return Type.GROUP_ITEMS;
  }

  @Override
  public Map<String, Entity> getEntityMap() {
    return ImmutableMap.of(GROUP_ENTITY_NAME, groupEntity, ITEMS_ENTITY_NAME, itemsEntity);
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

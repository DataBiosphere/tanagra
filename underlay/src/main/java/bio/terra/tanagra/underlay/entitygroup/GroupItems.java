package bio.terra.tanagra.underlay.entitygroup;

import bio.terra.tanagra.serialization.entitygroup.UFGroupItems;
import bio.terra.tanagra.underlay.DataPointer;
import bio.terra.tanagra.underlay.Entity;
import bio.terra.tanagra.underlay.EntityGroup;
import bio.terra.tanagra.underlay.EntityGroupMapping;
import bio.terra.tanagra.underlay.Relationship;
import bio.terra.tanagra.underlay.Underlay;
import java.util.Map;
import java.util.Set;

public class GroupItems extends EntityGroup {
  private static final String GROUP_ITEMS_RELATIONSHIP_NAME = "groupToItems";

  private final Entity groupEntity;
  private final Entity itemsEntity;

  private GroupItems(Builder builder) {
    super(builder);
    this.groupEntity = builder.groupEntity;
    this.itemsEntity = builder.itemsEntity;
  }

  public static GroupItems fromSerialized(
      UFGroupItems serialized,
      Map<String, DataPointer> dataPointers,
      Map<String, Entity> entities) {
    // Entities.
    Entity groupEntity = entities.get(serialized.getGroupEntity());
    Entity itemsEntity = entities.get(serialized.getItemsEntity());

    // Relationships.
    Map<String, Relationship> relationships =
        Map.of(
            GROUP_ITEMS_RELATIONSHIP_NAME,
            new Relationship(
                GROUP_ITEMS_RELATIONSHIP_NAME,
                groupEntity,
                itemsEntity,
                buildRelationshipFieldList(groupEntity)));

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
        .sourceDataMapping(sourceDataMapping)
        .indexDataMapping(indexDataMapping);
    GroupItems groupItems = builder.groupEntity(groupEntity).itemsEntity(itemsEntity).build();

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

  public Entity getGroupEntity() {
    return groupEntity;
  }

  public Entity getItemsEntity() {
    return itemsEntity;
  }

  @Override
  public UFGroupItems serialize() {
    return new UFGroupItems(this);
  }

  @Override
  protected Set<Entity> getEntities() {
    return Set.of(groupEntity, itemsEntity);
  }

  public Relationship getGroupItemsRelationship() {
    return relationships.get(GROUP_ITEMS_RELATIONSHIP_NAME);
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

package bio.terra.tanagra.underlay.entitymodel.entitygroup;

import bio.terra.tanagra.underlay.entitymodel.Entity;
import bio.terra.tanagra.underlay.entitymodel.Relationship;
import com.google.common.collect.ImmutableSet;

public class GroupItems extends EntityGroup {
  private final Entity groupEntity;
  private final Entity itemsEntity;
  private final Relationship groupItemsRelationship;

  public GroupItems(
      String name,
      boolean useSourceIdPairsSql,
      Entity groupEntity,
      Entity itemsEntity,
      Relationship groupItemsRelationship) {
    super(name, useSourceIdPairsSql);
    this.groupEntity = groupEntity;
    this.itemsEntity = itemsEntity;
    this.groupItemsRelationship = groupItemsRelationship;
  }

  @Override
  public Type getType() {
    return Type.GROUP_ITEMS;
  }

  @Override
  public boolean includesEntity(String name) {
    return groupEntity.getName().equals(name) || itemsEntity.getName().equals(name);
  }

  @Override
  public ImmutableSet<Relationship> getRelationships() {
    return ImmutableSet.of(groupItemsRelationship);
  }

  @Override
  public boolean hasRollupCountField(String entity, String countedEntity) {
    // There are rollup counts on the group entity, counting the number of related items entities.
    return groupEntity.getName().equals(entity) && itemsEntity.getName().equals(countedEntity);
  }

  public Entity getGroupEntity() {
    return groupEntity;
  }

  public Entity getItemsEntity() {
    return itemsEntity;
  }

  public Relationship getGroupItemsRelationship() {
    return groupItemsRelationship;
  }
}

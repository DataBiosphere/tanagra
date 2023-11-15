package bio.terra.tanagra.underlay.entitymodel.entitygroup;

import bio.terra.tanagra.underlay.entitymodel.Entity;
import bio.terra.tanagra.underlay.entitymodel.Relationship;
import com.google.common.collect.ImmutableSet;

public class GroupItems extends EntityGroup {
  private final Entity groupEntity;
  private final Entity itemsEntity;
  private final Relationship groupItemsRelationship;

  public GroupItems(
      String name, Entity groupEntity, Entity itemsEntity, Relationship groupItemsRelationship) {
    super(name);
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

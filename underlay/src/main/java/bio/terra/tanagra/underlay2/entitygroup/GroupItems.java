package bio.terra.tanagra.underlay2.entitygroup;

import bio.terra.tanagra.exception.SystemException;
import bio.terra.tanagra.query.FieldPointer;
import bio.terra.tanagra.underlay2.Entity;
import bio.terra.tanagra.underlay2.Relationship;
import bio.terra.tanagra.underlay2.indexschema.EntityMain;
import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;

public class GroupItems extends EntityGroup {
  private final Entity groupEntity;
  private final Entity itemsEntity;
  private final Relationship relationship;
  private final ImmutableMap<String, FieldPointer> indexNumItemsInGroupCountFields;

  public GroupItems(
      String name,
      String description,
      Entity groupEntity,
      Entity itemsEntity,
      Relationship relationship) {
    super(name, description);
    this.groupEntity = groupEntity;
    this.itemsEntity = itemsEntity;
    this.relationship = relationship;

    // Resolve index fields.
    Map<String, FieldPointer> indexNumItemsInGroupCountFieldBuilder = new HashMap<>();
    indexNumItemsInGroupCountFieldBuilder.put(
        null, EntityMain.getEntityGroupCountField(groupEntity.getName(), null, name));
    groupEntity.getHierarchies().stream()
        .forEach(
            h ->
                indexNumItemsInGroupCountFieldBuilder.put(
                    h.getName(),
                    EntityMain.getEntityGroupCountField(groupEntity.getName(), h.getName(), name)));
    this.indexNumItemsInGroupCountFields =
        ImmutableMap.copyOf(indexNumItemsInGroupCountFieldBuilder);
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
  public FieldPointer getCountField(
      String countForEntity, String countedEntity, @Nullable String countForHierarchy) {
    if (!groupEntity.getName().equals(countForEntity)
        || !itemsEntity.getName().equals(countedEntity)) {
      throw new SystemException(
          "No count field for "
              + countForEntity
              + "/"
              + countedEntity
              + ". Expected "
              + groupEntity.getName()
              + "/"
              + itemsEntity.getName());
    }
    FieldPointer countField = indexNumItemsInGroupCountFields.get(countForHierarchy);
    if (countField == null) {
      throw new SystemException(
          "No count field for "
              + countForEntity
              + "/"
              + countedEntity
              + ", hierarchy "
              + countForHierarchy);
    }
    return countField;
  }

  public Entity getGroupEntity() {
    return groupEntity;
  }

  public Entity getItemsEntity() {
    return itemsEntity;
  }

  public Relationship getRelationship() {
    return relationship;
  }

  public FieldPointer getIndexNumItemsInGroupCountField(@Nullable String hierarchyName) {
    FieldPointer field = indexNumItemsInGroupCountFields.get(hierarchyName);
    if (field == null) {
      throw new SystemException("No index count field found for hierarchy: " + hierarchyName);
    }
    return field;
  }
}

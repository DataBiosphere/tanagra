package bio.terra.tanagra.underlay.entitygroup;

import bio.terra.tanagra.indexing.WorkflowCommand;
import bio.terra.tanagra.serialization.entitygroup.UFOneToMany;
import bio.terra.tanagra.underlay.DataPointer;
import bio.terra.tanagra.underlay.Entity;
import bio.terra.tanagra.underlay.EntityGroup;
import bio.terra.tanagra.underlay.RelationshipMapping;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class OneToMany extends EntityGroup {
  private Entity entity1;
  private Entity entityM;
  private RelationshipMapping oneToManyRelationship;

  private OneToMany(
      String name,
      DataPointer indexDataPointer,
      Entity entity1,
      Entity entityM,
      RelationshipMapping oneToManyRelationship) {
    super(name, indexDataPointer);
    this.entity1 = entity1;
    this.entityM = entityM;
    this.oneToManyRelationship = oneToManyRelationship;
  }

  public static OneToMany fromSerialized(
      UFOneToMany serialized, Map<String, DataPointer> dataPointers, Map<String, Entity> entities) {
    if (serialized.getIndexDataPointer() == null || serialized.getIndexDataPointer().isEmpty()) {
      throw new IllegalArgumentException("Index data pointer is undefined");
    }
    DataPointer indexDataPointer = dataPointers.get(serialized.getIndexDataPointer());
    if (indexDataPointer == null) {
      throw new IllegalArgumentException("Index data pointer not found in set of data pointers");
    }

    if (serialized.getEntity1() == null || serialized.getEntity1().isEmpty()) {
      throw new IllegalArgumentException("No entity1 defined");
    }
    Entity entity1 = entities.get(serialized.getEntity1());
    if (entity1 == null) {
      throw new IllegalArgumentException("Entity1 not found in set of entities");
    }

    if (serialized.getEntityM() == null || serialized.getEntityM().isEmpty()) {
      throw new IllegalArgumentException("No entityM defined");
    }
    Entity entityM = entities.get(serialized.getEntityM());
    if (entityM == null) {
      throw new IllegalArgumentException("EntityM not found in set of entities");
    }

    if (serialized.getOneToManyRelationship() == null) {
      throw new IllegalArgumentException("No one-to-many relationship defined");
    }
    RelationshipMapping oneToManyRelationship =
        serialized.getOneToManyRelationship().deserializeToInternal(entity1, entityM, dataPointers);

    return new OneToMany(
        serialized.getName(), indexDataPointer, entity1, entityM, oneToManyRelationship);
  }

  @Override
  public EntityGroup.Type getType() {
    return Type.ONE_TO_MANY;
  }

  @Override
  public List<WorkflowCommand> getIndexingCommands() {
    return Collections.emptyList(); // no indexing workflows for one-to-many relationships, yet
  }

  public Entity getEntity1() {
    return entity1;
  }

  public Entity getEntityM() {
    return entityM;
  }

  public RelationshipMapping getOneToManyRelationship() {
    return oneToManyRelationship;
  }
}

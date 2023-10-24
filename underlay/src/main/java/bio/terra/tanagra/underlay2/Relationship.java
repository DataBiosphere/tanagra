package bio.terra.tanagra.underlay2;

import bio.terra.tanagra.query.FieldPointer;
import bio.terra.tanagra.query.TablePointer;
import bio.terra.tanagra.underlay2.indexschema.RelationshipIdPairs;
import java.util.Optional;

public class Relationship {
  private final Entity entityA;
  private final Entity entityB;
  private final TablePointer sourceIdPairsTable;
  private final FieldPointer sourceEntityAIdField;
  private final FieldPointer sourceEntityBIdField;
  private final boolean isForeignKeyAttributeEntityA;
  private final boolean isForeignKeyAttributeEntityB;
  private final TablePointer indexIdPairsTable;
  private final FieldPointer indexEntityAIdField;
  private final FieldPointer indexEntityBIdField;

  public Relationship(
      Entity entityA,
      Entity entityB,
      TablePointer sourceIdPairsTable,
      FieldPointer sourceEntityAIdField,
      FieldPointer sourceEntityBIdField) {
    this.entityA = entityA;
    this.entityB = entityB;
    this.sourceIdPairsTable = sourceIdPairsTable;
    this.sourceEntityAIdField = sourceEntityAIdField;
    this.sourceEntityBIdField = sourceEntityBIdField;

    // Check if sourceEntityBIdField matches any entityA attribute source value fields.
    Optional<Attribute> foreignKeyAttributeEntityA =
        entityA.getAttributes().stream()
            .filter(a -> a.getSourceValueField().equals(sourceEntityBIdField))
            .findFirst();
    this.isForeignKeyAttributeEntityA = foreignKeyAttributeEntityA.isPresent();

    // Check if sourceSelectEntityIdField matches any whereEntity attribute source value fields.
    Optional<Attribute> foreignKeyAttributeEntityB =
        entityB.getAttributes().stream()
            .filter(a -> a.getSourceValueField().equals(sourceEntityAIdField))
            .findFirst();
    this.isForeignKeyAttributeEntityB = foreignKeyAttributeEntityB.isPresent();

    // Resolve index tables and fields.
    if (this.isForeignKeyAttributeEntityA) {
      this.indexIdPairsTable =
          foreignKeyAttributeEntityA.get().getIndexValueField().getTablePointer();
      this.indexEntityAIdField = entityA.getIdAttribute().getIndexValueField();
      this.indexEntityBIdField = foreignKeyAttributeEntityA.get().getIndexValueField();
    } else if (this.isForeignKeyAttributeEntityB) {
      this.indexIdPairsTable =
          foreignKeyAttributeEntityB.get().getIndexValueField().getTablePointer();
      this.indexEntityAIdField = foreignKeyAttributeEntityB.get().getIndexValueField();
      this.indexEntityBIdField = entityB.getIdAttribute().getIndexValueField();
    } else {
      this.indexIdPairsTable = RelationshipIdPairs.getTable(entityA.getName(), entityB.getName());
      this.indexEntityAIdField =
          RelationshipIdPairs.getSelectEntityIdField(entityA.getName(), entityB.getName());
      this.indexEntityBIdField =
          RelationshipIdPairs.getWhereEntityIdField(entityA.getName(), entityB.getName());
    }
  }

  public Entity getEntityA() {
    return entityA;
  }

  public Entity getEntityB() {
    return entityB;
  }

  public TablePointer getSourceIdPairsTable() {
    return sourceIdPairsTable;
  }

  public FieldPointer getSourceEntityAIdField() {
    return sourceEntityAIdField;
  }

  public FieldPointer getSourceEntityBIdField() {
    return sourceEntityBIdField;
  }

  public TablePointer getIndexIdPairsTable() {
    return indexIdPairsTable;
  }

  public FieldPointer getIndexEntityAIdField() {
    return indexEntityAIdField;
  }

  public FieldPointer getIndexEntityBIdField() {
    return indexEntityBIdField;
  }

  public boolean isForeignKeyAttributeEntityA() {
    return isForeignKeyAttributeEntityA;
  }

  public boolean isForeignKeyAttributeEntityB() {
    return isForeignKeyAttributeEntityB;
  }

  public boolean isIntermediateTable() {
    return !isForeignKeyAttributeEntityA() && !isForeignKeyAttributeEntityB();
  }

  public FieldPointer getIndexEntityIdField(String entity) {
    return entityA.getName().equals(entity) ? indexEntityAIdField : indexEntityBIdField;
  }
}

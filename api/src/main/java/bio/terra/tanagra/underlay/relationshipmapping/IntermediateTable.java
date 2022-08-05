package bio.terra.tanagra.underlay.relationshipmapping;

import bio.terra.tanagra.serialization.relationshipmapping.UFIntermediateTable;
import bio.terra.tanagra.underlay.Attribute;
import bio.terra.tanagra.underlay.DataPointer;
import bio.terra.tanagra.underlay.Entity;
import bio.terra.tanagra.underlay.FieldPointer;
import bio.terra.tanagra.underlay.RelationshipMapping;
import bio.terra.tanagra.underlay.TablePointer;
import java.util.Map;

public class IntermediateTable extends RelationshipMapping {
  private FieldPointer entityKeyA;
  private FieldPointer entityKeyB;

  private IntermediateTable(
      Attribute idAttributeA,
      Attribute idAttributeB,
      FieldPointer entityKeyA,
      FieldPointer entityKeyB) {
    super(idAttributeA, idAttributeB);
    this.entityKeyA = entityKeyA;
    this.entityKeyB = entityKeyB;
  }

  public static IntermediateTable fromSerialized(
      UFIntermediateTable serialized,
      Entity entityA,
      Entity entityB,
      Map<String, DataPointer> dataPointers) {
    TablePointer intermediateTable =
        new TablePointer(
            serialized.getTablePointer(), dataPointers.get(serialized.getDataPointer()));
    FieldPointer entityKeyA = new FieldPointer(intermediateTable, serialized.getEntityKeyFieldA());
    FieldPointer entityKeyB = new FieldPointer(intermediateTable, serialized.getEntityKeyFieldB());

    return new IntermediateTable(
        entityA.getIdAttribute(), entityB.getIdAttribute(), entityKeyA, entityKeyB);
  }

  @Override
  public Type getType() {
    return Type.INTERMEDIATE_TABLE;
  }

  public FieldPointer getEntityKeyA() {
    return entityKeyA;
  }

  public FieldPointer getEntityKeyB() {
    return entityKeyB;
  }

  public TablePointer getTablePointer() {
    return entityKeyA.getTablePointer();
  }

  public DataPointer getDataPointer() {
    return entityKeyA.getTablePointer().getDataPointer();
  }
}

package bio.terra.tanagra.underlay;

import bio.terra.tanagra.serialization.UFRelationshipMapping;
import bio.terra.tanagra.serialization.relationshipmapping.UFForeignKey;
import bio.terra.tanagra.serialization.relationshipmapping.UFIntermediateTable;
import bio.terra.tanagra.underlay.relationshipmapping.ForeignKey;
import bio.terra.tanagra.underlay.relationshipmapping.IntermediateTable;

public abstract class RelationshipMapping {
  /** Enum for the types of entity relationships supported by Tanagra. */
  public enum Type {
    FOREIGN_KEY,
    INTERMEDIATE_TABLE
  }

  private Attribute idAttributeA;
  private Attribute idAttributeB;

  protected RelationshipMapping(Attribute idAttributeA, Attribute idAttributeB) {
    this.idAttributeA = idAttributeA;
    this.idAttributeB = idAttributeB;
  }

  public UFRelationshipMapping serialize() {
    switch (getType()) {
      case FOREIGN_KEY:
        return new UFForeignKey((ForeignKey) this);
      case INTERMEDIATE_TABLE:
        return new UFIntermediateTable((IntermediateTable) this);
      default:
        throw new RuntimeException("Unknown relationship mapping type: " + getType());
    }
  }

  public abstract Type getType();

  public Attribute getIdAttributeA() {
    return idAttributeA;
  }

  public Attribute getIdAttributeB() {
    return idAttributeB;
  }
}

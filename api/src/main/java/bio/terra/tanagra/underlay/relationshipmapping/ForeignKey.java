package bio.terra.tanagra.underlay.relationshipmapping;

import bio.terra.tanagra.serialization.relationshipmapping.UFForeignKey;
import bio.terra.tanagra.underlay.Attribute;
import bio.terra.tanagra.underlay.Entity;
import bio.terra.tanagra.underlay.RelationshipMapping;

public class ForeignKey extends RelationshipMapping {
  private Attribute keyAttributeA;

  private ForeignKey(Attribute idAttributeA, Attribute idAttributeB, Attribute keyAttributeA) {
    super(idAttributeA, idAttributeB);
    this.keyAttributeA = keyAttributeA;
  }

  public static ForeignKey fromSerialized(UFForeignKey serialized, Entity entityA, Entity entityB) {
    if (serialized.getKeyAttribute() == null || serialized.getKeyAttribute().isEmpty()) {
      throw new IllegalArgumentException("Key attribute is undefined");
    }
    Attribute keyAttributeA = entityA.getAttribute(serialized.getKeyAttribute());
    return new ForeignKey(entityA.getIdAttribute(), entityB.getIdAttribute(), keyAttributeA);
  }

  @Override
  public Type getType() {
    return Type.FOREIGN_KEY;
  }

  public Attribute getKeyAttributeA() {
    return keyAttributeA;
  }
}

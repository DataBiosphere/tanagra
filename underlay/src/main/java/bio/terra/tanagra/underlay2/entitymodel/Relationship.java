package bio.terra.tanagra.underlay2.entitymodel;

import javax.annotation.Nullable;

public class Relationship {
  private final Entity entityA;
  private final Entity entityB;
  private final @Nullable Attribute foreignKeyAttributeEntityA;
  private final @Nullable Attribute foreignKeyAttributeEntityB;

  public Relationship(
      Entity entityA,
      Entity entityB,
      @Nullable Attribute foreignKeyAttributeEntityA,
      @Nullable Attribute foreignKeyAttributeEntityB) {
    this.entityA = entityA;
    this.entityB = entityB;
    this.foreignKeyAttributeEntityA = foreignKeyAttributeEntityA;
    this.foreignKeyAttributeEntityB = foreignKeyAttributeEntityB;
  }

  public Entity getEntityA() {
    return entityA;
  }

  public Entity getEntityB() {
    return entityB;
  }

  public boolean isForeignKeyAttributeEntityA() {
    return foreignKeyAttributeEntityA != null;
  }

  public boolean isForeignKeyAttributeEntityB() {
    return foreignKeyAttributeEntityB != null;
  }

  public boolean isForeignKeyAttribute(Entity entity) {
    return entity.equals(entityA) ? isForeignKeyAttributeEntityA() : isForeignKeyAttributeEntityB();
  }

  @Nullable
  public Attribute getForeignKeyAttributeEntityA() {
    return foreignKeyAttributeEntityA;
  }

  @Nullable
  public Attribute getForeignKeyAttributeEntityB() {
    return foreignKeyAttributeEntityB;
  }

  @Nullable
  public Attribute getForeignKeyAttribute(Entity entity) {
    return entity.equals(entityA)
        ? getForeignKeyAttributeEntityA()
        : getForeignKeyAttributeEntityB();
  }

  public boolean isIntermediateTable() {
    return !isForeignKeyAttributeEntityA() && !isForeignKeyAttributeEntityB();
  }
}

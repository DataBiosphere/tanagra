package bio.terra.tanagra.underlay;

public class Relationship {
  private final String name;
  private final Entity entityA;
  private final Entity entityB;

  public Relationship(String name, Entity entityA, Entity entityB) {
    this.name = name;
    this.entityA = entityA;
    this.entityB = entityB;
  }

  public String getName() {
    return name;
  }

  public Entity getEntityA() {
    return entityA;
  }

  public Entity getEntityB() {
    return entityB;
  }
}

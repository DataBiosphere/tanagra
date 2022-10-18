package bio.terra.tanagra.underlay;

public class Relationship {
  private final String name;
  private final Entity entityA;
  private final Entity entityB;

  private RelationshipMapping sourceMapping;
  private RelationshipMapping indexMapping;
  private EntityGroup entityGroup;

  public Relationship(String name, Entity entityA, Entity entityB) {
    this.name = name;
    this.entityA = entityA;
    this.entityB = entityB;
  }

  public void initialize(
      RelationshipMapping sourceMapping,
      RelationshipMapping indexMapping,
      EntityGroup entityGroup) {
    this.sourceMapping = sourceMapping;
    this.indexMapping = indexMapping;
    this.entityGroup = entityGroup;
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

  public EntityGroup getEntityGroup() {
    return entityGroup;
  }

  public RelationshipMapping getMapping(Underlay.MappingType mappingType) {
    return Underlay.MappingType.SOURCE.equals(mappingType) ? sourceMapping : indexMapping;
  }
}

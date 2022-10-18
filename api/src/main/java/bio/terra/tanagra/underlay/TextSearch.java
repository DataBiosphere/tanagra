package bio.terra.tanagra.underlay;

public class TextSearch {
  private final TextSearchMapping sourceMapping;
  private final TextSearchMapping indexMapping;
  private Entity entity;

  public TextSearch(TextSearchMapping sourceMapping, TextSearchMapping indexMapping) {
    this.sourceMapping = sourceMapping;
    this.indexMapping = indexMapping;
  }

  public void initialize(Entity entity) {
    this.entity = entity;
    sourceMapping.initialize(this);
    indexMapping.initialize(this);
  }

  public TextSearchMapping getMapping(Underlay.MappingType mappingType) {
    return Underlay.MappingType.SOURCE.equals(mappingType) ? sourceMapping : indexMapping;
  }

  public Entity getEntity() {
    return entity;
  }
}

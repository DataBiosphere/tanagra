package bio.terra.tanagra.underlay;

public class Hierarchy {
  private final String name;
  private final HierarchyMapping sourceMapping;
  private final HierarchyMapping indexMapping;

  public Hierarchy(String name, HierarchyMapping sourceMapping, HierarchyMapping indexMapping) {
    this.name = name;
    this.sourceMapping = sourceMapping;
    this.indexMapping = indexMapping;
  }

  public String getName() {
    return name;
  }

  public HierarchyMapping getMapping(Underlay.MappingType mappingType) {
    return Underlay.MappingType.SOURCE.equals(mappingType) ? sourceMapping : indexMapping;
  }
}

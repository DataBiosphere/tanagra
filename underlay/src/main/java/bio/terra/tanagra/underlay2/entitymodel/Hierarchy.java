package bio.terra.tanagra.underlay2.entitymodel;

public class Hierarchy {
  private final String name;
  private final int maxDepth;
  private final boolean isKeepOrphanNodes;

  public Hierarchy(String name, int maxDepth, boolean isKeepOrphanNodes) {
    this.name = name;
    this.maxDepth = maxDepth;
    this.isKeepOrphanNodes = isKeepOrphanNodes;
  }

  public String getName() {
    return name;
  }

  public int getMaxDepth() {
    return maxDepth;
  }

  public boolean isKeepOrphanNodes() {
    return isKeepOrphanNodes;
  }
}

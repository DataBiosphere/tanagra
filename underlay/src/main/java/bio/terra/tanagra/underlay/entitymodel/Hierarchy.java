package bio.terra.tanagra.underlay.entitymodel;

import com.google.common.collect.ImmutableSet;
import java.util.Set;
import javax.annotation.Nullable;

public class Hierarchy {
  public static final String DEFAULT_NAME = "default";
  private final String name;
  private final int maxDepth;
  private final boolean isKeepOrphanNodes;
  private final ImmutableSet<Long> rootNodeIds;
  private final boolean isCleanHierarchyNodesWithZeroCounts;

  public Hierarchy(
      String name,
      int maxDepth,
      boolean isKeepOrphanNodes,
      @Nullable Set<Long> rootNodeIds,
      boolean isCleanZeroCountNodes) {
    this.name = name;
    this.maxDepth = maxDepth;
    this.isKeepOrphanNodes = isKeepOrphanNodes;
    this.rootNodeIds = rootNodeIds == null ? ImmutableSet.of() : ImmutableSet.copyOf(rootNodeIds);
    this.isCleanHierarchyNodesWithZeroCounts = isCleanZeroCountNodes;
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

  public ImmutableSet<Long> getRootNodeIds() {
    return rootNodeIds;
  }

  public boolean isCleanHierarchyNodesWithZeroCounts() {
    return isCleanHierarchyNodesWithZeroCounts;
  }
}

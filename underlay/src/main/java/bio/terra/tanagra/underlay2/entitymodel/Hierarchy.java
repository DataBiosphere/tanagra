package bio.terra.tanagra.underlay2.entitymodel;

import com.google.common.collect.ImmutableSet;
import java.util.Set;
import javax.annotation.Nullable;

public class Hierarchy {
  private final String name;
  private final int maxDepth;
  private final boolean isKeepOrphanNodes;
  private final ImmutableSet<Long> rootNodeIds;

  public Hierarchy(
      String name, int maxDepth, boolean isKeepOrphanNodes, @Nullable Set<Long> rootNodeIds) {
    this.name = name;
    this.maxDepth = maxDepth;
    this.isKeepOrphanNodes = isKeepOrphanNodes;
    this.rootNodeIds = rootNodeIds == null ? ImmutableSet.of() : ImmutableSet.copyOf(rootNodeIds);
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
}

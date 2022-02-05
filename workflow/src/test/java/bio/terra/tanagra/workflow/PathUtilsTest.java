package bio.terra.tanagra.workflow;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.beam.sdk.testing.PAssert;
import org.apache.beam.sdk.testing.TestPipeline;
import org.apache.beam.sdk.transforms.Create;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollection;
import org.junit.Rule;
import org.junit.Test;

public class PathUtilsTest {
  @Rule public final transient TestPipeline pipeline = TestPipeline.create();

  // list of all nodes for a graph with only one child per parent
  private static final List<Long> NO_SIBLINGS_ALLNODES = List.of(11L, 21L, 31L, 12L, 22L, 13L);

  // list of parent-child relationships for a graph with only one child per parent
  private static Multimap<Long, Long> noSiblingsParentChildRelationships() {
    Multimap<Long, Long> parentChildRelationships =
        MultimapBuilder.hashKeys().arrayListValues().build();
    parentChildRelationships.putAll(11L, List.of(21L));
    parentChildRelationships.putAll(21L, List.of(31L));
    parentChildRelationships.putAll(12L, List.of(22L));
    return parentChildRelationships;
  }

  // maximum path length for a graph with only one child per parent
  private static final int NO_SIBLINGS_MAXPATHLENGTH = 4;

  // list of all nodes for a graph with multiple children per parent
  private static final List<Long> HAS_SIBLINGS_ALLNODES =
      List.of(10L, 11L, 20L, 21L, 31L, 12L, 22L, 13L);

  // list of parent-child relationships for a graph with multiple children per parent
  private static Multimap<Long, Long> hasSiblingsParentChildRelationships() {
    Multimap<Long, Long> parentChildRelationships =
        MultimapBuilder.hashKeys().arrayListValues().build();
    parentChildRelationships.putAll(10L, List.of(21L));
    parentChildRelationships.putAll(11L, List.of(21L));
    parentChildRelationships.putAll(20L, List.of(31L));
    parentChildRelationships.putAll(21L, List.of(31L));
    parentChildRelationships.putAll(12L, List.of(22L));
    return parentChildRelationships;
  }

  // maximum path length for a graph with multiple children per parent
  private static final int HAS_SIBLINGS_MAXPATHLENGTH = 4;

  @Test
  public void noSiblingsPaths() {
    Multimap<Long, String> expectedPaths = MultimapBuilder.hashKeys().arrayListValues().build();
    expectedPaths.put(11L, "21.31");
    expectedPaths.put(21L, "31");
    expectedPaths.put(31L, "");
    expectedPaths.put(12L, "22");
    expectedPaths.put(22L, "");
    expectedPaths.put(13L, "");

    runComputePathsAndAssert(
        NO_SIBLINGS_ALLNODES,
        noSiblingsParentChildRelationships(),
        expectedPaths,
        NO_SIBLINGS_MAXPATHLENGTH);
  }

  @Test
  public void hasSiblingsPaths() {
    Multimap<Long, String> expectedPaths = MultimapBuilder.hashKeys().arrayListValues().build();
    expectedPaths.put(10L, "21.31");
    expectedPaths.put(11L, "21.31");
    expectedPaths.put(20L, "31");
    expectedPaths.put(21L, "31");
    expectedPaths.put(31L, "");
    expectedPaths.put(12L, "22");
    expectedPaths.put(22L, "");
    expectedPaths.put(13L, "");

    runComputePathsAndAssert(
        HAS_SIBLINGS_ALLNODES,
        hasSiblingsParentChildRelationships(),
        expectedPaths,
        HAS_SIBLINGS_MAXPATHLENGTH);
  }

  /**
   * Run a test {@link PathUtils#computePaths} pipeline with the input nodes and parent-child
   * relationships. Assert that the expected paths are returned.
   */
  void runComputePathsAndAssert(
      List<Long> allNodes,
      Multimap<Long, Long> parentChildRelationships,
      Multimap<Long, String> expectedPaths,
      int maxPathLength) {
    PCollection<Long> allNodesPC =
        pipeline.apply("create all nodes pcollection", Create.of(allNodes));
    PCollection<KV<Long, Long>> parentChildRelationshipsPC =
        pipeline.apply(
            "create parent-child kv pairs pcollection",
            Create.of(convertToKvs(parentChildRelationships)));

    PCollection<KV<Long, String>> nodePaths =
        PathUtils.computePaths(allNodesPC, parentChildRelationshipsPC, maxPathLength);

    PAssert.that(nodePaths).containsInAnyOrder(convertToKvs(expectedPaths));
    pipeline.run().waitUntilFinish();
  }

  @Test
  public void noSiblingsNumChildren() {
    Multimap<Long, Long> expectedNumChildren = MultimapBuilder.hashKeys().arrayListValues().build();
    expectedNumChildren.put(11L, 0L);
    expectedNumChildren.put(21L, 1L);
    expectedNumChildren.put(31L, 1L);
    expectedNumChildren.put(12L, 0L);
    expectedNumChildren.put(22L, 1L);
    expectedNumChildren.put(13L, 0L);

    runCountChildrenAndAssert(
        NO_SIBLINGS_ALLNODES, noSiblingsParentChildRelationships(), expectedNumChildren);
  }

  @Test
  public void hasSiblingsNumChildren() {
    Multimap<Long, Long> expectedNumChildren = MultimapBuilder.hashKeys().arrayListValues().build();
    expectedNumChildren.put(10L, 0L);
    expectedNumChildren.put(11L, 0L);
    expectedNumChildren.put(20L, 0L);
    expectedNumChildren.put(21L, 2L);
    expectedNumChildren.put(31L, 2L);
    expectedNumChildren.put(12L, 0L);
    expectedNumChildren.put(22L, 1L);
    expectedNumChildren.put(13L, 0L);

    runCountChildrenAndAssert(
        HAS_SIBLINGS_ALLNODES, hasSiblingsParentChildRelationships(), expectedNumChildren);
  }

  /**
   * Run a test {@link PathUtils#countChildren} pipeline with the parent-child relationships. Assert
   * that the expected number of children are returned.
   */
  void runCountChildrenAndAssert(
      List<Long> allNodes,
      Multimap<Long, Long> parentChildRelationships,
      Multimap<Long, Long> expectedNumChildren) {
    PCollection<Long> allNodesPC =
        pipeline.apply("create all nodes pcollection", Create.of(allNodes));
    PCollection<KV<Long, Long>> parentChildRelationshipsPC =
        pipeline.apply(
            "create parent-child kv pairs pcollection",
            Create.of(convertToKvs(parentChildRelationships)));

    PCollection<KV<Long, Long>> nodeNumChildren =
        PathUtils.countChildren(allNodesPC, parentChildRelationshipsPC);
    PAssert.that(nodeNumChildren).containsInAnyOrder(convertToKvs(expectedNumChildren));

    pipeline.run().waitUntilFinish();
  }

  @Test
  public void noSiblingsPrunedPaths() {
    Multimap<Long, String> expectedPrunedPaths =
        MultimapBuilder.hashKeys().arrayListValues().build();
    expectedPrunedPaths.put(11L, "21.31");
    expectedPrunedPaths.put(21L, "31");
    expectedPrunedPaths.put(31L, "");
    expectedPrunedPaths.put(12L, "22");
    expectedPrunedPaths.put(22L, "");
    expectedPrunedPaths.put(13L, null);

    runPruneOrphanPathsAndAssert(
        NO_SIBLINGS_ALLNODES,
        noSiblingsParentChildRelationships(),
        NO_SIBLINGS_MAXPATHLENGTH,
        expectedPrunedPaths);
  }

  @Test
  public void hasSiblingsPrunedPaths() {
    Multimap<Long, String> expectedPrunedPaths =
        MultimapBuilder.hashKeys().arrayListValues().build();
    expectedPrunedPaths.put(10L, "21.31");
    expectedPrunedPaths.put(11L, "21.31");
    expectedPrunedPaths.put(20L, "31");
    expectedPrunedPaths.put(21L, "31");
    expectedPrunedPaths.put(31L, "");
    expectedPrunedPaths.put(12L, "22");
    expectedPrunedPaths.put(22L, "");
    expectedPrunedPaths.put(13L, null);

    runPruneOrphanPathsAndAssert(
        HAS_SIBLINGS_ALLNODES,
        hasSiblingsParentChildRelationships(),
        HAS_SIBLINGS_MAXPATHLENGTH,
        expectedPrunedPaths);
  }

  /**
   * Run a test {@link PathUtils#pruneOrphanPaths} pipeline with the input nodes and parent-child
   * relationships. Assert that the expected path and number of children are returned.
   */
  void runPruneOrphanPathsAndAssert(
      List<Long> allNodes,
      Multimap<Long, Long> parentChildRelationships,
      int maxPathLength,
      Multimap<Long, String> expectedPrunedPaths) {
    PCollection<Long> allNodesPC =
        pipeline.apply("create all nodes pcollection", Create.of(allNodes));
    PCollection<KV<Long, Long>> parentChildRelationshipsPC =
        pipeline.apply(
            "create parent-child kv pairs pcollection",
            Create.of(convertToKvs(parentChildRelationships)));

    PCollection<KV<Long, String>> nodePaths =
        PathUtils.computePaths(allNodesPC, parentChildRelationshipsPC, maxPathLength);
    PCollection<KV<Long, Long>> nodeNumChildren =
        PathUtils.countChildren(allNodesPC, parentChildRelationshipsPC);
    PCollection<KV<Long, String>> nodePathAndNumChildren =
        PathUtils.pruneOrphanPaths(nodePaths, nodeNumChildren);

    PAssert.that(nodePathAndNumChildren).containsInAnyOrder(convertToKvs(expectedPrunedPaths));

    pipeline.run().waitUntilFinish();
  }

  /** Convert a {@link Multimap} to an equivalent list of {@link KV}s. */
  private static <K, V> List<KV<K, V>> convertToKvs(Multimap<K, V> multimap) {
    return multimap.entries().stream()
        .map(entry -> KV.of(entry.getKey(), entry.getValue()))
        .collect(Collectors.toList());
  }
}

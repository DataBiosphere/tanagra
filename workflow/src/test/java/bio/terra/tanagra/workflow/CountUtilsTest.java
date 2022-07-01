package bio.terra.tanagra.workflow;

import bio.terra.tanagra.workflow.utils.KVUtils;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import java.util.List;
import javax.annotation.Nullable;
import org.apache.beam.sdk.testing.PAssert;
import org.apache.beam.sdk.testing.TestPipeline;
import org.apache.beam.sdk.transforms.Create;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollection;
import org.junit.Rule;
import org.junit.Test;

public class CountUtilsTest {
  @Rule public final transient TestPipeline pipeline = TestPipeline.create();

  // list of (5) all nodes
  private static final List<Long> ALL_NODES_5 = List.of(10L, 11L, 12L, 13L, 14L);

  // list of (10) all nodes
  private static final List<Long> ALL_NODES_10 =
      List.of(10L, 11L, 12L, 13L, 14L, 15L, 16L, 17L, 18L, 19L);

  @Test
  public void allNodesHaveNonZeroCount() {
    Multimap<Long, Long> auxiliaryNodes = MultimapBuilder.hashKeys().arrayListValues().build();
    auxiliaryNodes.putAll(10L, List.of(20L));
    auxiliaryNodes.putAll(11L, List.of(21L, 22L));
    auxiliaryNodes.putAll(12L, List.of(22L));
    auxiliaryNodes.putAll(13L, List.of(23L, 23L));
    auxiliaryNodes.putAll(14L, List.of(24L, 25L, 26L));

    Multimap<Long, Long> expectedCounts = MultimapBuilder.hashKeys().arrayListValues().build();
    expectedCounts.put(10L, 1L);
    expectedCounts.put(11L, 2L);
    expectedCounts.put(12L, 1L);
    expectedCounts.put(13L, 1L);
    expectedCounts.put(14L, 3L);

    runCountDistinctAndAssert(ALL_NODES_5, auxiliaryNodes, expectedCounts);
  }

  @Test
  public void someNodesHaveZeroCount() {
    Multimap<Long, Long> auxiliaryNodes = MultimapBuilder.hashKeys().arrayListValues().build();
    auxiliaryNodes.putAll(10L, List.of(20L));
    auxiliaryNodes.putAll(11L, List.of(21L, 22L));
    auxiliaryNodes.putAll(13L, List.of(23L, 23L));
    auxiliaryNodes.putAll(14L, List.of(24L, 25L, 26L));

    Multimap<Long, Long> expectedCounts = MultimapBuilder.hashKeys().arrayListValues().build();
    expectedCounts.put(10L, 1L);
    expectedCounts.put(11L, 2L);
    expectedCounts.put(12L, 0L);
    expectedCounts.put(13L, 1L);
    expectedCounts.put(14L, 3L);

    runCountDistinctAndAssert(ALL_NODES_5, auxiliaryNodes, expectedCounts);
  }

  @Test
  public void someAuxiliaryNodesPointToNonExistentPrimaryNodes() {
    Multimap<Long, Long> auxiliaryNodes = MultimapBuilder.hashKeys().arrayListValues().build();
    auxiliaryNodes.putAll(10L, List.of(20L));
    auxiliaryNodes.putAll(11L, List.of(21L, 22L));
    auxiliaryNodes.putAll(13L, List.of(23L, 23L));
    auxiliaryNodes.putAll(14L, List.of(24L, 25L, 26L));
    auxiliaryNodes.putAll(15L, List.of(26L));

    Multimap<Long, Long> expectedCounts = MultimapBuilder.hashKeys().arrayListValues().build();
    expectedCounts.put(10L, 1L);
    expectedCounts.put(11L, 2L);
    expectedCounts.put(12L, 0L);
    expectedCounts.put(13L, 1L);
    expectedCounts.put(14L, 3L);

    runCountDistinctAndAssert(ALL_NODES_5, auxiliaryNodes, expectedCounts);
  }

  @Test
  public void hierarchyWithOneLevelAndSelfReference() {
    Multimap<Long, Long> auxiliaryNodes = MultimapBuilder.hashKeys().arrayListValues().build();
    auxiliaryNodes.putAll(10L, List.of(20L)); // 1
    auxiliaryNodes.putAll(11L, List.of(21L, 22L)); // 2
    auxiliaryNodes.putAll(13L, List.of(23L, 23L)); // 1
    auxiliaryNodes.putAll(14L, List.of(24L, 25L, 26L)); // 3

    Multimap<Long, Long> descendantAncestor = MultimapBuilder.hashKeys().arrayListValues().build();
    descendantAncestor.putAll(10L, List.of(14L));
    descendantAncestor.putAll(11L, List.of(14L));
    descendantAncestor.putAll(12L, List.of(14L));
    descendantAncestor.putAll(13L, List.of(14L));
    descendantAncestor.putAll(14L, List.of(14L)); // include a self-reference

    Multimap<Long, Long> expectedCounts = MultimapBuilder.hashKeys().arrayListValues().build();
    expectedCounts.put(10L, 1L);
    expectedCounts.put(11L, 2L);
    expectedCounts.put(12L, 0L);
    expectedCounts.put(13L, 1L);
    expectedCounts.put(14L, 7L);

    runCountDistinctAndAggregateForHierarchyAndAssert(
        ALL_NODES_5, auxiliaryNodes, descendantAncestor, expectedCounts);
  }

  @Test
  public void hierarchyWithTwoLayersAndUnconnectedNodes() {
    Multimap<Long, Long> auxiliaryNodes = MultimapBuilder.hashKeys().arrayListValues().build();
    auxiliaryNodes.putAll(10L, List.of(20L)); // 1
    auxiliaryNodes.putAll(11L, List.of(21L, 22L)); // 2
    auxiliaryNodes.putAll(13L, List.of(23L, 23L)); // 1
    auxiliaryNodes.putAll(14L, List.of(24L, 25L, 26L)); // 3
    auxiliaryNodes.putAll(15L, List.of(26L)); // 1
    auxiliaryNodes.putAll(16L, List.of(27L, 28L, 29L)); // 3
    auxiliaryNodes.putAll(17L, List.of(27L, 27L, 27L, 24L)); // 2
    auxiliaryNodes.putAll(18L, List.of(28L, 29L)); // 2
    auxiliaryNodes.putAll(19L, List.of(28L)); // 1

    Multimap<Long, Long> descendantAncestor = MultimapBuilder.hashKeys().arrayListValues().build();
    descendantAncestor.putAll(10L, List.of(13L, 14L));
    descendantAncestor.putAll(11L, List.of(13L, 14L));
    descendantAncestor.putAll(12L, List.of(13L, 14L));
    descendantAncestor.putAll(13L, List.of(14L));
    descendantAncestor.putAll(16L, List.of(14L));

    Multimap<Long, Long> expectedCounts = MultimapBuilder.hashKeys().arrayListValues().build();
    expectedCounts.put(10L, 1L);
    expectedCounts.put(11L, 2L);
    expectedCounts.put(12L, 0L);
    expectedCounts.put(13L, 4L);
    expectedCounts.put(14L, 10L);
    expectedCounts.put(15L, 1L);
    expectedCounts.put(16L, 3L);
    expectedCounts.put(17L, 2L);
    expectedCounts.put(18L, 2L);
    expectedCounts.put(19L, 1L);

    runCountDistinctAndAggregateForHierarchyAndAssert(
        ALL_NODES_10, auxiliaryNodes, descendantAncestor, expectedCounts);
  }

  /**
   * Run a test {@link CountUtils#countDistinct(PCollection, PCollection)} pipeline with the input
   * primary and auxiliary nodes. Use this method for primary nodes that DO NOT have a hierarchy.
   * Assert that the expected counts are returned.
   */
  void runCountDistinctAndAssert(
      List<Long> allNodes,
      Multimap<Long, Long> auxiliaryNodes,
      Multimap<Long, Long> expectedCounts) {
    runCountDistinctAndAggregateForHierarchyAndAssert(
        allNodes, auxiliaryNodes, null, expectedCounts);
  }

  /**
   * Run a test {@link CountUtils#countDistinct(PCollection, PCollection)} pipeline with the input
   * primary and auxiliary nodes, and the descendant-ancestor pairs for the hierarchy on the primary
   * node. Use this method for primary nodes that DO have a hierarchy. Assert that the expected
   * counts are returned.
   */
  void runCountDistinctAndAggregateForHierarchyAndAssert(
      List<Long> allNodes,
      Multimap<Long, Long> auxiliaryNodes,
      @Nullable Multimap<Long, Long> descendantAncestors,
      Multimap<Long, Long> expectedCounts) {
    PCollection<Long> allNodesPC =
        pipeline.apply("create all nodes pcollection", Create.of(allNodes));
    PCollection<KV<Long, Long>> auxiliaryNodesPC =
        pipeline.apply(
            "create node-secondary kv pairs pcollection",
            Create.of(KVUtils.convertToKvs(auxiliaryNodes)));

    PCollection<KV<Long, Long>> nodeCounts = CountUtils.countDistinct(allNodesPC, auxiliaryNodesPC);

    if (descendantAncestors == null) {
      PAssert.that(nodeCounts).containsInAnyOrder(KVUtils.convertToKvs(expectedCounts));
    } else {
      PCollection<KV<Long, Long>> descendantAncestorPC =
          pipeline.apply(
              "create descendant-ancestor kv pairs pcollection",
              Create.of(KVUtils.convertToKvs(descendantAncestors)));

      PCollection<KV<Long, Long>> nodeCountsAggregated =
          CountUtils.aggregateCountsInHierarchy(nodeCounts, descendantAncestorPC);

      PAssert.that(nodeCountsAggregated).containsInAnyOrder(KVUtils.convertToKvs(expectedCounts));
    }
    pipeline.run().waitUntilFinish();
  }
}

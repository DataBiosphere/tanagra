package bio.terra.tanagra.indexing.job.bigquery.beam;

import bio.terra.tanagra.indexing.job.dataflow.beam.CountUtils;
import bio.terra.tanagra.testing.KVUtils;
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
    Multimap<Long, Long> occurrences = MultimapBuilder.hashKeys().arrayListValues().build();
    occurrences.putAll(10L, List.of(20L));
    occurrences.putAll(11L, List.of(21L, 22L));
    occurrences.putAll(12L, List.of(22L));
    occurrences.putAll(13L, List.of(23L, 23L));
    occurrences.putAll(14L, List.of(24L, 25L, 26L));

    Multimap<Long, Long> expectedCounts = MultimapBuilder.hashKeys().arrayListValues().build();
    expectedCounts.put(10L, 1L);
    expectedCounts.put(11L, 2L);
    expectedCounts.put(12L, 1L);
    expectedCounts.put(13L, 1L);
    expectedCounts.put(14L, 3L);

    runCountDistinctAndAssert(ALL_NODES_5, occurrences, expectedCounts);
  }

  @Test
  public void someNodesHaveZeroCount() {
    Multimap<Long, Long> occurrences = MultimapBuilder.hashKeys().arrayListValues().build();
    occurrences.putAll(10L, List.of(20L));
    occurrences.putAll(11L, List.of(21L, 22L));
    occurrences.putAll(13L, List.of(23L, 23L));
    occurrences.putAll(14L, List.of(24L, 25L, 26L));

    Multimap<Long, Long> expectedCounts = MultimapBuilder.hashKeys().arrayListValues().build();
    expectedCounts.put(10L, 1L);
    expectedCounts.put(11L, 2L);
    expectedCounts.put(12L, 0L);
    expectedCounts.put(13L, 1L);
    expectedCounts.put(14L, 3L);

    runCountDistinctAndAssert(ALL_NODES_5, occurrences, expectedCounts);
  }

  @Test
  public void someOccurrencesPointToNonExistentPrimaryNodes() {
    Multimap<Long, Long> occurrences = MultimapBuilder.hashKeys().arrayListValues().build();
    occurrences.putAll(10L, List.of(20L));
    occurrences.putAll(11L, List.of(21L, 22L));
    occurrences.putAll(13L, List.of(23L, 23L));
    occurrences.putAll(14L, List.of(24L, 25L, 26L));
    occurrences.putAll(15L, List.of(26L));

    Multimap<Long, Long> expectedCounts = MultimapBuilder.hashKeys().arrayListValues().build();
    expectedCounts.put(10L, 1L);
    expectedCounts.put(11L, 2L);
    expectedCounts.put(12L, 0L);
    expectedCounts.put(13L, 1L);
    expectedCounts.put(14L, 3L);

    runCountDistinctAndAssert(ALL_NODES_5, occurrences, expectedCounts);
  }

  @Test
  public void hierarchyWithOneLevelAndSelfReference() {
    Multimap<Long, Long> occurrences = MultimapBuilder.hashKeys().arrayListValues().build();
    occurrences.putAll(10L, List.of(20L)); // 1
    occurrences.putAll(11L, List.of(21L, 22L)); // 2
    occurrences.putAll(13L, List.of(23L, 23L)); // 1
    occurrences.putAll(14L, List.of(24L, 25L, 26L)); // 3

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

    runCountDistinctWithHierarchyAndAssert(
        ALL_NODES_5, occurrences, descendantAncestor, expectedCounts);
  }

  @Test
  public void hierarchyWithTwoLayersAndUnconnectedNodes() {
    Multimap<Long, Long> occurrences = MultimapBuilder.hashKeys().arrayListValues().build();
    occurrences.putAll(10L, List.of(20L)); // 1
    occurrences.putAll(11L, List.of(21L, 22L)); // 2
    occurrences.putAll(13L, List.of(23L, 23L)); // 1
    occurrences.putAll(14L, List.of(24L, 25L, 26L)); // 3
    occurrences.putAll(15L, List.of(26L)); // 1
    occurrences.putAll(16L, List.of(27L, 28L, 29L)); // 3
    occurrences.putAll(17L, List.of(27L, 27L, 27L, 24L)); // 2
    occurrences.putAll(18L, List.of(28L, 29L)); // 2
    occurrences.putAll(19L, List.of(28L)); // 1

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

    runCountDistinctWithHierarchyAndAssert(
        ALL_NODES_10, occurrences, descendantAncestor, expectedCounts);
  }

  @Test
  public void hierarchyWithRepeatedValuesInDifferentChildren() {
    Multimap<Long, Long> occurrences = MultimapBuilder.hashKeys().arrayListValues().build();
    occurrences.putAll(10L, List.of(20L)); // 1
    occurrences.putAll(11L, List.of(20L, 22L)); // 2
    occurrences.putAll(13L, List.of(23L, 23L)); // 1
    occurrences.putAll(14L, List.of(20L, 24L, 25L, 26L)); // 4

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
    expectedCounts.put(14L, 6L);

    runCountDistinctWithHierarchyAndAssert(
        ALL_NODES_5, occurrences, descendantAncestor, expectedCounts);
  }

  /**
   * Run a test {@link CountUtils#countDistinct(PCollection, PCollection)} pipeline with the input
   * primary nodes and occurrences. Use this method for primary nodes that DO NOT have a hierarchy.
   * Assert that the expected counts are returned.
   */
  void runCountDistinctAndAssert(
      List<Long> primaryNodes,
      Multimap<Long, Long> occurrences,
      Multimap<Long, Long> expectedCounts) {
    runCountDistinctWithHierarchyAndAssert(primaryNodes, occurrences, null, expectedCounts);
  }

  /**
   * Run a test {@link CountUtils#countDistinct(PCollection, PCollection)} pipeline with the input
   * primary nodes and occurrences, and the descendant-ancestor pairs for the hierarchy on the
   * primary nodes. Use this method for primary nodes that DO have a hierarchy. Assert that the
   * expected counts are returned.
   */
  void runCountDistinctWithHierarchyAndAssert(
      List<Long> primaryNodes,
      Multimap<Long, Long> occurrences,
      @Nullable Multimap<Long, Long> descendantAncestors,
      Multimap<Long, Long> expectedCounts) {
    PCollection<Long> allNodesPC =
        pipeline.apply("create all nodes pcollection", Create.of(primaryNodes));
    PCollection<KV<Long, Long>> occurrencesPC =
        pipeline.apply(
            "create node-secondary kv pairs pcollection",
            Create.of(KVUtils.convertToKvs(occurrences)));

    if (descendantAncestors != null) {
      PCollection<KV<Long, Long>> descendantAncestorPC =
          pipeline.apply(
              "create descendant-ancestor kv pairs pcollection",
              Create.of(KVUtils.convertToKvs(descendantAncestors)));

      occurrencesPC = CountUtils.repeatOccurrencesForHierarchy(occurrencesPC, descendantAncestorPC);
    }

    PCollection<KV<Long, Long>> nodeCounts = CountUtils.countDistinct(allNodesPC, occurrencesPC);
    PAssert.that(nodeCounts).containsInAnyOrder(KVUtils.convertToKvs(expectedCounts));

    pipeline.run().waitUntilFinish();
  }
}

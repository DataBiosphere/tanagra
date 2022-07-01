package bio.terra.tanagra.workflow;

import bio.terra.tanagra.workflow.utils.KVUtils;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import java.util.List;
import org.apache.beam.sdk.testing.PAssert;
import org.apache.beam.sdk.testing.TestPipeline;
import org.apache.beam.sdk.transforms.Create;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollection;
import org.junit.Rule;
import org.junit.Test;

public class CountUtilsTest {
  @Rule public final transient TestPipeline pipeline = TestPipeline.create();

  // list of all nodes
  private static final List<Long> ALL_NODES = List.of(10L, 11L, 12L, 13L, 14L);

  @Test
  public void allNodesHaveNonZeroCount() {
    Multimap<Long, Long> auxiliaryNodes = MultimapBuilder.hashKeys().arrayListValues().build();
    auxiliaryNodes.putAll(10L, List.of(20L));
    auxiliaryNodes.putAll(11L, List.of(21L));
    auxiliaryNodes.putAll(11L, List.of(22L));
    auxiliaryNodes.putAll(12L, List.of(22L));
    auxiliaryNodes.putAll(13L, List.of(23L));
    auxiliaryNodes.putAll(13L, List.of(23L));
    auxiliaryNodes.putAll(14L, List.of(24L));
    auxiliaryNodes.putAll(14L, List.of(25L));
    auxiliaryNodes.putAll(14L, List.of(26L));

    Multimap<Long, Long> expectedCounts = MultimapBuilder.hashKeys().arrayListValues().build();
    expectedCounts.put(10L, 1L);
    expectedCounts.put(11L, 2L);
    expectedCounts.put(12L, 1L);
    expectedCounts.put(13L, 1L);
    expectedCounts.put(14L, 3L);

    runCountDistinctAndAssert(ALL_NODES, auxiliaryNodes, expectedCounts);
  }

  @Test
  public void someNodesHaveZeroCount() {
    Multimap<Long, Long> auxiliaryNodes = MultimapBuilder.hashKeys().arrayListValues().build();
    auxiliaryNodes.putAll(10L, List.of(20L));
    auxiliaryNodes.putAll(11L, List.of(21L));
    auxiliaryNodes.putAll(11L, List.of(22L));
    auxiliaryNodes.putAll(13L, List.of(23L));
    auxiliaryNodes.putAll(13L, List.of(23L));
    auxiliaryNodes.putAll(14L, List.of(24L));
    auxiliaryNodes.putAll(14L, List.of(25L));
    auxiliaryNodes.putAll(14L, List.of(26L));

    Multimap<Long, Long> expectedCounts = MultimapBuilder.hashKeys().arrayListValues().build();
    expectedCounts.put(10L, 1L);
    expectedCounts.put(11L, 2L);
    expectedCounts.put(12L, 0L);
    expectedCounts.put(13L, 1L);
    expectedCounts.put(14L, 3L);

    runCountDistinctAndAssert(ALL_NODES, auxiliaryNodes, expectedCounts);
  }

  @Test
  public void someAuxiliaryNodesPointToNonExistentPrimaryNodes() {
    Multimap<Long, Long> auxiliaryNodes = MultimapBuilder.hashKeys().arrayListValues().build();
    auxiliaryNodes.putAll(10L, List.of(20L));
    auxiliaryNodes.putAll(11L, List.of(21L));
    auxiliaryNodes.putAll(11L, List.of(22L));
    auxiliaryNodes.putAll(13L, List.of(23L));
    auxiliaryNodes.putAll(13L, List.of(23L));
    auxiliaryNodes.putAll(14L, List.of(24L));
    auxiliaryNodes.putAll(14L, List.of(25L));
    auxiliaryNodes.putAll(14L, List.of(26L));
    auxiliaryNodes.putAll(15L, List.of(26L));

    Multimap<Long, Long> expectedCounts = MultimapBuilder.hashKeys().arrayListValues().build();
    expectedCounts.put(10L, 1L);
    expectedCounts.put(11L, 2L);
    expectedCounts.put(12L, 0L);
    expectedCounts.put(13L, 1L);
    expectedCounts.put(14L, 3L);

    runCountDistinctAndAssert(ALL_NODES, auxiliaryNodes, expectedCounts);
  }

  /**
   * Run a test {@link CountUtils#countDistinct(PCollection, PCollection)} pipeline with the input
   * primary and auxiliary nodes. Assert that the expected counts are returned.
   */
  void runCountDistinctAndAssert(
      List<Long> allNodes,
      Multimap<Long, Long> auxiliaryNodes,
      Multimap<Long, Long> expectedCounts) {
    PCollection<Long> allNodesPC =
        pipeline.apply("create all nodes pcollection", Create.of(allNodes));
    PCollection<KV<Long, Long>> auxiliaryNodesPC =
        pipeline.apply(
            "create node-secondary kv pairs pcollection",
            Create.of(KVUtils.convertToKvs(auxiliaryNodes)));

    PCollection<KV<Long, Long>> nodeCounts = CountUtils.countDistinct(allNodesPC, auxiliaryNodesPC);

    PAssert.that(nodeCounts).containsInAnyOrder(KVUtils.convertToKvs(expectedCounts));
    pipeline.run().waitUntilFinish();
  }
}

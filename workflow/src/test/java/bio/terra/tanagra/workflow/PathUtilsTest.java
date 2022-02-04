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

  /** Convert a {@link Multimap} to an equivalent list of {@link KV}s. */
  private static <K, V> List<KV<K, V>> convertToKvs(Multimap<K, V> multimap) {
    return multimap.entries().stream()
        .map(entry -> KV.of(entry.getKey(), entry.getValue()))
        .collect(Collectors.toList());
  }

  @Test
  public void onlyOneChildPerParent() {
    final List<Long> allNodes = List.of(11L, 21L, 31L, 12L, 22L, 13L);

    Multimap<Long, Long> parentChildRelationships =
        MultimapBuilder.hashKeys().arrayListValues().build();
    parentChildRelationships.putAll(11L, List.of(21L));
    parentChildRelationships.putAll(21L, List.of(31L));
    parentChildRelationships.putAll(12L, List.of(22L));

    Multimap<Long, String> expectedPaths = MultimapBuilder.hashKeys().arrayListValues().build();
    expectedPaths.put(11L, "21.31");
    expectedPaths.put(21L, "31");
    expectedPaths.put(31L, "");
    expectedPaths.put(12L, "22");
    expectedPaths.put(22L, "");
    expectedPaths.put(13L, "");

    runComputePathsAndAssert(allNodes, parentChildRelationships, expectedPaths, 4);
  }

  @Test
  public void multipleChildrenPerParent() {
    final List<Long> allNodes = List.of(10L, 11L, 20L, 21L, 31L, 12L, 22L, 13L);

    Multimap<Long, Long> parentChildRelationships =
        MultimapBuilder.hashKeys().arrayListValues().build();
    parentChildRelationships.putAll(10L, List.of(21L));
    parentChildRelationships.putAll(11L, List.of(21L));
    parentChildRelationships.putAll(20L, List.of(31L));
    parentChildRelationships.putAll(21L, List.of(31L));
    parentChildRelationships.putAll(12L, List.of(22L));

    Multimap<Long, String> expectedPaths = MultimapBuilder.hashKeys().arrayListValues().build();
    expectedPaths.put(10L, "21.31");
    expectedPaths.put(11L, "21.31");
    expectedPaths.put(20L, "31");
    expectedPaths.put(21L, "31");
    expectedPaths.put(31L, "");
    expectedPaths.put(12L, "22");
    expectedPaths.put(22L, "");
    expectedPaths.put(13L, "");

    runComputePathsAndAssert(allNodes, parentChildRelationships, expectedPaths, 4);
  }
}

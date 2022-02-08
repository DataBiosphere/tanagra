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

public class TextSearchUtilsTest {
  @Rule public final transient TestPipeline pipeline = TestPipeline.create();

  // list of all nodes
  final List<Long> ALL_NODES = List.of(10L, 11L, 12L, 13L, 14L);

  @Test
  public void allNodesHaveAtLeastOneSearchString() {
    Multimap<Long, String> searchStrings = MultimapBuilder.hashKeys().arrayListValues().build();
    searchStrings.putAll(10L, List.of("apple", "red fruit"));
    searchStrings.putAll(11L, List.of("peach", "fruit from georgia"));
    searchStrings.putAll(12L, List.of("orange"));
    searchStrings.putAll(13L, List.of("lemon", "myers lemon"));
    searchStrings.putAll(14L, List.of("pear"));

    Multimap<Long, String> expectedSingleSearchStrings =
        MultimapBuilder.hashKeys().arrayListValues().build();
    expectedSingleSearchStrings.put(10L, "apple , red fruit");
    expectedSingleSearchStrings.put(11L, "fruit from georgia , peach");
    expectedSingleSearchStrings.put(12L, "orange");
    expectedSingleSearchStrings.put(13L, "lemon , myers lemon");
    expectedSingleSearchStrings.put(14L, "pear");

    runBuildSingleSearchStringAndAssert(ALL_NODES, searchStrings, expectedSingleSearchStrings);
  }

  @Test
  public void someNodesHaveNoSearchString() {
    Multimap<Long, String> searchStrings = MultimapBuilder.hashKeys().arrayListValues().build();
    searchStrings.putAll(10L, List.of("apple", "red fruit"));
    searchStrings.putAll(11L, List.of("peach", "fruit from georgia"));
    searchStrings.putAll(13L, List.of("lemon", "myers lemon"));

    Multimap<Long, String> expectedSingleSearchStrings =
        MultimapBuilder.hashKeys().arrayListValues().build();
    expectedSingleSearchStrings.put(10L, "apple , red fruit");
    expectedSingleSearchStrings.put(11L, "fruit from georgia , peach");
    expectedSingleSearchStrings.put(12L, "");
    expectedSingleSearchStrings.put(13L, "lemon , myers lemon");
    expectedSingleSearchStrings.put(14L, "");

    runBuildSingleSearchStringAndAssert(ALL_NODES, searchStrings, expectedSingleSearchStrings);
  }

  @Test
  public void someSearchStringsHaveNoNode() {
    Multimap<Long, String> searchStrings = MultimapBuilder.hashKeys().arrayListValues().build();
    searchStrings.putAll(10L, List.of("apple", "red fruit"));
    searchStrings.putAll(11L, List.of("peach", "fruit from georgia"));
    searchStrings.putAll(12L, List.of("orange"));
    searchStrings.putAll(13L, List.of("lemon", "myers lemon"));
    searchStrings.putAll(14L, List.of("pear"));
    searchStrings.putAll(15L, List.of("lime , corona garnish"));

    Multimap<Long, String> expectedSingleSearchStrings =
        MultimapBuilder.hashKeys().arrayListValues().build();
    expectedSingleSearchStrings.put(10L, "apple , red fruit");
    expectedSingleSearchStrings.put(11L, "fruit from georgia , peach");
    expectedSingleSearchStrings.put(12L, "orange");
    expectedSingleSearchStrings.put(13L, "lemon , myers lemon");
    expectedSingleSearchStrings.put(14L, "pear");

    runBuildSingleSearchStringAndAssert(ALL_NODES, searchStrings, expectedSingleSearchStrings);
  }

  /**
   * Run a test {@link TextSearchUtils#buildSingleSearchString(PCollection, PCollection)} pipeline
   * with the input nodes and search strings. Assert that the expected single search strings are
   * returned.
   */
  void runBuildSingleSearchStringAndAssert(
      List<Long> allNodes,
      Multimap<Long, String> searchStrings,
      Multimap<Long, String> expectedSingleSearchStrings) {
    PCollection<Long> allNodesPC =
        pipeline.apply("create all nodes pcollection", Create.of(allNodes));
    PCollection<KV<Long, String>> searchStringsPC =
        pipeline.apply(
            "create node-searchString kv pairs pcollection",
            Create.of(KVUtils.convertToKvs(searchStrings)));

    PCollection<KV<Long, String>> nodeSingleSearchStrings =
        TextSearchUtils.buildSingleSearchString(allNodesPC, searchStringsPC);

    PAssert.that(nodeSingleSearchStrings)
        .containsInAnyOrder(KVUtils.convertToKvs(expectedSingleSearchStrings));
    pipeline.run().waitUntilFinish();
  }
}

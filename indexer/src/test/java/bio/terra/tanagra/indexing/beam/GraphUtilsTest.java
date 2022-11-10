package bio.terra.tanagra.indexing.beam;

import bio.terra.tanagra.indexing.job.beam.GraphUtils;
import bio.terra.tanagra.testing.KVUtils;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import java.util.List;
import org.apache.beam.sdk.testing.PAssert;
import org.apache.beam.sdk.testing.TestPipeline;
import org.apache.beam.sdk.transforms.Create;
import org.apache.beam.sdk.transforms.Distinct;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollection;
import org.junit.Rule;
import org.junit.Test;

public class GraphUtilsTest {

  @Rule public final transient TestPipeline pipeline = TestPipeline.create();

  /**
   * Runs a test pipeline with the input edges and {@link GraphUtils#transitiveClosure}, asserting
   * the expected transitive edges are produced.
   */
  <T> void runTransitiveClosureAndAssert(
      Multimap<T, T> edges, Multimap<T, T> expectedEdges, int maxPathLength) {
    PCollection<KV<T, T>> transitiveEdges =
        GraphUtils.transitiveClosure(
                pipeline.apply(Create.of(KVUtils.convertToKvs(edges))), maxPathLength)
            .apply(Distinct.create()); // The transitiveClosure may output duplicates.
    PAssert.that(transitiveEdges).containsInAnyOrder(KVUtils.convertToKvs(expectedEdges));
    pipeline.run().waitUntilFinish();
  }

  @Test
  public void transitiveClosureMultipleHeadsAndTails() {
    // Ascii art of the graph described by edges directed left to right.
    //  /B-D\ /F
    // A     E
    //  \ C / \G
    Multimap<String, String> edges = MultimapBuilder.hashKeys().arrayListValues().build();
    edges.putAll("A", List.of("B", "C"));
    edges.putAll("B", List.of("D"));
    edges.putAll("C", List.of("E"));
    edges.putAll("D", List.of("E"));
    edges.putAll("E", List.of("F", "G"));

    Multimap<String, String> expectedEdges = MultimapBuilder.hashKeys().arrayListValues().build();
    expectedEdges.putAll("A", List.of("B", "C", "D", "E", "F", "G"));
    expectedEdges.putAll("B", List.of("D", "E", "F", "G"));
    expectedEdges.putAll("C", List.of("E", "F", "G"));
    expectedEdges.putAll("D", List.of("E", "F", "G"));
    expectedEdges.putAll("E", List.of("F", "G"));

    runTransitiveClosureAndAssert(edges, expectedEdges, 4);
  }

  @Test
  public void transitiveClosureLengthOneGraph() {
    Multimap<String, String> edges = MultimapBuilder.hashKeys().arrayListValues().build();
    edges.put("A", "B");

    Multimap<String, String> expectedEdges = MultimapBuilder.hashKeys().arrayListValues().build();
    expectedEdges.putAll("A", List.of("B"));

    runTransitiveClosureAndAssert(edges, expectedEdges, 1);
  }

  @Test
  public void transitiveClosureLengthTwoGraph() {
    Multimap<String, String> edges = MultimapBuilder.hashKeys().arrayListValues().build();
    edges.put("A", "B");
    edges.put("B", "C");

    Multimap<String, String> expectedEdges = MultimapBuilder.hashKeys().arrayListValues().build();
    expectedEdges.putAll("A", List.of("B", "C"));
    expectedEdges.putAll("B", List.of("C"));

    runTransitiveClosureAndAssert(edges, expectedEdges, 2);
  }

  @Test
  public void transitiveClosureLongChain() {
    // Ascii art of the graph described by edges directed left to right.
    // A-B-C-D-E-F-G-H-I-J-K
    //                    \L
    Multimap<String, String> edges = MultimapBuilder.hashKeys().arrayListValues().build();
    edges.put("A", "B");
    edges.put("B", "C");
    edges.put("C", "D");
    edges.put("D", "E");
    edges.put("E", "F");
    edges.put("F", "G");
    edges.put("G", "H");
    edges.put("H", "I");
    edges.put("I", "J");
    edges.putAll("J", List.of("K", "L"));

    Multimap<String, String> expectedEdges = MultimapBuilder.hashKeys().arrayListValues().build();
    expectedEdges.putAll("A", List.of("B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L"));
    expectedEdges.putAll("B", List.of("C", "D", "E", "F", "G", "H", "I", "J", "K", "L"));
    expectedEdges.putAll("C", List.of("D", "E", "F", "G", "H", "I", "J", "K", "L"));
    expectedEdges.putAll("D", List.of("E", "F", "G", "H", "I", "J", "K", "L"));
    expectedEdges.putAll("E", List.of("F", "G", "H", "I", "J", "K", "L"));
    expectedEdges.putAll("F", List.of("G", "H", "I", "J", "K", "L"));
    expectedEdges.putAll("G", List.of("H", "I", "J", "K", "L"));
    expectedEdges.putAll("H", List.of("I", "J", "K", "L"));
    expectedEdges.putAll("I", List.of("J", "K", "L"));
    expectedEdges.putAll("J", List.of("K", "L"));

    runTransitiveClosureAndAssert(edges, expectedEdges, 10);
  }

  @Test
  public void transitiveClosureLargePathLengthOk() {
    // Ascii art of the graph described by edges directed left to right.
    // A-B-C-D-E
    Multimap<String, String> edges = MultimapBuilder.hashKeys().arrayListValues().build();
    edges.put("A", "B");
    edges.put("B", "C");
    edges.put("C", "D");
    edges.put("D", "E");
    Multimap<String, String> expectedEdges = MultimapBuilder.hashKeys().arrayListValues().build();
    expectedEdges.putAll("A", List.of("B", "C", "D", "E"));
    expectedEdges.putAll("B", List.of("C", "D", "E"));
    expectedEdges.putAll("C", List.of("D", "E"));
    expectedEdges.putAll("D", List.of("E"));

    // Not an error to have maxPathLength be much greater than the longest path, just extra work.
    runTransitiveClosureAndAssert(edges, expectedEdges, 64);
  }

  @Test
  public void transitiveClosureCycle() {
    // Ascii art of the graph described by edges directed left to right.
    // A-B-C-D-E-A... cycle
    Multimap<String, String> edges = MultimapBuilder.hashKeys().arrayListValues().build();
    edges.put("A", "B");
    edges.put("B", "C");
    edges.put("C", "D");
    edges.put("D", "E");
    edges.put("E", "A");
    Multimap<String, String> expectedEdges = MultimapBuilder.hashKeys().arrayListValues().build();
    expectedEdges.putAll("A", List.of("A", "B", "C", "D", "E"));
    expectedEdges.putAll("B", List.of("A", "B", "C", "D", "E"));
    expectedEdges.putAll("C", List.of("A", "B", "C", "D", "E"));
    expectedEdges.putAll("D", List.of("A", "B", "C", "D", "E"));
    expectedEdges.putAll("E", List.of("A", "B", "C", "D", "E"));

    runTransitiveClosureAndAssert(edges, expectedEdges, 5);
  }
}

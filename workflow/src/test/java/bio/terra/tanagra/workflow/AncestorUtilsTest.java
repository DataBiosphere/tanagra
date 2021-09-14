package bio.terra.tanagra.workflow;

import java.util.List;
import org.apache.beam.sdk.testing.PAssert;
import org.apache.beam.sdk.testing.TestPipeline;
import org.apache.beam.sdk.transforms.Create;
import org.apache.beam.sdk.transforms.Create.Values;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollection;
import org.junit.Rule;
import org.junit.Test;

public class AncestorUtilsTest {

  @Rule public final transient TestPipeline pipeline = TestPipeline.create();

  //  /B-D\
  // A     E-F
  //  \C-/  \G
  List<KV<String, String>> x = List.of(
      KV.of("A", "B"),
      KV.of("A", "C"),
      KV.of("B", "D"),
      KV.of("C", "E"),
      KV.of("D", "E"),
      KV.of("E", "F"),
      KV.of("E", "G")
  );
  Multimap<String, String> y = Multimaps


  @Test
  public void transitiveClosure() {
    Values<KV<String, String>> edges = Create.of(List.of(KV.of("A", "B"), KV.of("B", "C")));
    PCollection<KV<String, String>> transitiveEdges =
        AncestorUtils.transitiveClosure(pipeline.apply(edges), 2);
    PAssert.that(transitiveEdges)
        .containsInAnyOrder(KV.of("A", "B"), KV.of("A", "C"), KV.of("B", "C"));
    pipeline.run().waitUntilFinish();
  }
}

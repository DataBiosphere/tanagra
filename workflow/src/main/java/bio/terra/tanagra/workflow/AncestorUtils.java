package bio.terra.tanagra.workflow;

import avro.shaded.com.google.common.collect.ImmutableList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.Flatten;
import org.apache.beam.sdk.transforms.KvSwap;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.transforms.join.CoGbkResult;
import org.apache.beam.sdk.transforms.join.CoGroupByKey;
import org.apache.beam.sdk.transforms.join.KeyedPCollectionTuple;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.PCollectionList;
import org.apache.beam.sdk.values.TupleTag;
import org.apache.beam.vendor.guava.v26_0_jre.com.google.common.collect.Lists;

// DO NOT SUBMIT rename me
public final class AncestorUtils {
  private AncestorUtils() {}

  // DO NOT SUBMIT comment me.
  public static <T> PCollection<KV<T, T>> transitiveClosure(
      PCollection<KV<T, T>> edges, int longestPath) {
    // allPaths[n] is the set of vertices [V0, V1], where V0 -> V1 is reachable in N or less edges.
    // That is, there is a path with N or less edges from V0, to V1.
    Map<Integer, PCollection<KV<T, T>>> allPaths = new HashMap<>();
    // exactPaths[n] is the set of vertices where there exists a path from V0 -> V1 in exactly N
    // edges.
    Map<Integer, PCollection<KV<T, T>>> exactPaths = new HashMap<>();

    // The input edges are both the paths and exact paths for exactly N=1.
    allPaths.put(1, edges);
    exactPaths.put(1, edges);

    int n = 1;
    while (true) {
      PCollection<KV<T, T>> nExactPaths = exactPaths.get(n);
      PCollection<KV<T, T>> twoNExactPaths = concatenate(nExactPaths, nExactPaths, "twoN" + n);
      exactPaths.put(2 * n, twoNExactPaths);

      PCollection<KV<T, T>> previousAllPaths = allPaths.get(2 * n - 1);

      PCollection<KV<T, T>> newAllPaths =
          PCollectionList.of(previousAllPaths)
              .and(twoNExactPaths)
              .and(concatenate(twoNExactPaths, previousAllPaths, "allPaths" + n))
              .apply(Flatten.pCollections());
      int maxPathLengthSoFar = 4 * n - 1;
      if (maxPathLengthSoFar > longestPath) {
        return newAllPaths;
      }
      allPaths.put(maxPathLengthSoFar, newAllPaths);
      n = n * 2;
    }
  }

  /**
   * Concatenate two collections of edges by joining their "middle" shared vertex.
   *
   * <p>E.g. if paths1 has [A, B] and paths2 has [B, C], we output [A, C] by joining on B.
   */
  private static <T> PCollection<KV<T, T>> concatenate(
      PCollection<KV<T, T>> paths1, PCollection<KV<T, T>> paths2, String nameSuffix) {
    final TupleTag<T> t1 = new TupleTag<>();
    final TupleTag<T> t2 = new TupleTag<>();

    // Swap the key-values so that the middle vertex is the first key for both collections.
    PCollection<KV<T, T>> swappedPaths1 =
        paths1.apply("concatenateSwap" + nameSuffix, KvSwap.create());

    return KeyedPCollectionTuple.of(t1, swappedPaths1)
        .and(t2, paths2)
        .apply("concatenateJoin" + nameSuffix, CoGroupByKey.create())
        .apply(
            "concatenateTransform" + nameSuffix,
            ParDo.of(
                new DoFn<KV<T, CoGbkResult>, KV<T, T>>() {
                  @ProcessElement
                  public void processElement(ProcessContext context) {
                    KV<T, CoGbkResult> element = context.element();
                    // heads of paths1.
                    ImmutableList<T> heads = ImmutableList.copyOf(element.getValue().getAll(t1));
                    // tails of paths2.
                    ImmutableList<T> tails = ImmutableList.copyOf(element.getValue().getAll(t2));
                    // All combinations of heads and tails, where the 0th element is the head and
                    // the 1st
                    // element is the tail.
                    List<List<T>> newEdges = Lists.cartesianProduct(heads, tails);
                    newEdges.stream()
                        .map((List<T> edge) -> KV.of(edge.get(0), edge.get(1)))
                        .forEach(context::output);
                  }
                }));
  }
}

package bio.terra.tanagra.workflow;

import java.util.Iterator;
import org.apache.beam.sdk.transforms.Count;
import org.apache.beam.sdk.transforms.Distinct;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.MapElements;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.transforms.join.CoGbkResult;
import org.apache.beam.sdk.transforms.join.CoGroupByKey;
import org.apache.beam.sdk.transforms.join.KeyedPCollectionTuple;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.TupleTag;
import org.apache.beam.sdk.values.TypeDescriptors;

public final class CountUtils {
  private CountUtils() {}

  /**
   * Count the number of distinct auxiliary nodes that each primary node has.
   *
   * @param primaryNodes a collection of the primary nodes that we want a count for
   * @param auxiliaryNodes a collection of all auxiliary nodes that we want to count
   * @return a collection of (node, count) mappings
   */
  public static PCollection<KV<Long, Long>> countDistinct(
      PCollection<Long> primaryNodes, PCollection<KV<Long, Long>> auxiliaryNodes) {
    // remove duplicate auxiliary nodes
    PCollection<KV<Long, Long>> distinctAuxiliaryNodes =
        auxiliaryNodes.apply("remove duplicate auxiliary nodes", Distinct.create());

    // count the number of auxiliary nodes per primary node
    // note that this will not include any zeros -- only primary nodes that have >=1 auxiliary node
    // will show up here
    PCollection<KV<Long, Long>> nodeToNonZeroCountKVs =
        distinctAuxiliaryNodes.apply(
            "count the number of distinct auxiliary nodes per primary node", Count.perKey());

    // build a collection of KV<node,count> where count=0
    PCollection<KV<Long, Long>> nodeCountKVs =
        primaryNodes.apply(
            "build (node,count) KV pairs",
            MapElements.into(TypeDescriptors.kvs(TypeDescriptors.longs(), TypeDescriptors.longs()))
                .via(node -> KV.of(node, 0L)));

    // define the CoGroupByKey tags
    final TupleTag<Long> countTag = new TupleTag<>();
    final TupleTag<Long> nonZeroCountTag = new TupleTag<>();

    // do a CoGroupByKey join of the current node-count collection and the node-nonZeroCount
    // collection
    PCollection<KV<Long, CoGbkResult>> nodeNonZeroCountJoin =
        KeyedPCollectionTuple.of(countTag, nodeCountKVs)
            .and(nonZeroCountTag, nodeToNonZeroCountKVs)
            .apply("join node-count and node-nonZeroCount collections", CoGroupByKey.create());

    // run a ParDo for each row of the join result
    return nodeNonZeroCountJoin.apply(
        "run ParDo for each row of the node-count and node-nonZeroCount join result",
        ParDo.of(
            new DoFn<KV<Long, CoGbkResult>, KV<Long, Long>>() {
              @ProcessElement
              public void processElement(ProcessContext context) {
                KV<Long, CoGbkResult> element = context.element();
                Long node = element.getKey();
                Iterator<Long> countTagIter = element.getValue().getAll(countTag).iterator();
                Iterator<Long> nonZeroCountTagIter =
                    element.getValue().getAll(nonZeroCountTag).iterator();

                // if the auxiliary row contains a primary node that is not included in the
                // collection of all primary nodes, then skip processing
                if (!countTagIter.hasNext()) {
                  return;
                }

                // output a count for each primary node, including counts of zero
                Long count =
                    nonZeroCountTagIter.hasNext()
                        ? nonZeroCountTagIter.next()
                        : countTagIter.next();
                context.output(KV.of(node, count));
              }
            }));
  }
}

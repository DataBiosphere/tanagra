package bio.terra.tanagra.workflow;

import java.util.Iterator;
import org.apache.beam.sdk.transforms.Count;
import org.apache.beam.sdk.transforms.Distinct;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.MapElements;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.transforms.Sum;
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

  /**
   * Aggregate counts in a hierarchy, so that nodes have a count that includes all their
   * descendants.
   *
   * @param nodeDirectCount a collection of (node, count) mappings where the count is the direct
   *     count for that node only, not including descendants
   * @param descendantAncestor a collection of all (descendant, ancestor) pairs
   * @return a collection of (node, count) mappings where the count for a node includes the counts
   *     of all its descendants.
   */
  public static PCollection<KV<Long, Long>> aggregateCountsInHierarchy(
      PCollection<KV<Long, Long>> nodeDirectCount, PCollection<KV<Long, Long>> descendantAncestor) {

    // join on node = descendant. for each item, output a pair (ancestor, directCount)

    // define the CoGroupByKey tags
    final TupleTag<Long> directCountTag = new TupleTag<>();
    final TupleTag<Long> ancestorTag = new TupleTag<>();

    // do a CoGroupByKey join of the node-directCount collection and the descendant-ancestor
    // collection
    PCollection<KV<Long, CoGbkResult>> nodeDescendantJoin =
        KeyedPCollectionTuple.of(directCountTag, nodeDirectCount)
            .and(ancestorTag, descendantAncestor)
            .apply(
                "join node-directCount and descendant-ancestor collections", CoGroupByKey.create());

    // run a ParDo for each row of the join result
    PCollection<KV<Long, Long>> ancestorDirectCounts =
        nodeDescendantJoin.apply(
            "run ParDo for each row of the node-directCount and descendant-ancestor join result",
            ParDo.of(
                new DoFn<KV<Long, CoGbkResult>, KV<Long, Long>>() {
                  @ProcessElement
                  public void processElement(ProcessContext context) {
                    KV<Long, CoGbkResult> element = context.element();
                    Long node = element.getKey();
                    Iterator<Long> directCountTagIter =
                        element.getValue().getAll(directCountTag).iterator();
                    Iterator<Long> ancestorTagIter =
                        element.getValue().getAll(ancestorTag).iterator();

                    // if there is no direct count for this node, then skip processing
                    if (!directCountTagIter.hasNext()) {
                      return;
                    }

                    Long directCount = directCountTagIter.next();

                    while (ancestorTagIter.hasNext()) {
                      Long ancestor = ancestorTagIter.next();

                      // if this ancestor = descendant, then continue because we handle that case
                      // explicitly after the loop. this way we can handle ancestor-descendant
                      // tables that include self-relationships or not.
                      if (ancestor.equals(node)) {
                        continue;
                      }

                      // output a pair (ancestor, directCount)
                      context.output(KV.of(ancestor, directCount));
                    }

                    // output a pair for the descendant also
                    context.output(KV.of(node, directCount));
                  }
                }));

    // sum the counts for each ancestor
    return ancestorDirectCounts.apply(
        "sum all the direct counts for each ancestor", Sum.longsPerKey());
  }
}

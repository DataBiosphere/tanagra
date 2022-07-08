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
   * Count the number of distinct occurrences for each primary node.
   *
   * <p>For example, say we want to precompute the number of people that have >=1 occurrence of a
   * particular condition.
   *
   * <p>primary node=condition, occurrence=condition_occurrence=(condition,person)
   *
   * @param primaryNodes a collection of the primary nodes that we want a count for: (node)
   * @param occurrences a collection of all occurrences of primary nodes: (node, what_to_count)
   * @return a collection of (node, count) mappings
   */
  public static PCollection<KV<Long, Long>> countDistinct(
      PCollection<Long> primaryNodes, PCollection<KV<Long, Long>> occurrences) {
    // remove duplicate occurrences
    PCollection<KV<Long, Long>> distinctOccurrences =
        occurrences.apply("remove duplicate occurrences before counting", Distinct.create());

    // count the number of occurrences per primary node
    // note that this will not include any zeros -- only primary nodes that have >=1 auxiliary node
    // will show up here
    PCollection<KV<Long, Long>> nonZeroCountKVs =
        distinctOccurrences.apply(
            "count the number of distinct occurrences per primary node", Count.perKey());

    // build a collection of KV<node,count> where count=0
    PCollection<KV<Long, Long>> allZeroCountKVs =
        primaryNodes.apply(
            "build initial (node,count) KV pairs",
            MapElements.into(TypeDescriptors.kvs(TypeDescriptors.longs(), TypeDescriptors.longs()))
                .via(node -> KV.of(node, 0L)));

    // JOIN nonZeroCount (node, count>0) x allZeroCount (node, count=0)
    // ON nonZeroCount (node) = allZeroCount (node)
    // RESULT = tuples of (node, count>0, count=0)
    // for each RESULT tuple:
    //   - if there is a count>0, then output (node, count>0)
    //   - otherwise, output (node, count=0)

    // define the CoGroupByKey tags
    final TupleTag<Long> nonZeroCountTag = new TupleTag<>();
    final TupleTag<Long> zeroCountTag = new TupleTag<>();

    // do a CoGroupByKey join of the current nonZeroCount x allZeroCount collections
    PCollection<KV<Long, CoGbkResult>> nodeNonZeroCountJoin =
        KeyedPCollectionTuple.of(nonZeroCountTag, nonZeroCountKVs)
            .and(zeroCountTag, allZeroCountKVs)
            .apply("join nonZeroCount x allZeroCount collections", CoGroupByKey.create());

    // run a ParDo for each row of the join result
    return nodeNonZeroCountJoin.apply(
        "run ParDo for each tuple of the  nonZeroCount x allZeroCount join result",
        ParDo.of(
            new DoFn<KV<Long, CoGbkResult>, KV<Long, Long>>() {
              @ProcessElement
              public void processElement(ProcessContext context) {
                KV<Long, CoGbkResult> element = context.element();
                Long node = element.getKey();
                Iterator<Long> nonZeroCountTagIter =
                    element.getValue().getAll(nonZeroCountTag).iterator();
                Iterator<Long> zeroCountTagIter =
                    element.getValue().getAll(zeroCountTag).iterator();

                // if there is a primary node that exists in the occurrences collection,
                // but not in the primary nodes collection, then skip processing it
                if (!zeroCountTagIter.hasNext()) {
                  return;
                }

                // output a count for each primary node, including counts of zero
                if (nonZeroCountTagIter.hasNext()) {
                  context.output(KV.of(node, nonZeroCountTagIter.next()));
                } else {
                  context.output(
                      KV.of(node, zeroCountTagIter.next())); // equivalent to KV.of(node, 0L)
                }
              }
            }));
  }

  /**
   * For each occurrence (node, what_to_count), generate a new occurrence for each ancestor of the
   * primary node (ancestor, what_to_count).
   *
   * <p>For example, say we want to precompute the number of people that have >=1 occurrence of a
   * particular condition.
   *
   * <p>primary=condition, occurrence=condition_occurrence, condition hierarchy= medicalProblem -
   * endocrineDisorder - diabetes
   *
   * <p>This method takes a condition_occurrence (diabetes, personA) and generates 2 more
   * condition_occurrences (endocrineDisorder, personA), (medicalProblem, personA).
   *
   * <p>The reason to do this, instead of just counting the people that have diabetes, and then
   * summing the counts of the children to get the count for each parent condition in the hierarchy,
   * is that two child conditions' person counts may include the same person, and then we'd be
   * counting that person twice for the parent condition. So we need to preserve the person
   * information for each level of the hierarchy and sum them independently.
   *
   * <p>For example, say endocrineDisorder has 2 children: diabetes and hyperthyroidism. PersonB has
   * an occurrence of each. The counts for both diabetes and hyperthyroidism will include PersonB.
   * If we just sum the counts of the children to get the count for endocrineDisorder, then we will
   * be counting PersonB twice.
   *
   * @param occurrences a collection of all occurrences that we want to count
   * @param descendantAncestor a collection of (descendant, ancestor) pairs for the primary nodes
   *     that we want a count for. note that this is the expanded set of all transitive
   *     relationships in the hierarchy, not just the parent/child pairs
   * @return an expanded collection of occurrences (node, what_to_count), where each occurrence has
   *     been repeated for each ancestor of its primary node
   */
  public static PCollection<KV<Long, Long>> repeatOccurrencesForHierarchy(
      PCollection<KV<Long, Long>> occurrences, PCollection<KV<Long, Long>> descendantAncestor) {
    // remove duplicate occurrences
    PCollection<KV<Long, Long>> distinctOccurrences =
        occurrences.apply(
            "remove duplicate occurrences before repeating for hierarchy", Distinct.create());

    // JOIN occurrences (node, what_to_count) x descendantAncestor (descendant_node, ancestor_node)
    // ON occurrences (node) = descendantAncestor (descendant_node)
    // RESULT = tuples of (node, list of what_to_count, list of ancestor_nodes)
    // for each RESULT tuple:
    //   - iterate through the ancestor_nodes and output (ancestor_node, what_to_count)
    //   - output (node, what_to_count)
    // e.g. node = condition, what_to_count = person

    // define the CoGroupByKey tags
    final TupleTag<Long> whatToCountTag = new TupleTag<>();
    final TupleTag<Long> ancestorTag = new TupleTag<>();

    // do a CoGroupByKey join of the occurrences and descendantAncestor collections
    PCollection<KV<Long, CoGbkResult>> joinResultTuples =
        KeyedPCollectionTuple.of(whatToCountTag, distinctOccurrences)
            .and(ancestorTag, descendantAncestor)
            .apply(
                "join distinctOccurrences and descendantAncestor collections",
                CoGroupByKey.create());

    // run a ParDo for each join result tuple
    return joinResultTuples.apply(
        "run ParDo for each tuple of the distinctOccurrences x descendantAncestor join result",
        ParDo.of(
            new DoFn<KV<Long, CoGbkResult>, KV<Long, Long>>() {
              @ProcessElement
              public void processElement(ProcessContext context) {
                KV<Long, CoGbkResult> element = context.element();
                Long descendantNode = element.getKey();
                Iterator<Long> whatToCountTagIter =
                    element.getValue().getAll(whatToCountTag).iterator();

                // iterate through each of the occurrences for this node
                while (whatToCountTagIter.hasNext()) {
                  Long whatToCount = whatToCountTagIter.next();

                  // output a pair for the original occurrence (node, what_to_count)
                  context.output(KV.of(descendantNode, whatToCount));

                  // get a new iterator through the ancestor nodes each loop, so we start from the
                  // beginning each time
                  Iterator<Long> ancestorTagIter =
                      element.getValue().getAll(ancestorTag).iterator();

                  // output a pair for each ancestor (ancestor_node, what_to_count)
                  while (ancestorTagIter.hasNext()) {
                    Long ancestorNode = ancestorTagIter.next();

                    context.output(KV.of(ancestorNode, whatToCount));
                  }
                }
              }
            }));
  }
}

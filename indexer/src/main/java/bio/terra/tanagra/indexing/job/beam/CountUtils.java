package bio.terra.tanagra.indexing.job.beam;

import org.apache.beam.sdk.extensions.joinlibrary.Join;
import org.apache.beam.sdk.transforms.*;
import org.apache.beam.sdk.values.*;

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
    // Remove duplicate occurrences.
    // Do this because there could be duplicate occurrences (i.e. 2 occurrences of diabetes for
    // personA) in the original data. Or, for concepts that include a hierarchy, there would be
    // duplicates if e.g. a parent condition had two children, each with an occurrence for the same
    // person. This distinct step is so we only count occurrences of a condition for each person
    // once.
    PCollection<KV<Long, Long>> distinctOccurrences =
        occurrences.apply("remove duplicate occurrences before counting", Distinct.create());

    // Count the number of occurrences per primary node.
    // Note that this will not include any zeros -- only primary nodes that have >=1 auxiliary node
    // will show up here.
    PCollection<KV<Long, Long>> countKVs =
        distinctOccurrences.apply(
            "count the number of distinct occurrences per primary node", Count.perKey());

    // Build a collection of KV<node,[placeholder 0L]>. This is just a collection of the primary
    // nodes, but here we expand it to a map where each primary node is mapped to 0L, just so we can
    // do an outer join with the countKVs above. The 0L is just a placeholder value in the map, the
    // actual count will be set in the join result.
    PCollection<KV<Long, Long>> nodeToPlaceholderKVs =
        primaryNodes.apply(
            "build initial (node,[placeholder 0L]) KV pairs",
            MapElements.into(TypeDescriptors.kvs(TypeDescriptors.longs(), TypeDescriptors.longs()))
                .via(node -> KV.of(node, 0L)));

    // JOIN: primaryNodeInitialKVs (node, placeholder zero) LEFT OUTER JOIN countKVs (node, non-zero
    // count)
    //       ON node=node, use 0L where there is no matching node in the right-hand side countKVs
    // RESULT: nodeToPlaceholderAndCounts (node=node, (placeholder zero, non-zero count or zero if
    // no non-zero count exists))
    PCollection<KV<Long, KV<Long, Long>>> nodeToPlaceholderAndCounts =
        Join.leftOuterJoin(
            "left outer join nodes with non-zero counts", nodeToPlaceholderKVs, countKVs, 0L);

    // Get rid of the placeholder zero. That was only needed because the leftOuterJoin function
    // requires the same type on the right and left hand sides.
    // RESULT: (node, non-zero count or zero if no non-zero count was found)
    return nodeToPlaceholderAndCounts.apply(
        MapElements.via(
            new SimpleFunction<>() {
              @Override
              public KV<Long, Long> apply(KV<Long, KV<Long, Long>> placeholderAndCount) {
                return KV.of(
                    placeholderAndCount.getKey(), placeholderAndCount.getValue().getValue());
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
    // Remove duplicate occurrences.
    PCollection<KV<Long, Long>> distinctOccurrences =
        occurrences.apply(
            "remove duplicate occurrences before repeating for hierarchy", Distinct.create());

    // JOIN: descendantAncestor (descendant, ancestor) INNER JOIN distinctOccurrences (node,
    // what_to_count)
    //       ON descendant=node
    // RESULT: nodeToAncestorAndWhatToCount (descendant=node, (ancestor, what_to_count))
    PCollection<KV<Long, KV<Long, Long>>> nodeToAncestorAndWhatToCount =
        Join.innerJoin(
            "inner join occurrences with ancestors", descendantAncestor, distinctOccurrences);

    // Get rid of the descendant node. That was only needed as the innerJoin field.
    // RESULT: (ancestor, what_to_count)
    PCollection<KV<Long, Long>> ancestorOccurrences =
        nodeToAncestorAndWhatToCount.apply(Values.create());

    // The descendant-ancestor pairs don't include a self-reference row (i.e. descendant=ancestor).
    // So to get the full set of occurrences, concatenate the original occurrences with the ancestor
    // duplicates.
    return PCollectionList.of(distinctOccurrences)
        .and(ancestorOccurrences)
        .apply(Flatten.pCollections());
  }
}

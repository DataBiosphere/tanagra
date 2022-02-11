package bio.terra.tanagra.workflow;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
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

public final class TextSearchUtils {
  private TextSearchUtils() {}

  /**
   * Concatenate all search strings for a node into a single string. e.g. (123, "apple pie"), (123,
   * "fruit dessert"), (123, "apple crumble") => (123, "apple pie , fruit dessert , apple crumble")
   *
   * @param allNodes a collection of all (node) entity instances
   * @param searchStrings a collection of all (node, text) mappings, where there may be zero or
   *     multiple elements per node
   * @return a collection of (node, text) mappings, where there is exactly one element per node
   */
  public static PCollection<KV<Long, String>> concatenateSearchStringsByKey(
      PCollection<Long> allNodes, PCollection<KV<Long, String>> searchStrings) {
    // build a collection of KV<node,singleSearchString> where singleSearchString=""
    PCollection<KV<Long, String>> nodeSingleSearchStringKVs =
        allNodes.apply(
            "build (node,singleSearchString) KV pairs",
            MapElements.into(
                    TypeDescriptors.kvs(TypeDescriptors.longs(), TypeDescriptors.strings()))
                .via(node -> KV.of(node, "")));

    // remove any duplicate search strings
    PCollection<KV<Long, String>> nodeSearchStrings =
        searchStrings.apply("remove duplicate search strings", Distinct.create());

    // define the CoGroupByKey tags
    final TupleTag<String> singleSearchStringTag = new TupleTag<>();
    final TupleTag<String> searchStringsTag = new TupleTag<>();

    // do a CoGroupByKey join of the current node-singleSearchString collection and the node-[search
    // strings] collection
    PCollection<KV<Long, CoGbkResult>> nodeSearchStringsJoin =
        KeyedPCollectionTuple.of(singleSearchStringTag, nodeSingleSearchStringKVs)
            .and(searchStringsTag, nodeSearchStrings)
            .apply(
                "join node-singleSearchString and node-[search strings] collections",
                CoGroupByKey.create());

    return nodeSearchStringsJoin.apply(
        ParDo.of(
            new DoFn<KV<Long, CoGbkResult>, KV<Long, String>>() {
              @ProcessElement
              public void processElement(ProcessContext context) {
                KV<Long, CoGbkResult> element = context.element();
                Long node = element.getKey();
                Iterator<String> singleSearchStringTagIter =
                    element.getValue().getAll(singleSearchStringTag).iterator();
                Iterator<String> searchStringsTagIter =
                    element.getValue().getAll(searchStringsTag).iterator();

                // if this node is not included in the list of all nodes, then ignore any search
                // strings
                if (!singleSearchStringTagIter.hasNext()) {
                  return;
                }

                // concatenate all search strings into a single string, delimited by a comma
                List<String> searchStrings = new ArrayList<>();
                while (searchStringsTagIter.hasNext()) {
                  searchStrings.add(searchStringsTagIter.next());
                }
                // sort the strings lexicographically, for easier assertion during testing
                Collections.sort(searchStrings);
                String fullTextString = searchStrings.stream().collect(Collectors.joining(" , "));

                context.output(KV.of(node, fullTextString));
              }
            }));
  }
}

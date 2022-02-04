package bio.terra.tanagra.workflow;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
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

public final class PathUtils {
  private PathUtils() {}

  // the path of nodes is currently encoded as a string. use this as the delimiter between nodes.
  private static final String PATH_DELIMITER = ".";
  private static final String PATH_DELIMITER_REGEX = "\\.";

  /**
   * Compute one path from each node in a hierarchy to a root node. If there are multiple paths from
   * a given node to a root node, then select one at random.
   *
   * @param allNodes a collection of all nodes in the hierarchy
   * @param parentChildRelationships a collection of all parent-child relationships in the hierarchy
   * @param maxPathLengthHint the maximum path length to handle
   * @return a collection of (node, path) mappings
   */
  public static PCollection<KV<Long, String>> computePaths(
      PCollection<Long> allNodes,
      PCollection<KV<Long, Long>> parentChildRelationships,
      int maxPathLengthHint) {
    // build a collection of KV<node,path> where path="initial node"
    PCollection<KV<Long, String>> nextNodePathKVs =
        allNodes.apply(
            "build (node,path) KV pairs",
            MapElements.into(
                    TypeDescriptors.kvs(TypeDescriptors.longs(), TypeDescriptors.strings()))
                .via(PathUtils::nodeToNodePath));

    // define the CoGroupByKey tags
    final TupleTag<String> pathTag = new TupleTag<>();
    final TupleTag<Long> parentTag = new TupleTag<>();

    /* Example for CoGroupByKey+ParDo iteration below:
    desired result (node,path)
    (a1,"a1.a2.a3")
    (a2,"a2.a3")
    (a3,"a3")
    (b1,"b1.b2")
    (b2,"b2")
    (c1,"c1")

    childParentKVs (child,parent)
    (a1,"a2")
    (a2,"a3")
    (b1,"b2")

    [iteration 0] nodePathKVs (next node,path)
    (a1,"a1")
    (a2,"a2")
    (a3,"a3")
    (b1,"b1")
    (b2,"b2")
    (c1,"c1")
    [iteration 0] nodeParentPathJoin (next node,paths,next node parents)
    (a1,["a1"],["a2"])
    (a2,["a2"],["a3"])
    (a3,["a3"],[])
    (b1,["b1"],["b2"])
    (b2,["b2"],[])
    (c1,["c1"],[])

    [iteration 1] nodePathKVs (next node,path)
    (a2,"a2.a1")
    (a3,"a3.a2")
    (a3,"a3")
    (b2,"b2.b1")
    (b2,"b2")
    (c1,"c1")
    [iteration 1] nodeParentPathJoin (next node,paths,next node parents)
    (a1,[],["a2"])
    (a2,["a2.a1"],["a3"])
    (a3,["a3.a2","a3"],[])
    (b1,[],["b2]")
    (b2,["b2.b1","b2"],[])
    (c1,["c1"],[]) */
    // iterate through each possible level of the hierarchy, adding up to one node to each path per
    // iteration
    for (int ctr = 0; ctr < maxPathLengthHint; ctr++) {
      // do a CoGroupByKey join of the current node-path collection and the child-parent collection
      PCollection<KV<Long, CoGbkResult>> nodeParentPathJoin =
          KeyedPCollectionTuple.of(pathTag, nextNodePathKVs)
              .and(parentTag, parentChildRelationships)
              .apply("join node-path and child-parent collections: " + ctr, CoGroupByKey.create());

      // run a ParDo for each row of the join result
      nextNodePathKVs =
          nodeParentPathJoin.apply(
              "run ParDo for each row of the join result: " + ctr,
              ParDo.of(
                  new DoFn<KV<Long, CoGbkResult>, KV<Long, String>>() {
                    @ProcessElement
                    public void processElement(ProcessContext context) {
                      KV<Long, CoGbkResult> element = context.element();
                      Long node = element.getKey();
                      Iterator<String> pathTagIter = element.getValue().getAll(pathTag).iterator();
                      Iterator<Long> parentTagIter =
                          element.getValue().getAll(parentTag).iterator();

                      // there may be multiple possible next steps (i.e. the current node may have
                      // multiple parents). just pick the first one
                      Long nextNodeInPath = parentTagIter.hasNext() ? parentTagIter.next() : null;

                      // iterate through all the paths that need this relationship to complete their
                      // next step
                      while (pathTagIter.hasNext()) {
                        String currentPath = pathTagIter.next();

                        if (nextNodeInPath == null) {
                          // if there are no relationships to complete the next step, then we've
                          // reached a root node. just keep the path as is
                          context.output(KV.of(node, currentPath));
                        } else {
                          // if there is a next node, then append it to the path and make it the new
                          // key (i.e. the new next node)
                          context.output(
                              KV.of(nextNodeInPath, currentPath + PATH_DELIMITER + nextNodeInPath));
                        }
                      }
                    }
                  }));
    }

    // swap the key of all pairs from the next node in the path (which should all be root nodes at
    // this point), to the first node in the path. also trim the first node from the path.
    return nextNodePathKVs.apply(
        ParDo.of(
            new DoFn<KV<Long, String>, KV<Long, String>>() {
              @ProcessElement
              public void processElement(ProcessContext context) {
                KV<Long, String> kvPair = context.element();

                String path = kvPair.getValue();

                // strip out the first node in the path, and make it the key
                // e.g. (11,"11.21.31") => (11,"21.31")
                //      (12,"12") => (12,"")
                List<String> nodesInPath =
                    new java.util.ArrayList<>(List.of(path.split(PATH_DELIMITER_REGEX)));
                String firstNodeInPath = nodesInPath.remove(0);
                String pathWithoutFirstNode =
                    nodesInPath.stream().collect(Collectors.joining(PATH_DELIMITER));

                Long firstNode = Long.valueOf(firstNodeInPath);

                context.output(KV.of(firstNode, pathWithoutFirstNode));
              }
            }));
  }

  /**
   * Build a {@link KV} pair for a node: (node, path). The starting path is just the node itself.
   */
  private static KV<Long, String> nodeToNodePath(Long node) {
    return KV.of(node, node.toString());
  }
}

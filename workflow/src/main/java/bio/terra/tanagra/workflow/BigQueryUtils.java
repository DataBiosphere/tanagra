package bio.terra.tanagra.workflow;

import com.google.api.services.bigquery.model.TableRow;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.io.gcp.bigquery.BigQueryIO;
import org.apache.beam.sdk.transforms.MapElements;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.TypeDescriptors;

public final class BigQueryUtils {
  private BigQueryUtils() {}

  /** Read the set of nodes from BQ and build a {@link PCollection} of just the node identifiers. */
  public static PCollection<Long> readNodesFromBQ(
      Pipeline pipeline, String sqlQuery, String description) {
    PCollection<TableRow> allNodesBqRows =
        pipeline.apply(
            "read (node) rows: " + description,
            BigQueryIO.readTableRows()
                .fromQuery(sqlQuery)
                .withMethod(BigQueryIO.TypedRead.Method.EXPORT)
                .usingStandardSql());
    return allNodesBqRows.apply(
        "build (node) pcollection: " + description,
        MapElements.into(TypeDescriptors.longs())
            .via(tableRow -> Long.parseLong((String) tableRow.get("node"))));
  }

  /**
   * Read all the child-parent relationships from BQ and build a {@link PCollection} of {@link KV}
   * pairs (child, parent).
   */
  public static PCollection<KV<Long, Long>> readChildParentRelationshipsFromBQ(
      Pipeline pipeline, String sqlQuery) {
    return readTwoFieldRowsFromBQ(pipeline, sqlQuery, "child", "parent");
  }

  /**
   * Read all the auxiliary rows from BQ and build a {@link PCollection} of {@link KV} pairs (node,
   * secondary).
   */
  public static PCollection<KV<Long, Long>> readAuxiliaryNodesFromBQ(
      Pipeline pipeline, String sqlQuery) {
    return readTwoFieldRowsFromBQ(pipeline, sqlQuery, "node", "secondary");
  }

  /**
   * Read all the ancestor-descendant relationships from BQ and build a {@link PCollection} of
   * {@link KV} pairs (descendant, ancestor).
   */
  public static PCollection<KV<Long, Long>> readAncestorDescendantRelationshipsFromBQ(
      Pipeline pipeline, String sqlQuery) {
    return readTwoFieldRowsFromBQ(pipeline, sqlQuery, "descendant", "ancestor");
  }

  /**
   * Read all the two-field rows from BQ and build a {@link PCollection} of {@link KV} pairs
   * (field1, field2).
   */
  public static PCollection<KV<Long, Long>> readTwoFieldRowsFromBQ(
      Pipeline pipeline, String sqlQuery, String field1Name, String field2Name) {
    PCollection<TableRow> bqRows =
        pipeline.apply(
            "read all (" + field1Name + ", " + field2Name + ") rows",
            BigQueryIO.readTableRows()
                .fromQuery(sqlQuery)
                .withMethod(BigQueryIO.TypedRead.Method.EXPORT)
                .usingStandardSql());
    return bqRows.apply(
        "build (" + field1Name + ", " + field2Name + ") pcollection",
        MapElements.into(TypeDescriptors.kvs(TypeDescriptors.longs(), TypeDescriptors.longs()))
            .via(
                tableRow -> {
                  Long field1 = Long.parseLong((String) tableRow.get(field1Name));
                  Long field2 = Long.parseLong((String) tableRow.get(field2Name));
                  return KV.of(field1, field2);
                }));
  }
}

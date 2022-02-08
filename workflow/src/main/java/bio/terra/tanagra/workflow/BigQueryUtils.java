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
    PCollection<TableRow> childParentBqRows =
        pipeline.apply(
            "read all (child, parent) rows",
            BigQueryIO.readTableRows()
                .fromQuery(sqlQuery)
                .withMethod(BigQueryIO.TypedRead.Method.EXPORT)
                .usingStandardSql());
    return childParentBqRows.apply(
        "build (child, parent) pcollection",
        MapElements.into(TypeDescriptors.kvs(TypeDescriptors.longs(), TypeDescriptors.longs()))
            .via(
                tableRow -> {
                  Long parent = Long.parseLong((String) tableRow.get("parent"));
                  Long child = Long.parseLong((String) tableRow.get("child"));
                  return KV.of(child, parent);
                }));
  }
}

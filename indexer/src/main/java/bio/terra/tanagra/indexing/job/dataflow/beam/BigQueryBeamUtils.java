package bio.terra.tanagra.indexing.job.dataflow.beam;

import com.google.api.services.bigquery.model.TableRow;
import java.time.LocalDate;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.io.gcp.bigquery.BigQueryIO;
import org.apache.beam.sdk.transforms.MapElements;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.TypeDescriptors;

public final class BigQueryBeamUtils {

  private BigQueryBeamUtils() {}

  /** Read the set of nodes from BQ and build a {@link PCollection} of just the node identifiers. */
  public static PCollection<Long> readNodesFromBQ(
      Pipeline pipeline, String sqlQuery, String idColumnName, String description) {
    PCollection<TableRow> allNodesBqRows =
        pipeline.apply(
            "read (id) rows: " + description,
            BigQueryIO.readTableRows()
                .fromQuery(sqlQuery)
                .withMethod(BigQueryIO.TypedRead.Method.EXPORT)
                .usingStandardSql());
    return allNodesBqRows.apply(
        "build (id) pcollection: " + description,
        MapElements.into(TypeDescriptors.longs())
            .via(tableRow -> Long.parseLong((String) tableRow.get(idColumnName))));
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

  /** Apache Beam returns DATE/TIMESTAMP columns as string. Convert to LocalDate. */
  public static LocalDate toLocalDate(String date) {
    // For DATE column, date looks like "2017-09-13".
    // For TIMESTAMP column, date looks like "1947-02-06 00:00:00 UTC".
    // Convert latter to former, since LocalDate doesn't know how to parse latter.
    return LocalDate.parse(date.substring(0, 10));
  }
}

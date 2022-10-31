package bio.terra.tanagra.indexing.job;

import bio.terra.tanagra.indexing.BigQueryIndexingJob;
import bio.terra.tanagra.query.TablePointer;
import bio.terra.tanagra.underlay.Entity;
import bio.terra.tanagra.underlay.Underlay;
import com.google.api.services.bigquery.model.TableFieldSchema;
import com.google.api.services.bigquery.model.TableRow;
import com.google.api.services.bigquery.model.TableSchema;
import java.io.Serializable;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.io.gcp.bigquery.BigQueryIO;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.Filter;
import org.apache.beam.sdk.transforms.MapElements;
import org.apache.beam.sdk.transforms.Max;
import org.apache.beam.sdk.transforms.Min;
import org.apache.beam.sdk.transforms.join.CoGbkResult;
import org.apache.beam.sdk.transforms.join.CoGroupByKey;
import org.apache.beam.sdk.transforms.join.KeyedPCollectionTuple;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.TupleTag;
import org.apache.beam.sdk.values.TypeDescriptor;
import org.apache.beam.sdk.values.TypeDescriptors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ComputeRollups extends BigQueryIndexingJob {
  private static final Logger LOGGER = LoggerFactory.getLogger(ComputeRollups.class);

  // The default table schema for the output table.
  private static final TableSchema ROLLUPS_TABLE_SCHEMA =
      new TableSchema()
          .setFields(
              List.of(
                  new TableFieldSchema().setName("id").setType("INTEGER").setMode("NULLABLE"),
                  new TableFieldSchema().setName("min").setType("FLOAT").setMode("NULLABLE"),
                  new TableFieldSchema().setName("max").setType("FLOAT").setMode("NULLABLE")));

  public ComputeRollups(Entity entity) {
    super(entity);
  }

  @Override
  public String getName() {
    return "COMPUTE ROLLUPS (" + getEntity().getName() + ")";
  }

  @Override
  public void run(boolean isDryRun) {
    String sql =
        "SELECT mocc.measurement_concept_id, mocc.measurement_id, mocc.person_id, mocc.value_as_concept_id, c.concept_name, mocc.value_as_number "
            + "FROM `broad-tanagra-dev.aou_synthetic_SR2019q4r4`.measurement AS mocc "
            + "LEFT JOIN `broad-tanagra-dev.aou_synthetic_SR2019q4r4`.concept AS c "
            + "ON c.concept_id = mocc.value_as_concept_id "
            + "WHERE mocc.measurement_concept_id IN (3022318, 4301868)";
    LOGGER.info("select all attributes SQL: {}", sql);

    Pipeline pipeline =
        Pipeline.create(buildDataflowPipelineOptions(getBQDataPointer(getAuxiliaryTable())));

    PCollection<TableRow> allInstances =
        pipeline.apply(
            BigQueryIO.readTableRows()
                .fromQuery(sql)
                .withMethod(BigQueryIO.TypedRead.Method.EXPORT)
                .usingStandardSql());

    String criteriaIdName = "measurement_concept_id";
    String occurrenceIdName = "measurement_id";
    String primaryIdName = "person_id";

    // for integer type attributes:
    String integerAttributeName = "value_as_number";
    //   - remove rows with a null value
    PCollection<TableRow> integerNotNullInstances =
        allInstances.apply(
            Filter.by(
                tableRow ->
                    tableRow.get(criteriaIdName) != null
                        && tableRow.get(integerAttributeName) != null));
    //   - build key-value pairs: [key] criteriaId, [value] attribute value
    PCollection<KV<Long, Double>> criteriaValuePairs =
        integerNotNullInstances.apply(
            MapElements.into(
                    TypeDescriptors.kvs(TypeDescriptors.longs(), TypeDescriptors.doubles()))
                .via(
                    tableRow ->
                        KV.of(
                            Long.parseLong((String) tableRow.get(criteriaIdName)),
                            (Double) tableRow.get(integerAttributeName))));
    //   - combine-min by key: [key] criteriaId, [value] min attr value
    PCollection<KV<Long, Double>> criteriaMinPairs = criteriaValuePairs.apply(Min.doublesPerKey());
    //   - combine-max by key: [key] criteriaId, [value] max attr value
    PCollection<KV<Long, Double>> criteriaMaxPairs = criteriaValuePairs.apply(Max.doublesPerKey());
    //   - group by key, pardo for each key: [key] criteriaId, [value] min and max attr value
    final TupleTag<Double> minTag = new TupleTag<>();
    final TupleTag<Double> maxTag = new TupleTag<>();
    PCollection<KV<Long, CoGbkResult>> criteriaAndMinMax =
        KeyedPCollectionTuple.of(minTag, criteriaMinPairs)
            .and(maxTag, criteriaMaxPairs)
            .apply(CoGroupByKey.create());
    //   - build BQ rows to insert: (id=criteriaId, min=min, max=max)
    PCollection<TableRow> bqMinMaxRows =
        criteriaAndMinMax.apply(
            MapElements.into(TypeDescriptor.of(TableRow.class))
                .via(
                    cogb -> {
                      Long criteriaId = cogb.getKey();
                      Iterator<Double> minTagIter = cogb.getValue().getAll(minTag).iterator();
                      Iterator<Double> maxTagIter = cogb.getValue().getAll(maxTag).iterator();

                      Double min = minTagIter.hasNext() ? minTagIter.next() : null;
                      Double max = maxTagIter.hasNext() ? maxTagIter.next() : null;
                      return new TableRow().set("id", criteriaId).set("min", min).set("max", max);
                    }));
    //   - write rows to BQ: (criteriaId, attributeName, minValue, maxValue)
    bqMinMaxRows.apply(
        BigQueryIO.writeTableRows()
            .to(getAuxiliaryTable().getPathForIndexing())
            .withSchema(ROLLUPS_TABLE_SCHEMA)
            .withCreateDisposition(BigQueryIO.Write.CreateDisposition.CREATE_IF_NEEDED)
            .withWriteDisposition(BigQueryIO.Write.WriteDisposition.WRITE_EMPTY)
            .withMethod(BigQueryIO.Write.Method.FILE_LOADS));

    // for string type attributes:
    String enumValueAttributeName = "value_as_concept_id";
    String enumDisplayAttributeName = "concept_name";
    //   - remove rows with a null value
    //    PCollection<TableRow> enumNotNullInstances =
    //            allInstances.apply(
    //                    Filter.by(
    //                            tableRow ->
    //                                    tableRow.get(criteriaIdName) != null));
    //   - build key-value pairs: [key] criteriaId+attribute value/display, [value] person id

    //   - distinct (remove duplicate persons):  [key] criteriaId+attribute value-attribute display,
    // [value] person id
    //   - count by key: [key] criteriaId+attribute value-attribute display, [value] num person ids
    //   - write rows to BQ: (criteriaId, attributeName, value, display, numPrimaryIds)

    if (!isDryRun) {
      pipeline.run().waitUntilFinish();
    }
  }

  @Override
  public void clean(boolean isDryRun) {
    if (checkTableExists(getAuxiliaryTable())) {
      deleteTable(getAuxiliaryTable(), isDryRun);
    }
  }

  @Override
  public JobStatus checkStatus() {
    // Check if the table already exists.
    return checkTableExists(getAuxiliaryTable()) ? JobStatus.COMPLETE : JobStatus.NOT_STARTED;
  }

  public TablePointer getAuxiliaryTable() {
    return TablePointer.fromTableName(
        "rollups_entity",
        getEntity().getMapping(Underlay.MappingType.INDEX).getTablePointer().getDataPointer());
  }
}

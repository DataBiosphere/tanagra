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
import org.apache.beam.sdk.transforms.Count;
import org.apache.beam.sdk.transforms.Distinct;
import org.apache.beam.sdk.transforms.Filter;
import org.apache.beam.sdk.transforms.MapElements;
import org.apache.beam.sdk.transforms.Max;
import org.apache.beam.sdk.transforms.Min;
import org.apache.beam.sdk.transforms.SerializableFunction;
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
                  new TableFieldSchema().setName("max").setType("FLOAT").setMode("NULLABLE"),
                  new TableFieldSchema()
                      .setName("enumValue")
                      .setType("INTEGER")
                      .setMode("NULLABLE"),
                  new TableFieldSchema()
                      .setName("enumDisplay")
                      .setType("STRING")
                      .setMode("NULLABLE"),
                  new TableFieldSchema()
                      .setName("enumCount")
                      .setType("INTEGER")
                      .setMode("NULLABLE")));

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

    // Read in all instances.
    PCollection<TableRow> allInstances =
        pipeline.apply(
            BigQueryIO.readTableRows()
                .fromQuery(sql)
                .withMethod(BigQueryIO.TypedRead.Method.EXPORT)
                .usingStandardSql());

    String criteriaIdName = "measurement_concept_id";
    String primaryIdName = "person_id";

    // for numeric range display hints:
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
    //   - group by [key] criteriaId: [values] min, max
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

    // for enum display hints:
    String enumValueAttributeName = "value_as_concept_id";
    String enumDisplayAttributeName = "concept_name";
    //   - remove rows with a null value
    PCollection<TableRow> enumValueNotNullInstances =
        allInstances.apply(
            Filter.by(
                tableRow ->
                    tableRow.get(criteriaIdName) != null
                        && tableRow.get(enumValueAttributeName) != null));
    //   - build key-value pairs: [key] criteriaId+attribute value/display, [value] primaryId
    PCollection<KV<EnumValueInstance<Long>, Long>> criteriaEnumPrimaryPairs =
        enumValueNotNullInstances.apply(
            MapElements.into(
                    TypeDescriptors.kvs(
                        new TypeDescriptor<EnumValueInstance<Long>>() {}, TypeDescriptors.longs()))
                .via(
                    tableRow -> {
                      Long criteriaId = Long.parseLong((String) tableRow.get(criteriaIdName));
                      Long enumValue =
                          Long.parseLong((String) tableRow.get(enumValueAttributeName));
                      String enumDisplay = (String) tableRow.get(enumDisplayAttributeName);
                      Long primaryId = Long.parseLong((String) tableRow.get(primaryIdName));
                      return KV.of(
                          new EnumValueInstance<Long>(criteriaId, enumValue, enumDisplay),
                          primaryId);
                    }));
    //   - build key-value pairs for criteriaId+enum value/display: [key] serialized format (by
    // built-in functions), [value] deserialized object:
    //     [key] "criteriaId-value-display", [value] criteria+enum value/display
    PCollection<KV<String, EnumValueInstance<Long>>> serializedAndDeserialized =
        criteriaEnumPrimaryPairs.apply(
            MapElements.into(
                    TypeDescriptors.kvs(
                        TypeDescriptors.strings(),
                        new TypeDescriptor<EnumValueInstance<Long>>() {}))
                .via(
                    enumValueInstancePrimaryPair ->
                        KV.of(
                            enumValueInstancePrimaryPair.getKey().toString(),
                            enumValueInstancePrimaryPair.getKey())));
    //   - build key-value pairs: [key] serialized criteriaId+enum value/display, [value] primaryId
    PCollection<KV<String, Long>> serializedPrimaryPairs =
        criteriaEnumPrimaryPairs.apply(
            MapElements.into(
                    TypeDescriptors.kvs(TypeDescriptors.strings(), TypeDescriptors.longs()))
                .via(
                    enumValueInstancePrimaryPair ->
                        KV.of(
                            enumValueInstancePrimaryPair.getKey().toString(),
                            enumValueInstancePrimaryPair.getValue())));
    //   - distinct (remove duplicate persons):  [key] serialized criteriaId+enum value/display,
    // [value] primaryId
    PCollection<KV<String, Long>> distinctSerializedPrimaryPairs =
        serializedPrimaryPairs.apply(Distinct.create());
    //   - count by key: [key] serialized criteriaId+enum value/display, [value] num primaryId
    PCollection<KV<String, Long>> serializedCountPairs =
        distinctSerializedPrimaryPairs.apply(Count.perKey());
    //   - group by [key] serialized criteriaId+enum value/display: [values] deserialized object,
    // num primaryId
    final TupleTag<EnumValueInstance<Long>> deserializedTag = new TupleTag<>();
    final TupleTag<Long> numPrimaryIdTag = new TupleTag<>();
    PCollection<KV<String, CoGbkResult>> serializedAndDeserializedNumPrimaryId =
        KeyedPCollectionTuple.of(deserializedTag, serializedAndDeserialized)
            .and(numPrimaryIdTag, serializedCountPairs)
            .apply(CoGroupByKey.create());
    //   - build BQ rows to insert: (id=criteriaId, enumValue=enumValue, enumDisplay=enumDisplay,
    // enumCount=count)
    PCollection<TableRow> bqEnumCountRows =
        serializedAndDeserializedNumPrimaryId.apply(
            MapElements.into(TypeDescriptor.of(TableRow.class))
                .via(
                    cogb -> {
                      EnumValueInstance<Long> deserialized =
                          cogb.getValue().getAll(deserializedTag).iterator().next();
                      Long count = cogb.getValue().getOnly(numPrimaryIdTag);
                      return new TableRow()
                          .set("id", deserialized.getCriteriaId())
                          .set("enumValue", deserialized.getEnumValue())
                          .set("enumDisplay", deserialized.getEnumDisplay())
                          .set("enumCount", count);
                    }));
    //   - write rows to BQ: (criteriaId, attributeName, value, display, numPrimaryIds)
    bqEnumCountRows.apply(
        BigQueryIO.writeTableRows()
            .to(getAuxiliaryTable().getPathForIndexing())
            .withSchema(ROLLUPS_TABLE_SCHEMA)
            .withCreateDisposition(BigQueryIO.Write.CreateDisposition.CREATE_IF_NEEDED)
            .withWriteDisposition(BigQueryIO.Write.WriteDisposition.WRITE_APPEND)
            .withMethod(BigQueryIO.Write.Method.FILE_LOADS));

    if (!isDryRun) {
      pipeline.run().waitUntilFinish();
    }
  }

  static class EnumValueInstance<T> implements Serializable {
    Long criteriaId;
    T enumValue;
    String enumDisplay;

    public EnumValueInstance(Long criteriaId, T enumValue, String enumDisplay) {
      this.criteriaId = criteriaId;
      this.enumValue = enumValue;
      this.enumDisplay = enumDisplay;
    }

    public Long getCriteriaId() {
      return criteriaId;
    }

    public T getEnumValue() {
      return enumValue;
    }

    public String getEnumDisplay() {
      return enumDisplay;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof EnumValueInstance)) return false;
      EnumValueInstance<?> that = (EnumValueInstance<?>) o;
      return criteriaId.equals(that.criteriaId)
          && Objects.equals(enumValue, that.enumValue)
          && Objects.equals(enumDisplay, that.enumDisplay);
    }

    @Override
    public int hashCode() {
      return Objects.hash(criteriaId, enumValue, enumDisplay);
    }

    @Override
    public String toString() {
      return criteriaId
          + " - "
          + (enumValue == null ? "null" : enumValue.toString())
          + " - "
          + enumDisplay;
    }
  }

  static class SerializeEnumValueInstance<T>
      implements SerializableFunction<KV<EnumValueInstance<Long>, Long>, String> {
    @Override
    public String apply(KV<EnumValueInstance<Long>, Long> input) {
      return input.getKey().toString() + ", " + input.getValue();
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

package bio.terra.tanagra.indexing.job;

import bio.terra.tanagra.indexing.BigQueryIndexingJob;
import bio.terra.tanagra.indexing.job.beam.DisplayHintUtils;
import bio.terra.tanagra.query.TablePointer;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.entitygroup.CriteriaOccurrence;
import com.google.api.services.bigquery.model.TableFieldSchema;
import com.google.api.services.bigquery.model.TableRow;
import com.google.api.services.bigquery.model.TableSchema;
import java.util.List;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.io.gcp.bigquery.BigQueryIO;
import org.apache.beam.sdk.transforms.Filter;
import org.apache.beam.sdk.transforms.MapElements;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.TypeDescriptor;
import org.apache.beam.sdk.values.TypeDescriptors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ComputeDisplayHints extends BigQueryIndexingJob {
  private static final Logger LOGGER = LoggerFactory.getLogger(ComputeDisplayHints.class);

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

  private final CriteriaOccurrence criteriaOccurrence;

  public ComputeDisplayHints(CriteriaOccurrence criteriaOccurrence) {
    super(criteriaOccurrence.getOccurrenceEntity());
    this.criteriaOccurrence = criteriaOccurrence;
  }

  @Override
  public String getName() {
    return "COMPUTE ROLLUPS (" + criteriaOccurrence.getName() + ")";
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
    String numericAttributeName = "value_as_number";
    numericRangeHint(allInstances, numericAttributeName, criteriaIdName);

    // for enum display hints:
    String enumValueAttributeName = "value_as_concept_id";
    String enumDisplayAttributeName = "concept_name";
    enumValHint(
        allInstances,
        enumValueAttributeName,
        enumDisplayAttributeName,
        criteriaIdName,
        primaryIdName);

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

  /** Compute the numeric range for each criteriaId and write it to BQ. */
  private void numericRangeHint(
      PCollection<TableRow> allInstances, String numericColName, String criteriaIdColName) {
    // Remove rows with a null value.
    // Build key-value pairs: [key] criteriaId, [value] attribute value.
    PCollection<KV<Long, Double>> criteriaValuePairs =
        allInstances
            .apply(
                Filter.by(
                    tableRow ->
                        tableRow.get(criteriaIdColName) != null
                            && tableRow.get(numericColName) != null))
            .apply(
                MapElements.into(
                        TypeDescriptors.kvs(TypeDescriptors.longs(), TypeDescriptors.doubles()))
                    .via(
                        tableRow ->
                            KV.of(
                                Long.parseLong((String) tableRow.get(criteriaIdColName)),
                                (Double) tableRow.get(numericColName))));

    // Compute numeric range for each criteriaId.
    PCollection<DisplayHintUtils.IdNumericRange> numericRanges =
        DisplayHintUtils.numericRangeHint(criteriaValuePairs);

    // Build BQ rows to insert: (id=criteriaId, min=min, max=max).
    // Write rows to BQ: (criteriaId, attributeName, minValue, maxValue).
    numericRanges
        .apply(
            MapElements.into(TypeDescriptor.of(TableRow.class))
                .via(
                    idNumericRange ->
                        new TableRow()
                            .set("id", idNumericRange.getId())
                            .set("min", idNumericRange.getMin())
                            .set("max", idNumericRange.getMax())))
        .apply(
            BigQueryIO.writeTableRows()
                .to(getAuxiliaryTable().getPathForIndexing())
                .withSchema(ROLLUPS_TABLE_SCHEMA)
                .withCreateDisposition(BigQueryIO.Write.CreateDisposition.CREATE_IF_NEEDED)
                .withWriteDisposition(BigQueryIO.Write.WriteDisposition.WRITE_EMPTY)
                .withMethod(BigQueryIO.Write.Method.FILE_LOADS));
  }

  /** Compute the possible enum values and counts for each criteriaId and write it to BQ. */
  private void enumValHint(
      PCollection<TableRow> allInstances,
      String enumValColName,
      String enumDisplayColName,
      String criteriaIdColName,
      String primaryIdColName) {
    // Remove rows with a null value.
    // Build key-value pairs: [key] criteriaId+attribute value/display, [value] primaryId.
    PCollection<KV<DisplayHintUtils.IdEnumValue, Long>> criteriaEnumPrimaryPairs =
        allInstances
            .apply(
                Filter.by(
                    tableRow ->
                        tableRow.get(criteriaIdColName) != null
                            && tableRow.get(enumValColName) != null))
            .apply(
                MapElements.into(
                        TypeDescriptors.kvs(
                            new TypeDescriptor<DisplayHintUtils.IdEnumValue>() {},
                            TypeDescriptors.longs()))
                    .via(
                        tableRow -> {
                          Long criteriaId =
                              Long.parseLong((String) tableRow.get(criteriaIdColName));
                          String enumValue = (String) tableRow.get(enumValColName);
                          String enumDisplay = (String) tableRow.get(enumDisplayColName);
                          Long primaryId = Long.parseLong((String) tableRow.get(primaryIdColName));
                          return KV.of(
                              new DisplayHintUtils.IdEnumValue(criteriaId, enumValue, enumDisplay),
                              primaryId);
                        }));

    // Compute enum values and counts for each criteriaId.
    PCollection<DisplayHintUtils.IdEnumValue> enumValueCounts =
        DisplayHintUtils.enumValHint(criteriaEnumPrimaryPairs);

    // Build BQ rows to insert: (id=criteriaId, enumValue=enumValue, enumDisplay=enumDisplay,
    // enumCount=count).
    // Write rows to BQ: (criteriaId, attributeName, value, display, numPrimaryIds).
    enumValueCounts
        .apply(
            MapElements.into(TypeDescriptor.of(TableRow.class))
                .via(
                    idEnumValue ->
                        new TableRow()
                            .set("id", idEnumValue.getId())
                            .set("enumValue", Long.parseLong(idEnumValue.getEnumValue()))
                            .set("enumDisplay", idEnumValue.getEnumDisplay())
                            .set("enumCount", idEnumValue.getCount())))
        .apply(
            BigQueryIO.writeTableRows()
                .to(getAuxiliaryTable().getPathForIndexing())
                .withSchema(ROLLUPS_TABLE_SCHEMA)
                .withCreateDisposition(BigQueryIO.Write.CreateDisposition.CREATE_IF_NEEDED)
                .withWriteDisposition(BigQueryIO.Write.WriteDisposition.WRITE_APPEND)
                .withMethod(BigQueryIO.Write.Method.FILE_LOADS));
  }
}

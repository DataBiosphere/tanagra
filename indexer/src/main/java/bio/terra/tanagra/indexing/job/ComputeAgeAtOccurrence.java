package bio.terra.tanagra.indexing.job;

import static bio.terra.tanagra.underlay.entitygroup.CriteriaOccurrence.AGE_AT_OCCURRENCE_ATTRIBUTE_NAME;

import bio.terra.tanagra.indexing.BigQueryIndexingJob;
import bio.terra.tanagra.indexing.job.beam.BigQueryUtils;
import bio.terra.tanagra.query.ColumnSchema;
import bio.terra.tanagra.query.FieldPointer;
import bio.terra.tanagra.query.FieldVariable;
import bio.terra.tanagra.query.Literal;
import bio.terra.tanagra.query.Literal.DataType;
import bio.terra.tanagra.query.Query;
import bio.terra.tanagra.query.TablePointer;
import bio.terra.tanagra.query.TableVariable;
import bio.terra.tanagra.query.filtervariable.BinaryFilterVariable;
import bio.terra.tanagra.query.filtervariable.BinaryFilterVariable.BinaryOperator;
import bio.terra.tanagra.underlay.AttributeMapping;
import bio.terra.tanagra.underlay.Entity;
import bio.terra.tanagra.underlay.Relationship;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.Underlay.MappingType;
import com.google.api.services.bigquery.model.TableRow;
import com.google.common.collect.Lists;
import com.google.common.collect.MoreCollectors;
import com.google.common.collect.Streams;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.io.gcp.bigquery.BigQueryIO;
import org.apache.beam.sdk.io.gcp.bigquery.BigQueryIO.Write.WriteDisposition;
import org.apache.beam.sdk.transforms.FlatMapElements;
import org.apache.beam.sdk.transforms.GroupByKey;
import org.apache.beam.sdk.transforms.MapElements;
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

/**
 * Populate age_at_occurrence column. This column only exists for occurrence entities of
 * CRITERIA_OCCURRENCE entity groups.
 */
public class ComputeAgeAtOccurrence extends BigQueryIndexingJob {
  private static final Logger LOGGER = LoggerFactory.getLogger(ComputeAgeAtOccurrence.class);

  private final Relationship occurrencePrimaryRelationship;

  public ComputeAgeAtOccurrence(Entity entity, Relationship occurrencePrimaryRelationship) {
    super(entity);
    this.occurrencePrimaryRelationship = occurrencePrimaryRelationship;
  }

  @Override
  public String getName() {
    return "COMPUTE AGE AT OCCURRENCE (" + getEntity().getName() + ")";
  }

  @Override
  public void run(boolean isDryRun) {
    Entity primaryEntity = occurrencePrimaryRelationship.getEntityB();
    Entity occurrenceEntity = occurrencePrimaryRelationship.getEntityA();
    TablePointer indexTablePointer = primaryEntity.getMapping(MappingType.INDEX).getTablePointer();
    Pipeline pipeline =
        Pipeline.create(buildDataflowPipelineOptions(getBQDataPointer(indexTablePointer)));

    // Create map from primary id to date of birth.
    PCollection<KV<String, LocalDate>> primaryIdToDateOfBirths =
        getPrimaryIdToDateOfBirths(pipeline, primaryEntity);

    // Create map from primary id to a list of TableRow containing occurrence id, occurrence start
    // dates. We fetch occurrence start dates from source, since there may not be an attribute for
    // occurrence start date.
    String primaryIdColumnName =
        primaryEntity.getIdAttribute().getMapping(MappingType.SOURCE).getValue().getColumnName();
    PCollection<KV<String, Iterable<TableRow>>> primaryIdToSourceOccurrenceStartDates =
        getPrimaryIdToSourceOccurrenceStartDates(pipeline, occurrenceEntity, primaryIdColumnName);

    // Create map from primary id to a list of index occurrence rows for that person. This will be
    // used to rewrite index occurrence table with age_of_occurrence filled in.
    PCollection<KV<String, Iterable<TableRow>>> primaryIdToIndexOccurrenceRows =
        getPrimaryIdToIndexOccurrenceRows(pipeline, occurrenceEntity, primaryIdColumnName);

    // Join on primary id. Create a map from primary id to list of index occurrence rows, with
    // age_at_occurrence filled in for each row.
    PCollection<KV<String, List<TableRow>>> primaryIdToIndexOccurrenceRowsWithAgeAtOccurrence =
        getPrimaryIdToIndexOccurrenceRowsWithAgeOfOccurrence(
            occurrenceEntity,
            primaryIdToDateOfBirths,
            primaryIdToSourceOccurrenceStartDates,
            primaryIdToIndexOccurrenceRows);

    // Flatten to PCollection<TableRow>
    PCollection<TableRow> indexRowsWithAgeAtOccurrence =
        primaryIdToIndexOccurrenceRowsWithAgeAtOccurrence.apply(
            FlatMapElements.into(TypeDescriptor.of(TableRow.class))
                .via((KV<String, List<TableRow>> kv) -> kv.getValue()));

    // Write to BQ
    indexRowsWithAgeAtOccurrence.apply(
        BigQueryIO.writeTableRows()
            .to(
                occurrenceEntity
                    .getMapping(MappingType.INDEX)
                    .getTablePointer()
                    .getPathForIndexing())
            .withCreateDisposition(BigQueryIO.Write.CreateDisposition.CREATE_NEVER)
            .withWriteDisposition(WriteDisposition.WRITE_TRUNCATE));

    if (!isDryRun) {
      pipeline.run().waitUntilFinish();
    }
  }

  @Override
  public JobStatus checkStatus() {
    // Check if the table already exists.
    if (!checkTableExists(getEntityIndexTable())) {
      return JobStatus.NOT_STARTED;
    }

    // Check if the table has at least 1 row where age_of_occurrence IS NOT NULL
    FieldPointer field =
        new FieldPointer.Builder()
            .tablePointer(getEntityIndexTable())
            .columnName(AGE_AT_OCCURRENCE_ATTRIBUTE_NAME)
            .build();
    ColumnSchema columnSchema =
        getEntity()
            .getAttribute(AGE_AT_OCCURRENCE_ATTRIBUTE_NAME)
            .getMapping(Underlay.MappingType.INDEX)
            .buildValueColumnSchema();
    return checkOneNotNullRowExists(field, columnSchema)
        ? JobStatus.COMPLETE
        : JobStatus.NOT_STARTED;
  }

  @Override
  public void clean(boolean isDryRun) {
    LOGGER.info(
        "Nothing to clean. CreateEntityTable will delete the output table, which includes all the rows inserted by this job.");
  }

  /**
   * Returns map from primary id to date of birth. Regardless of what type id column is, id is
   * converted to string.
   */
  private PCollection<KV<String, LocalDate>> getPrimaryIdToDateOfBirths(
      Pipeline pipeline, Entity primaryEntity) {
    TableVariable tableVar =
        TableVariable.forPrimary(primaryEntity.getMapping(MappingType.SOURCE).getTablePointer());
    List<TableVariable> tableVars = List.of(tableVar);
    FieldVariable idFieldVar =
        primaryEntity
            .getIdAttribute()
            .getMapping(MappingType.SOURCE)
            .buildFieldVariables(tableVar, tableVars)
            .get(0);
    String startDateColumnName = primaryEntity.getStartDateColumn().getColumnName();
    FieldVariable startDateFieldVar =
        new FieldVariable(primaryEntity.getStartDateColumn(), tableVar, startDateColumnName);
    Query query =
        new Query.Builder()
            .select(List.of(idFieldVar, startDateFieldVar))
            .where(
                new BinaryFilterVariable(
                    idFieldVar,
                    BinaryOperator.EQUALS,
                    new Literal.Builder().dataType(DataType.INT64).int64Val(1107050).build()))
            .tables(tableVars)
            .build();
    String sql = query.renderSQL();
    LOGGER.info("Select date of births SQL: {}", sql);

    String idColumnName =
        primaryEntity.getIdAttribute().getMapping(MappingType.INDEX).getValue().getColumnName();
    return pipeline
        .apply(
            "Read primary id and date of birth",
            BigQueryIO.readTableRows()
                .fromQuery(sql)
                .withMethod(BigQueryIO.TypedRead.Method.EXPORT)
                .usingStandardSql())
        .apply(
            MapElements.into(
                    TypeDescriptors.kvs(
                        TypeDescriptors.strings(), TypeDescriptor.of(LocalDate.class)))
                .via(
                    (TableRow row) -> {
                      String id = (String) row.get(idColumnName);
                      LocalDate startDate =
                          BigQueryUtils.toLocalDate((String) row.get(startDateColumnName));
                      return KV.of(id, startDate);
                    }));
  }

  /**
   * Returns map from primary id to list of TableRows containing occurrence id, occurrence start
   * date.
   */
  public static PCollection<KV<String, Iterable<TableRow>>>
      getPrimaryIdToSourceOccurrenceStartDates(
          Pipeline pipeline, Entity occurrenceEntity, String primaryIdColumnName) {
    // Get occurrence start date from source. There might not be an attribute for it, so it might
    // not be in the index.
    TablePointer tablePointer = occurrenceEntity.getMapping(MappingType.SOURCE).getTablePointer();
    TableVariable tableVar = TableVariable.forPrimary(tablePointer);
    List<TableVariable> tableVars = Lists.newArrayList(tableVar);
    FieldVariable occurrenceIdFieldVar =
        occurrenceEntity
            .getIdAttribute()
            .getMapping(MappingType.SOURCE)
            .buildFieldVariables(tableVar, tableVars)
            .get(0);
    FieldVariable primaryIdFieldVar =
        new FieldVariable(
            new FieldPointer.Builder()
                .tablePointer(tablePointer)
                .columnName(primaryIdColumnName)
                .build(),
            tableVar,
            primaryIdColumnName);
    String occurrenceStartDateColumnName = occurrenceEntity.getStartDateColumn().getColumnName();
    FieldVariable occurrenceStartDateFieldVar =
        new FieldVariable(
            occurrenceEntity.getStartDateColumn(), tableVar, occurrenceStartDateColumnName);
    Query query =
        new Query.Builder()
            .select(List.of(occurrenceIdFieldVar, primaryIdFieldVar, occurrenceStartDateFieldVar))
            .where(
                new BinaryFilterVariable(
                    primaryIdFieldVar,
                    BinaryOperator.EQUALS,
                    new Literal.Builder().dataType(DataType.INT64).int64Val(1107050).build()))
            .tables(tableVars)
            .build();
    String sql = query.renderSQL();
    LOGGER.info("Select occurrence start dates SQL: {}", sql);

    return pipeline
        .apply(
            String.format("Read id and start date for %s", occurrenceEntity.getName()),
            BigQueryIO.readTableRows()
                .fromQuery(sql)
                .withMethod(BigQueryIO.TypedRead.Method.EXPORT)
                .usingStandardSql())
        .apply(
            MapElements.into(
                    TypeDescriptors.kvs(
                        TypeDescriptors.strings(), TypeDescriptor.of(TableRow.class)))
                .via(
                    (TableRow row) -> {
                      String primaryId = (String) row.get(primaryIdColumnName);
                      return KV.of(primaryId, row);
                    }))
        .apply(
            String.format("Group occurrence start dates by primary id"),
            GroupByKey.<String, TableRow>create());
  }

  /** Returns map from primary id to a list of index occurrence rows for that person. */
  private PCollection<KV<String, Iterable<TableRow>>> getPrimaryIdToIndexOccurrenceRows(
      Pipeline pipeline, Entity occurrenceEntity, String primaryIdColumnName) {
    TableVariable tableVar =
        TableVariable.forPrimary(occurrenceEntity.getMapping(MappingType.INDEX).getTablePointer());
    List<TableVariable> tableVars = Lists.newArrayList(tableVar);

    // build the SELECT field variables and column schemas from attributes
    List<FieldVariable> selectFieldVars = new ArrayList<>();
    List<ColumnSchema> columnSchemas = new ArrayList<>();
    getEntity().getAttributes().stream()
        .forEach(
            attribute -> {
              AttributeMapping attributeMapping = attribute.getMapping(MappingType.INDEX);
              selectFieldVars.addAll(attributeMapping.buildFieldVariables(tableVar, tableVars));
              columnSchemas.addAll(attributeMapping.buildColumnSchemas());
            });

    FieldVariable primaryIdFieldVar =
        occurrenceEntity
            .getAttribute("person_id")
            .getMapping(MappingType.INDEX)
            .buildFieldVariables(tableVar, tableVars)
            .get(0);
    Query query =
        new Query.Builder()
            .select(selectFieldVars)
            .where(
                new BinaryFilterVariable(
                    primaryIdFieldVar,
                    BinaryOperator.EQUALS,
                    new Literal.Builder().dataType(DataType.INT64).int64Val(1107050).build()))
            .tables(tableVars)
            .build();
    String sql = query.renderSQL();
    LOGGER.info("Read index occurrence table SQL: {}", sql);

    return pipeline
        .apply(
            String.format("Read index occurrence table: %s", occurrenceEntity.getName()),
            BigQueryIO.readTableRows()
                .fromQuery(sql)
                .withMethod(BigQueryIO.TypedRead.Method.EXPORT)
                .usingStandardSql())
        .apply(
            String.format("Create kv of primary id and index occurrence TableRow"),
            MapElements.into(
                    TypeDescriptors.kvs(
                        TypeDescriptors.strings(), TypeDescriptor.of(TableRow.class)))
                .via(
                    (TableRow row) -> {
                      String primaryId = (String) row.get(primaryIdColumnName);
                      return KV.of(primaryId, row);
                    }))
        .apply(
            String.format("Group index occurrence TableRows by primary id"),
            GroupByKey.<String, TableRow>create());
  }

  private static PCollection<KV<String, List<TableRow>>>
      getPrimaryIdToIndexOccurrenceRowsWithAgeOfOccurrence(
          Entity occurrenceEntity,
          PCollection<KV<String, LocalDate>> primaryIdToDateOfBirths,
          PCollection<KV<String, Iterable<TableRow>>> primaryIdToSourceOccurrenceStartDates,
          PCollection<KV<String, Iterable<TableRow>>> primaryIdToIndexOccurrenceRows) {
    TupleTag<LocalDate> dateOfBirthTag = new TupleTag<>();
    TupleTag<Iterable<TableRow>> sourceOccurrenceStartDatesTag = new TupleTag<>();
    TupleTag<Iterable<TableRow>> indexOccurrenceRowsTag = new TupleTag<>();
    String sourceOccurrenceIdAlias = occurrenceEntity.getIdAttribute().getName();
    String sourceOccurrenceStartDateColumnName =
        occurrenceEntity.getStartDateColumn().getColumnName();
    String indexOccurrenceIdColumnName =
        occurrenceEntity.getIdAttribute().getMapping(MappingType.INDEX).getValue().getColumnName();
    return KeyedPCollectionTuple.of(dateOfBirthTag, primaryIdToDateOfBirths)
        .and(sourceOccurrenceStartDatesTag, primaryIdToSourceOccurrenceStartDates)
        .and(indexOccurrenceRowsTag, primaryIdToIndexOccurrenceRows)
        .apply(
            "Join date of birth, occurrence start dates, and index occurrence rows on primary id",
            CoGroupByKey.create())
        .apply(
            "Compute age at occurrence and set in index occurrence TableRows",
            MapElements.into(
                    TypeDescriptors.kvs(
                        TypeDescriptors.strings(),
                        TypeDescriptors.lists(TypeDescriptor.of(TableRow.class))))
                .via(
                    (KV<String, CoGbkResult> result) -> {
                      LocalDate dateOfBirth = result.getValue().getOnly(dateOfBirthTag);
                      Iterable<TableRow> sourceOccurrenceStartDates =
                          result.getValue().getOnly(sourceOccurrenceStartDatesTag);
                      Iterable<TableRow> indexRows =
                          result.getValue().getOnly(indexOccurrenceRowsTag);
                      List<TableRow> indexRowsWithAgeAtOccurrence =
                          Streams.stream(indexRows)
                              .map(
                                  indexRow -> {
                                    String occurrenceId =
                                        (String) indexRow.get(indexOccurrenceIdColumnName);
                                    LocalDate occurrenceStartDate =
                                        Streams.stream(sourceOccurrenceStartDates)
                                            .filter(
                                                sourceOccurrenceIdAndStartDateTableRow ->
                                                    sourceOccurrenceIdAndStartDateTableRow
                                                        .get(sourceOccurrenceIdAlias)
                                                        .equals(occurrenceId))
                                            .map(
                                                sourceOccurrenceIdAndStartDateTableRow ->
                                                    BigQueryUtils.toLocalDate(
                                                        (String)
                                                            sourceOccurrenceIdAndStartDateTableRow
                                                                .get(
                                                                    sourceOccurrenceStartDateColumnName)))
                                            .collect(MoreCollectors.onlyElement());
                                    int ageAtOccurrence =
                                        Period.between(dateOfBirth, occurrenceStartDate).getYears();
                                    return indexRow
                                        .clone()
                                        .set(AGE_AT_OCCURRENCE_ATTRIBUTE_NAME, ageAtOccurrence);
                                  })
                              .collect(Collectors.toList());
                      return KV.of(result.getKey(), indexRowsWithAgeAtOccurrence);
                    }));
  }
}

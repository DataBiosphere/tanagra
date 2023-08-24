package bio.terra.tanagra.indexing.job;

import bio.terra.tanagra.api.schemas.InstanceLevelDisplayHints;
import bio.terra.tanagra.exception.SystemException;
import bio.terra.tanagra.indexing.BigQueryIndexingJob;
import bio.terra.tanagra.indexing.job.beam.DisplayHintUtils;
import bio.terra.tanagra.query.Query;
import bio.terra.tanagra.query.TablePointer;
import bio.terra.tanagra.query.bigquery.BigQuerySchemaUtils;
import bio.terra.tanagra.underlay.Attribute;
import bio.terra.tanagra.underlay.Entity;
import bio.terra.tanagra.underlay.RelationshipMapping;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.entitygroup.CriteriaOccurrence;
import com.google.api.services.bigquery.model.TableRow;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.io.gcp.bigquery.BigQueryIO;
import org.apache.beam.sdk.transforms.Filter;
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

public class ComputeModifierDisplayHints extends BigQueryIndexingJob {
  private static final Logger LOGGER = LoggerFactory.getLogger(ComputeModifierDisplayHints.class);

  private final CriteriaOccurrence criteriaOccurrence;
  private final Entity occurrenceEntity;

  public ComputeModifierDisplayHints(
      CriteriaOccurrence criteriaOccurrence, Entity occurrenceEntity) {
    super(occurrenceEntity);
    this.criteriaOccurrence = criteriaOccurrence;
    this.occurrenceEntity = occurrenceEntity;
  }

  @Override
  public String getName() {
    return "COMPUTE DISPLAY HINTS (" + criteriaOccurrence.getName() + ")";
  }

  @Override
  public void run(boolean isDryRun) {
    Pipeline pipeline =
        Pipeline.create(buildDataflowPipelineOptions(getBQDataPointer(getAuxiliaryTable())));

    // Read in the occurrence attributes that we want to compute a display hint for.
    PCollection<KV<Long, TableRow>> occAllAttrs = readInOccAllAttrs(pipeline);

    // Read in the criteria-occurrence id pairs.
    PCollection<KV<Long, Long>> occCriIdPairs =
        readInIdPairs(
            criteriaOccurrence
                .getOccurrenceCriteriaRelationship(occurrenceEntity)
                .getMapping(Underlay.MappingType.SOURCE),
            pipeline);

    // Read in the primary-occurrence id pairs.
    PCollection<KV<Long, Long>> occPriIdPairs =
        readInIdPairs(
            criteriaOccurrence
                .getOccurrencePrimaryRelationship(occurrenceEntity)
                .getMapping(Underlay.MappingType.SOURCE),
            pipeline);

    for (Attribute attr : criteriaOccurrence.getModifierAttributes()) {
      if (Attribute.Type.KEY_AND_DISPLAY.equals(attr.getType())) {
        enumValHint(occCriIdPairs, occPriIdPairs, occAllAttrs, attr);
      } else {
        switch (attr.getDataType()) {
          case BOOLEAN:
          case STRING:
          case DATE:
            // TODO: Calculate display hints for other data types.
            continue;
          case INT64:
          case DOUBLE:
            numericRangeHint(occCriIdPairs, occAllAttrs, attr);
            break;
          default:
            throw new SystemException("Unknown attribute data type: " + attr.getDataType());
        }
      }
    }

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
    return criteriaOccurrence
        .getModifierAuxiliaryData()
        .getMapping(Underlay.MappingType.INDEX)
        .getTablePointer();
  }

  private PCollection<KV<Long, TableRow>> readInOccAllAttrs(Pipeline pipeline) {
    Query occAllAttrsQ =
        occurrenceEntity.getMapping(Underlay.MappingType.INDEX).queryAllAttributes();
    LOGGER.info("occAllAttrsQ: {}", occAllAttrsQ.renderSQL());
    String occIdName = occurrenceEntity.getIdAttribute().getName();
    LOGGER.info("occIdName: {}", occIdName);
    return pipeline
        .apply(
            BigQueryIO.readTableRows()
                .fromQuery(occAllAttrsQ.renderSQL())
                .withMethod(BigQueryIO.TypedRead.Method.EXPORT)
                .usingStandardSql())
        .apply(
            MapElements.into(
                    TypeDescriptors.kvs(TypeDescriptors.longs(), TypeDescriptor.of(TableRow.class)))
                .via(
                    tableRow -> KV.of(Long.parseLong((String) tableRow.get(occIdName)), tableRow)));
  }

  private PCollection<KV<Long, Long>> readInIdPairs(
      RelationshipMapping relationshipMapping, Pipeline pipeline) {
    Query idPairsQ = relationshipMapping.queryIdPairs("idA", "idB");
    LOGGER.info("idPairsQ: {}", idPairsQ.renderSQL());
    return pipeline
        .apply(
            BigQueryIO.readTableRows()
                .fromQuery(idPairsQ.renderSQL())
                .withMethod(BigQueryIO.TypedRead.Method.EXPORT)
                .usingStandardSql())
        .apply(
            MapElements.into(TypeDescriptors.kvs(TypeDescriptors.longs(), TypeDescriptors.longs()))
                .via(
                    tableRow ->
                        KV.of(
                            Long.parseLong((String) tableRow.get("idA")),
                            Long.parseLong((String) tableRow.get("idB")))));
  }

  /** Compute the numeric range for each criteriaId and write it to BQ. */
  private void numericRangeHint(
      PCollection<KV<Long, Long>> occCriIdPairs,
      PCollection<KV<Long, TableRow>> occAllAttrs,
      Attribute numericAttr) {
    String numValColName = numericAttr.getName();
    LOGGER.info("numValColName: {}", numValColName);

    // Remove rows with a null value.
    PCollection<KV<Long, Double>> occIdNumValPairs =
        occAllAttrs
            .apply(
                Filter.by(
                    occIdAndTableRow ->
                        occIdAndTableRow.getValue().get(numValColName) != null
                            && !occIdAndTableRow
                                .getValue()
                                .get(numValColName)
                                .toString()
                                .isEmpty()))
            .apply(
                MapElements.into(
                        TypeDescriptors.kvs(TypeDescriptors.longs(), TypeDescriptors.doubles()))
                    .via(
                        occIdAndTableRow -> {
                          Double doubleVal;
                          try {
                            doubleVal = (Double) occIdAndTableRow.getValue().get(numValColName);
                          } catch (ClassCastException ccEx) {
                            doubleVal = Double.MIN_VALUE;
                          }
                          return KV.of(occIdAndTableRow.getKey(), doubleVal);
                        }));

    // Build key-value pairs: [key] criteriaId, [value] attribute value.
    final TupleTag<Long> criIdTag = new TupleTag<>();
    final TupleTag<Double> numValTag = new TupleTag<>();
    PCollection<KV<Long, CoGbkResult>> occIdAndNumValCriId =
        KeyedPCollectionTuple.of(criIdTag, occCriIdPairs)
            .and(numValTag, occIdNumValPairs)
            .apply(CoGroupByKey.create());
    PCollection<KV<Long, Double>> criteriaValuePairs =
        occIdAndNumValCriId
            .apply(Filter.by(cogb -> cogb.getValue().getAll(numValTag).iterator().hasNext()))
            .apply(
                MapElements.into(
                        TypeDescriptors.kvs(TypeDescriptors.longs(), TypeDescriptors.doubles()))
                    .via(
                        cogb ->
                            KV.of(
                                cogb.getValue().getOnly(criIdTag),
                                cogb.getValue().getOnly(numValTag))));

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
                            .set("entity_id", idNumericRange.getId())
                            .set("attribute_name", numValColName)
                            .set("min", idNumericRange.getMin())
                            .set("max", idNumericRange.getMax())))
        .apply(
            BigQueryIO.writeTableRows()
                .to(getAuxiliaryTable().getPathForIndexing())
                .withSchema(
                    BigQuerySchemaUtils.getBigQueryTableSchema(
                        InstanceLevelDisplayHints.getColumns()))
                .withCreateDisposition(BigQueryIO.Write.CreateDisposition.CREATE_IF_NEEDED)
                .withWriteDisposition(BigQueryIO.Write.WriteDisposition.WRITE_APPEND)
                .withMethod(BigQueryIO.Write.Method.FILE_LOADS));
  }

  /** Compute the possible enum values and counts for each criteriaId and write it to BQ. */
  private void enumValHint(
      PCollection<KV<Long, Long>> occCriIdPairs,
      PCollection<KV<Long, Long>> occPriIdPairs,
      PCollection<KV<Long, TableRow>> occAllAttrs,
      Attribute enumAttr) {
    String enumValColName = enumAttr.getName();
    String enumDisplayColName =
        enumAttr.getMapping(Underlay.MappingType.SOURCE).getDisplayMappingAlias();
    LOGGER.info("enumValColName: {}", enumValColName);
    LOGGER.info("enumDisplayColName: {}", enumDisplayColName);

    // Remove rows with a null value.
    PCollection<KV<Long, TableRow>> occAllAttrsNotNull =
        occAllAttrs.apply(
            Filter.by(occIdAndTableRow -> occIdAndTableRow.getValue().get(enumValColName) != null));

    // Build key-value pairs: [key] criteriaId+attribute value/display, [value] primaryId.
    final TupleTag<TableRow> occAttrsTag = new TupleTag<>();
    final TupleTag<Long> criIdTag = new TupleTag<>();
    final TupleTag<Long> priIdTag = new TupleTag<>();
    PCollection<KV<Long, CoGbkResult>> occIdAndAttrsCriIdPriId =
        KeyedPCollectionTuple.of(occAttrsTag, occAllAttrsNotNull)
            .and(criIdTag, occCriIdPairs)
            .and(priIdTag, occPriIdPairs)
            .apply(CoGroupByKey.create());
    PCollection<KV<DisplayHintUtils.IdEnumValue, Long>> criteriaEnumPrimaryPairs =
        occIdAndAttrsCriIdPriId
            .apply(Filter.by(cogb -> cogb.getValue().getAll(occAttrsTag).iterator().hasNext()))
            .apply(
                MapElements.into(
                        TypeDescriptors.kvs(
                            new TypeDescriptor<DisplayHintUtils.IdEnumValue>() {},
                            TypeDescriptors.longs()))
                    .via(
                        cogb -> {
                          Long criId = cogb.getValue().getOnly(criIdTag);
                          Long priId = cogb.getValue().getOnly(priIdTag);

                          TableRow occAttrs = cogb.getValue().getOnly(occAttrsTag);
                          String enumValue = (String) occAttrs.get(enumValColName);
                          String enumDisplay = (String) occAttrs.get(enumDisplayColName);

                          return KV.of(
                              new DisplayHintUtils.IdEnumValue(criId, enumValue, enumDisplay),
                              priId);
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
                            .set("entity_id", idEnumValue.getId())
                            .set("attribute_name", enumValColName)
                            .set("enum_value", Long.parseLong(idEnumValue.getEnumValue()))
                            .set("enum_display", idEnumValue.getEnumDisplay())
                            .set("enum_count", idEnumValue.getCount())))
        .apply(
            BigQueryIO.writeTableRows()
                .to(getAuxiliaryTable().getPathForIndexing())
                .withSchema(
                    BigQuerySchemaUtils.getBigQueryTableSchema(
                        InstanceLevelDisplayHints.getColumns()))
                .withCreateDisposition(BigQueryIO.Write.CreateDisposition.CREATE_IF_NEEDED)
                .withWriteDisposition(BigQueryIO.Write.WriteDisposition.WRITE_APPEND)
                .withMethod(BigQueryIO.Write.Method.FILE_LOADS));
  }
}

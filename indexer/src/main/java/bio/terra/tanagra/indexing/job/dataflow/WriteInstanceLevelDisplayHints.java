package bio.terra.tanagra.indexing.job.dataflow;

import bio.terra.tanagra.api.shared.DataType;
import bio.terra.tanagra.indexing.job.BigQueryJob;
import bio.terra.tanagra.indexing.job.dataflow.beam.BigQueryBeamUtils;
import bio.terra.tanagra.indexing.job.dataflow.beam.DataflowUtils;
import bio.terra.tanagra.query.sql.SqlField;
import bio.terra.tanagra.query.sql.SqlQueryField;
import bio.terra.tanagra.underlay.entitymodel.Attribute;
import bio.terra.tanagra.underlay.entitymodel.Entity;
import bio.terra.tanagra.underlay.entitymodel.Relationship;
import bio.terra.tanagra.underlay.entitymodel.entitygroup.CriteriaOccurrence;
import bio.terra.tanagra.underlay.indextable.ITEntityMain;
import bio.terra.tanagra.underlay.indextable.ITInstanceLevelDisplayHints;
import bio.terra.tanagra.underlay.indextable.ITRelationshipIdPairs;
import bio.terra.tanagra.underlay.serialization.SZIndexer;
import com.google.api.services.bigquery.model.TableRow;
import java.io.Serializable;
import javax.annotation.Nullable;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.io.gcp.bigquery.BigQueryIO;
import org.apache.beam.sdk.transforms.Count;
import org.apache.beam.sdk.transforms.Distinct;
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

public class WriteInstanceLevelDisplayHints extends BigQueryJob {
  private static final Logger LOGGER =
      LoggerFactory.getLogger(WriteInstanceLevelDisplayHints.class);

  private final CriteriaOccurrence criteriaOccurrence;
  private final Entity occurrenceEntity;
  private final ITEntityMain criteriaEntityIndexTable;
  private final ITEntityMain occurrenceEntityIndexTable;
  private final ITEntityMain primaryEntityIndexTable;
  private final @Nullable ITRelationshipIdPairs occurrenceCriteriaRelationshipIdPairsTable;
  private final @Nullable ITRelationshipIdPairs occurrencePrimaryRelationshipIdPairsTable;
  private final ITInstanceLevelDisplayHints indexTable;

  @SuppressWarnings("checkstyle:ParameterNumber")
  public WriteInstanceLevelDisplayHints(
      SZIndexer indexerConfig,
      CriteriaOccurrence criteriaOccurrence,
      Entity occurrenceEntity,
      ITEntityMain criteriaEntityIndexTable,
      ITEntityMain occurrenceEntityIndexTable,
      ITEntityMain primaryEntityIndexTable,
      @Nullable ITRelationshipIdPairs occurrenceCriteriaRelationshipIdPairsTable,
      @Nullable ITRelationshipIdPairs occurrencePrimaryRelationshipIdPairsTable,
      ITInstanceLevelDisplayHints indexTable) {
    super(indexerConfig);
    this.criteriaOccurrence = criteriaOccurrence;
    this.occurrenceEntity = occurrenceEntity;
    this.criteriaEntityIndexTable = criteriaEntityIndexTable;
    this.occurrenceEntityIndexTable = occurrenceEntityIndexTable;
    this.primaryEntityIndexTable = primaryEntityIndexTable;
    this.occurrenceCriteriaRelationshipIdPairsTable = occurrenceCriteriaRelationshipIdPairsTable;
    this.occurrencePrimaryRelationshipIdPairsTable = occurrencePrimaryRelationshipIdPairsTable;
    this.indexTable = indexTable;
  }

  @Override
  public String getEntityGroup() {
    return criteriaOccurrence.getName();
  }

  @Override
  protected String getOutputTableName() {
    return indexTable.getTablePointer().getTableName();
  }

  @Override
  public JobStatus checkStatus() {
    return getOutputTable().isPresent() && outputTableHasAtLeastOneRow()
        ? JobStatus.COMPLETE
        : JobStatus.NOT_STARTED;
  }

  @Override
  public void run(boolean isDryRun) {
    // Build the pipeline object from the Dataflow config.
    Pipeline pipeline = Pipeline.create(DataflowUtils.getPipelineOptions(indexerConfig, getName()));

    // Build a query to select all occurrence instances (not just id, includes all attributes also)
    // from the index entity main table, and the pipeline steps to read the results and build a (id,
    // tablerow) KV PCollection.
    String allOccInstancesSql =
        "SELECT * FROM " + occurrenceEntityIndexTable.getTablePointer().render();
    LOGGER.info("allOccInstancesQuery: {}", allOccInstancesSql);
    PCollection<KV<Long, TableRow>> occIdRowKVs =
        readInOccRows(pipeline, allOccInstancesSql, occurrenceEntity.getIdAttribute().getName());

    // Build a query to select all occurrence-criteria id pairs, and the pipeline steps to read the
    // results and build a (occurrence id, criteria id) KV PCollection.
    final String entityAIdColumnName = "entityAId";
    final String entityBIdColumnName = "entityBId";
    String occCriIdPairsSql =
        getQueryRelationshipIdPairs(
            entityAIdColumnName,
            entityBIdColumnName,
            criteriaOccurrence.getOccurrenceCriteriaRelationship(occurrenceEntity.getName()),
            occurrenceEntityIndexTable,
            criteriaEntityIndexTable,
            occurrenceCriteriaRelationshipIdPairsTable);
    LOGGER.info("index occurrence-criteria id pairs query: {}", occCriIdPairsSql);
    PCollection<KV<Long, Long>> occCriIdPairKVs =
        readInRelationshipIdPairs(
            pipeline, occCriIdPairsSql, entityAIdColumnName, entityBIdColumnName);

    // Build a query to select all occurrence-criteria id pairs, and the pipeline steps to read the
    // results and build a (occurrence id, criteria id) KV PCollection.
    String occPriIdPairsSql =
        getQueryRelationshipIdPairs(
            entityAIdColumnName,
            entityBIdColumnName,
            criteriaOccurrence.getOccurrencePrimaryRelationship(occurrenceEntity.getName()),
            occurrenceEntityIndexTable,
            primaryEntityIndexTable,
            occurrencePrimaryRelationshipIdPairsTable);
    LOGGER.info("index occurrence-primary id pairs query: {}", occPriIdPairsSql);
    PCollection<KV<Long, Long>> occPriIdPairKVs =
        readInRelationshipIdPairs(
            pipeline, occPriIdPairsSql, entityAIdColumnName, entityBIdColumnName);

    criteriaOccurrence
        .getAttributesWithInstanceLevelDisplayHints(occurrenceEntity)
        .forEach(
            attribute -> {
              if (attribute.isValueDisplay()) {
                LOGGER.info("enum val hint: {}", attribute.getName());
                enumValHint(occCriIdPairKVs, occPriIdPairKVs, occIdRowKVs, attribute);
              } else if (DataType.INT64.equals(attribute.getDataType())
                  || DataType.DOUBLE.equals(attribute.getDataType())) {
                LOGGER.info("numeric range hint: {}", attribute.getName());
                numericRangeHint(occCriIdPairKVs, occIdRowKVs, attribute);
              } // TODO: Calculate display hints for other data types.
            });

    // Kick off the pipeline.
    if (!isDryRun) {
      pipeline.run().waitUntilFinish();
    }
  }

  private static PCollection<KV<Long, TableRow>> readInOccRows(
      Pipeline pipeline, String allOccInstancesQuery, String occurrenceEntityIdAttribute) {
    return pipeline
        .apply(
            BigQueryIO.readTableRows()
                .fromQuery(allOccInstancesQuery)
                .withMethod(BigQueryIO.TypedRead.Method.EXPORT)
                .usingStandardSql())
        .apply(
            MapElements.into(
                    TypeDescriptors.kvs(TypeDescriptors.longs(), TypeDescriptor.of(TableRow.class)))
                .via(
                    tableRow ->
                        KV.of(
                            Long.parseLong((String) tableRow.get(occurrenceEntityIdAttribute)),
                            tableRow)));
  }

  private static PCollection<KV<Long, Long>> readInRelationshipIdPairs(
      Pipeline pipeline,
      String idPairsQuery,
      String entityAIdColumnName,
      String entityBIdColumnName) {
    return pipeline
        .apply(
            BigQueryIO.readTableRows()
                .fromQuery(idPairsQuery)
                .withMethod(BigQueryIO.TypedRead.Method.EXPORT)
                .usingStandardSql())
        .apply(
            MapElements.into(TypeDescriptors.kvs(TypeDescriptors.longs(), TypeDescriptors.longs()))
                .via(
                    tableRow ->
                        KV.of(
                            Long.parseLong((String) tableRow.get(entityAIdColumnName)),
                            Long.parseLong((String) tableRow.get(entityBIdColumnName)))));
  }

  private static String getQueryRelationshipIdPairs(
      String entityAIdColumnName,
      String entityBIdColumnName,
      Relationship relationship,
      ITEntityMain entityAIndexTable,
      ITEntityMain entityBIndexTable,
      @Nullable ITRelationshipIdPairs relationshipIdPairsTable) {
    String idPairsSql;
    if (relationship.isForeignKeyAttributeEntityA()) {
      SqlField entityAIdField =
          entityAIndexTable.getAttributeValueField(
              relationship.getEntityA().getIdAttribute().getName());
      SqlField entityBIdField =
          entityAIndexTable.getAttributeValueField(
              relationship.getForeignKeyAttributeEntityA().getName());
      idPairsSql =
          "SELECT "
              + SqlQueryField.of(entityAIdField, entityAIdColumnName).renderForSelect()
              + ", "
              + SqlQueryField.of(entityBIdField, entityBIdColumnName).renderForSelect()
              + " FROM "
              + entityAIndexTable.getTablePointer().render();
    } else if (relationship.isForeignKeyAttributeEntityB()) {
      SqlField entityAIdField =
          entityBIndexTable.getAttributeValueField(
              relationship.getForeignKeyAttributeEntityB().getName());
      SqlField entityBIdField =
          entityBIndexTable.getAttributeValueField(
              relationship.getEntityB().getIdAttribute().getName());
      idPairsSql =
          "SELECT "
              + SqlQueryField.of(entityAIdField, entityAIdColumnName).renderForSelect()
              + ", "
              + SqlQueryField.of(entityBIdField, entityBIdColumnName).renderForSelect()
              + " FROM "
              + entityBIndexTable.getTablePointer().render();
    } else { // relationship.isIntermediateTable()
      SqlField entityAIdField = relationshipIdPairsTable.getEntityAIdField();
      SqlField entityBIdField = relationshipIdPairsTable.getEntityBIdField();
      idPairsSql =
          "SELECT "
              + SqlQueryField.of(entityAIdField, entityAIdColumnName).renderForSelect()
              + ", "
              + SqlQueryField.of(entityBIdField, entityBIdColumnName).renderForSelect()
              + " FROM "
              + relationshipIdPairsTable.getTablePointer().render();
    }
    return idPairsSql;
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
    PCollection<IdNumericRange> numericRanges = numericRangeHint(criteriaValuePairs);

    // Build BQ rows to insert: (id=criteriaId, min=min, max=max).
    // Write rows to BQ: (criteriaId, attributeName, minValue, maxValue).
    numericRanges
        .apply(
            MapElements.into(TypeDescriptor.of(TableRow.class))
                .via(
                    idNumericRange ->
                        new TableRow()
                            .set(
                                ITInstanceLevelDisplayHints.Column.ENTITY_ID
                                    .getSchema()
                                    .getColumnName(),
                                idNumericRange.getId())
                            .set(
                                ITInstanceLevelDisplayHints.Column.ATTRIBUTE_NAME
                                    .getSchema()
                                    .getColumnName(),
                                numValColName)
                            .set(
                                ITInstanceLevelDisplayHints.Column.MIN.getSchema().getColumnName(),
                                idNumericRange.getMin())
                            .set(
                                ITInstanceLevelDisplayHints.Column.MAX.getSchema().getColumnName(),
                                idNumericRange.getMax())))
        .apply(
            BigQueryIO.writeTableRows()
                .to(
                    BigQueryBeamUtils.getTableSqlPath(
                        indexerConfig.bigQuery.indexData.projectId,
                        indexerConfig.bigQuery.indexData.datasetId,
                        indexTable.getTablePointer().getTableName()))
                .withSchema(BigQueryBeamUtils.getBigQueryTableSchema(indexTable.getColumnSchemas()))
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
        occurrenceEntityIndexTable.getAttributeDisplayField(enumAttr.getName()).getColumnName();
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
    PCollection<KV<IdEnumValue, Long>> criteriaEnumPrimaryPairs =
        occIdAndAttrsCriIdPriId
            .apply(Filter.by(cogb -> cogb.getValue().getAll(occAttrsTag).iterator().hasNext()))
            .apply(
                MapElements.into(
                        TypeDescriptors.kvs(
                            new TypeDescriptor<IdEnumValue>() {}, TypeDescriptors.longs()))
                    .via(
                        cogb -> {
                          Long criId = cogb.getValue().getOnly(criIdTag);
                          Long priId = cogb.getValue().getOnly(priIdTag);

                          TableRow occAttrs = cogb.getValue().getOnly(occAttrsTag);
                          String enumValue = (String) occAttrs.get(enumValColName);
                          String enumDisplay = (String) occAttrs.get(enumDisplayColName);

                          return KV.of(new IdEnumValue(criId, enumValue, enumDisplay), priId);
                        }));

    // Compute enum values and counts for each criteriaId.
    PCollection<IdEnumValue> enumValueCounts = enumValHint(criteriaEnumPrimaryPairs);

    // Build BQ rows to insert: (id=criteriaId, enumValue=enumValue, enumDisplay=enumDisplay,
    // enumCount=count).
    // Write rows to BQ: (criteriaId, attributeName, value, display, numPrimaryIds).
    enumValueCounts
        .apply(
            MapElements.into(TypeDescriptor.of(TableRow.class))
                .via(
                    idEnumValue ->
                        new TableRow()
                            .set(
                                ITInstanceLevelDisplayHints.Column.ENTITY_ID
                                    .getSchema()
                                    .getColumnName(),
                                idEnumValue.getId())
                            .set(
                                ITInstanceLevelDisplayHints.Column.ATTRIBUTE_NAME
                                    .getSchema()
                                    .getColumnName(),
                                enumValColName)
                            .set(
                                ITInstanceLevelDisplayHints.Column.ENUM_VALUE
                                    .getSchema()
                                    .getColumnName(),
                                Long.parseLong(idEnumValue.getEnumValue()))
                            .set(
                                ITInstanceLevelDisplayHints.Column.ENUM_DISPLAY
                                    .getSchema()
                                    .getColumnName(),
                                idEnumValue.getEnumDisplay())
                            .set(
                                ITInstanceLevelDisplayHints.Column.ENUM_COUNT
                                    .getSchema()
                                    .getColumnName(),
                                idEnumValue.getCount())))
        .apply(
            BigQueryIO.writeTableRows()
                .to(
                    BigQueryBeamUtils.getTableSqlPath(
                        indexerConfig.bigQuery.indexData.projectId,
                        indexerConfig.bigQuery.indexData.datasetId,
                        indexTable.getTablePointer().getTableName()))
                .withSchema(BigQueryBeamUtils.getBigQueryTableSchema(indexTable.getColumnSchemas()))
                .withCreateDisposition(BigQueryIO.Write.CreateDisposition.CREATE_IF_NEEDED)
                .withWriteDisposition(BigQueryIO.Write.WriteDisposition.WRITE_APPEND)
                .withMethod(BigQueryIO.Write.Method.FILE_LOADS));
  }

  public static PCollection<IdNumericRange> numericRangeHint(
      PCollection<KV<Long, Double>> criteriaValuePairs) {
    // Combine-min by key: [key] criteriaId, [value] min attr value
    PCollection<KV<Long, Double>> criteriaMinPairs = criteriaValuePairs.apply(Min.doublesPerKey());

    // Combine-max by key: [key] criteriaId, [value] max attr value
    PCollection<KV<Long, Double>> criteriaMaxPairs = criteriaValuePairs.apply(Max.doublesPerKey());

    // Group by [key] criteriaId: [values] min, max
    final TupleTag<Double> minTag = new TupleTag<>();
    final TupleTag<Double> maxTag = new TupleTag<>();
    PCollection<KV<Long, CoGbkResult>> criteriaAndMinMax =
        KeyedPCollectionTuple.of(minTag, criteriaMinPairs)
            .and(maxTag, criteriaMaxPairs)
            .apply(CoGroupByKey.create());

    // Build NumericRange object per criteriaId to return.
    return criteriaAndMinMax.apply(
        MapElements.into(TypeDescriptor.of(IdNumericRange.class))
            .via(
                cogb -> {
                  Long criteriaId = cogb.getKey();
                  Double min = cogb.getValue().getOnly(minTag);
                  Double max = cogb.getValue().getOnly(maxTag);
                  return new IdNumericRange(criteriaId, min, max);
                }));
  }

  public static PCollection<IdEnumValue> enumValHint(
      PCollection<KV<IdEnumValue, Long>> criteriaEnumPrimaryPairs) {
    // Build key-value pairs for criteriaId+enum value/display.
    // [key] serialized format (by built-in functions), [value] deserialized object
    // [key] "criteriaId-value-display", [value] criteria+enum value/display
    PCollection<KV<String, IdEnumValue>> serializedAndDeserialized =
        criteriaEnumPrimaryPairs.apply(
            MapElements.into(
                    TypeDescriptors.kvs(
                        TypeDescriptors.strings(), new TypeDescriptor<IdEnumValue>() {}))
                .via(
                    enumValueInstancePrimaryPair ->
                        KV.of(
                            enumValueInstancePrimaryPair.getKey().toString(),
                            enumValueInstancePrimaryPair.getKey())));

    // Build key-value pairs: [key] serialized criteriaId+enum value/display, [value] primaryId.
    PCollection<KV<String, Long>> serializedPrimaryPairs =
        criteriaEnumPrimaryPairs.apply(
            MapElements.into(
                    TypeDescriptors.kvs(TypeDescriptors.strings(), TypeDescriptors.longs()))
                .via(
                    enumValueInstancePrimaryPair ->
                        KV.of(
                            enumValueInstancePrimaryPair.getKey().toString(),
                            enumValueInstancePrimaryPair.getValue())));

    // Distinct (remove duplicate persons).
    // [key] serialized criteriaId+enum value/display, [value] primaryId
    PCollection<KV<String, Long>> distinctSerializedPrimaryPairs =
        serializedPrimaryPairs.apply(Distinct.create());

    // Count by key: [key] serialized criteriaId+enum value/display, [value] num primaryId.
    PCollection<KV<String, Long>> serializedCountPairs =
        distinctSerializedPrimaryPairs.apply(Count.perKey());

    // Group by [key] serialized criteriaId+enum value/display
    // [values] deserialized object, num primaryId.
    final TupleTag<IdEnumValue> deserializedTag = new TupleTag<>();
    final TupleTag<Long> numPrimaryIdTag = new TupleTag<>();
    PCollection<KV<String, CoGbkResult>> serializedAndDeserializedNumPrimaryId =
        KeyedPCollectionTuple.of(deserializedTag, serializedAndDeserialized)
            .and(numPrimaryIdTag, serializedCountPairs)
            .apply(CoGroupByKey.create());
    return serializedAndDeserializedNumPrimaryId
        .apply(
            Filter.by(
                cogb ->
                    cogb.getValue() != null
                        && cogb.getValue().getAll(deserializedTag).iterator().hasNext()))
        .apply(
            MapElements.into(new TypeDescriptor<IdEnumValue>() {})
                .via(
                    cogb -> {
                      IdEnumValue deserialized =
                          cogb.getValue().getAll(deserializedTag).iterator().next();
                      Long count = cogb.getValue().getOnly(numPrimaryIdTag);
                      return new IdEnumValue(
                              deserialized.getId(),
                              deserialized.getEnumValue(),
                              deserialized.getEnumDisplay())
                          .count(count);
                    }));
  }

  public static class IdNumericRange implements Serializable {
    private final Long id;
    private final double min;
    private final double max;

    public IdNumericRange(Long id, double min, double max) {
      this.id = id;
      this.min = min;
      this.max = max;
    }

    public Long getId() {
      return id;
    }

    public double getMax() {
      return max;
    }

    public double getMin() {
      return min;
    }
  }

  public static class IdEnumValue implements Serializable {
    private final Long id;
    // TODO: Parameterize this class based on the type of the enum value. I couldn't get Beam to
    // recognize the coder for this class when it's a generic type.
    private final String enumValue;
    private final String enumDisplay;
    private long count;

    public IdEnumValue(Long id, String enumValue, String enumDisplay) {
      this.id = id;
      this.enumValue = enumValue;
      this.enumDisplay = enumDisplay;
    }

    public Long getId() {
      return id;
    }

    public String getEnumValue() {
      return enumValue;
    }

    public String getEnumDisplay() {
      return enumDisplay;
    }

    public long getCount() {
      return count;
    }

    public IdEnumValue count(long count) {
      this.count = count;
      return this;
    }

    @Override
    public String toString() {
      return id + " - " + (enumValue == null ? "null" : enumValue) + " - " + enumDisplay;
    }
  }
}

package bio.terra.tanagra.indexing.job.dataflow;

import bio.terra.tanagra.indexing.job.BigQueryJob;
import bio.terra.tanagra.indexing.job.dataflow.beam.DataflowUtils;
import bio.terra.tanagra.indexing.job.dataflow.beam.DisplayHintUtils;
import bio.terra.tanagra.query.FieldVariable;
import bio.terra.tanagra.query.Literal;
import bio.terra.tanagra.query.Query;
import bio.terra.tanagra.query.TableVariable;
import bio.terra.tanagra.query.bigquery.BigQuerySchemaUtils;
import bio.terra.tanagra.underlay2.entitymodel.Attribute;
import bio.terra.tanagra.underlay2.entitymodel.Entity;
import bio.terra.tanagra.underlay2.entitymodel.Relationship;
import bio.terra.tanagra.underlay2.entitymodel.entitygroup.CriteriaOccurrence;
import bio.terra.tanagra.underlay2.indextable.ITEntityMain;
import bio.terra.tanagra.underlay2.indextable.ITInstanceLevelDisplayHints;
import bio.terra.tanagra.underlay2.indextable.ITRelationshipIdPairs;
import bio.terra.tanagra.underlay2.serialization.SZIndexer;
import com.google.api.services.bigquery.model.TableRow;
import com.google.common.collect.Lists;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
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
    Query allOccInstancesQuery = occurrenceEntityIndexTable.getQueryAll(Map.of());
    LOGGER.info("allOccInstancesQuery: {}", allOccInstancesQuery.renderSQL());
    PCollection<KV<Long, TableRow>> occIdRowKVs =
        readInOccRows(
            pipeline,
            allOccInstancesQuery.renderSQL(),
            occurrenceEntity.getIdAttribute().getName());
    //    PCollection<KV<Long, TableRow>> occAllAttrs = readInOccRows(pipeline);

    // Build a query to select all occurrence-criteria id pairs, and the pipeline steps to read the
    // results and build a (occurrence id, criteria id) KV PCollection.
    final String entityAIdColumnName = "entityAId";
    final String entityBIdColumnName = "entityBId";
    Query occCriIdPairsQuery =
        getQueryRelationshipIdPairs(
            entityAIdColumnName,
            entityBIdColumnName,
            criteriaOccurrence.getOccurrenceCriteriaRelationship(occurrenceEntity.getName()),
            occurrenceEntityIndexTable,
            criteriaEntityIndexTable,
            occurrenceCriteriaRelationshipIdPairsTable);
    LOGGER.info("index occurrence-criteria id pairs query: {}", occCriIdPairsQuery.renderSQL());
    PCollection<KV<Long, Long>> occCriIdPairKVs =
        readInRelationshipIdPairs(
            pipeline, occCriIdPairsQuery.renderSQL(), entityAIdColumnName, entityBIdColumnName);
    //         // Read in the criteria-occurrence id pairs.
    //         PCollection<KV<Long, Long>> occCriIdPairs =
    //                 readInIdPairs(
    //                         criteriaOccurrence
    //                                 .getOccurrenceCriteriaRelationship(occurrenceEntity)
    //                                 .getMapping(Underlay.MappingType.SOURCE),
    //                         pipeline);

    // Build a query to select all occurrence-criteria id pairs, and the pipeline steps to read the
    // results and build a (occurrence id, criteria id) KV PCollection.
    Query occPriIdPairsQuery =
        getQueryRelationshipIdPairs(
            entityAIdColumnName,
            entityBIdColumnName,
            criteriaOccurrence.getOccurrencePrimaryRelationship(occurrenceEntity.getName()),
            occurrenceEntityIndexTable,
            primaryEntityIndexTable,
            occurrencePrimaryRelationshipIdPairsTable);
    LOGGER.info("index occurrence-primary id pairs query: {}", occPriIdPairsQuery.renderSQL());
    PCollection<KV<Long, Long>> occPriIdPairKVs =
        readInRelationshipIdPairs(
            pipeline, occPriIdPairsQuery.renderSQL(), entityAIdColumnName, entityBIdColumnName);
    //         // Read in the primary-occurrence id pairs.
    //    PCollection<KV<Long, Long>> occPriIdPairs =
    //        readInIdPairs(
    //            criteriaOccurrence
    //                .getOccurrencePrimaryRelationship(occurrenceEntity)
    //                .getMapping(Underlay.MappingType.SOURCE),
    //            pipeline);

    criteriaOccurrence.getAttributesWithInstanceLevelDisplayHints(occurrenceEntity).stream()
        .forEach(
            attribute -> {
              if (attribute.isValueDisplay()) {
                LOGGER.info("enum val hint: {}", attribute.getName());
                enumValHint(occCriIdPairKVs, occPriIdPairKVs, occIdRowKVs, attribute);
              } else if (Literal.DataType.INT64.equals(attribute.getDataType())
                  || Literal.DataType.DOUBLE.equals(attribute.getDataType())) {
                LOGGER.info("numeric range hint: {}", attribute.getName());
                numericRangeHint(occCriIdPairKVs, occIdRowKVs, attribute);
              } // TODO: Calculate display hints for other data types.
            });
    //    for (Attribute attr : criteriaOccurrence.getModifierAttributes()) {
    //      if (Attribute.Type.KEY_AND_DISPLAY.equals(attr.getType())) {
    //        enumValHint(occCriIdPairs, occPriIdPairs, occAllAttrs, attr);
    //      } else {
    //        switch (attr.getDataType()) {
    //          case INT64:
    //          case DOUBLE:
    //            numericRangeHint(occCriIdPairs, occAllAttrs, attr);
    //            break;
    //            case BOOLEAN:
    //            case STRING:
    //            case DATE:
    //            default:
    //                // TODO: Calculate display hints for other data types.
    //        }
    //      }
    //    }

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

  private static Query getQueryRelationshipIdPairs(
      String entityAIdColumnName,
      String entityBIdColumnName,
      Relationship relationship,
      ITEntityMain entityAIndexTable,
      ITEntityMain entityBIndexTable,
      @Nullable ITRelationshipIdPairs relationshipIdPairsTable) {
    Query idPairs;
    if (relationship.isForeignKeyAttributeEntityA()) {
      TableVariable entityMainTable = TableVariable.forPrimary(entityAIndexTable.getTablePointer());
      List<TableVariable> entityMainTableVars = Lists.newArrayList(entityMainTable);
      FieldVariable idAFieldVar =
          entityAIndexTable
              .getAttributeValueField(relationship.getEntityA().getIdAttribute().getName())
              .buildVariable(entityMainTable, entityMainTableVars, entityAIdColumnName);
      FieldVariable idBFieldVar =
          entityAIndexTable
              .getAttributeValueField(relationship.getForeignKeyAttributeEntityA().getName())
              .buildVariable(entityMainTable, entityMainTableVars, entityBIdColumnName);
      idPairs =
          new Query.Builder()
              .select(List.of(idAFieldVar, idBFieldVar))
              .tables(entityMainTableVars)
              .build();
    } else if (relationship.isForeignKeyAttributeEntityB()) {
      TableVariable entityMainTable = TableVariable.forPrimary(entityBIndexTable.getTablePointer());
      List<TableVariable> entityMainTableVars = Lists.newArrayList(entityMainTable);
      FieldVariable idAFieldVar =
          entityBIndexTable
              .getAttributeValueField(relationship.getForeignKeyAttributeEntityB().getName())
              .buildVariable(entityMainTable, entityMainTableVars, entityAIdColumnName);
      FieldVariable idBFieldVar =
          entityBIndexTable
              .getAttributeValueField(relationship.getEntityB().getIdAttribute().getName())
              .buildVariable(entityMainTable, entityMainTableVars, entityBIdColumnName);
      idPairs =
          new Query.Builder()
              .select(List.of(idAFieldVar, idBFieldVar))
              .tables(entityMainTableVars)
              .build();
    } else { // relationship.isIntermediateTable()
      idPairs =
          relationshipIdPairsTable.getQueryAll(
              Map.of(
                  ITRelationshipIdPairs.Column.ENTITY_A_ID.getSchema(),
                  entityAIdColumnName,
                  ITRelationshipIdPairs.Column.ENTITY_B_ID.getSchema(),
                  entityBIdColumnName));
    }
    return idPairs;
  }

  //  public TablePointer getAuxiliaryTable() {
  //    return criteriaOccurrence
  //        .getModifierAuxiliaryData()
  //        .getMapping(Underlay.MappingType.INDEX)
  //        .getTablePointer();
  //  }

  //  private PCollection<KV<Long, TableRow>> readInOccRows(Pipeline pipeline) {
  //    Query allOccInstancesQuery =
  //        occurrenceEntity.getMapping(Underlay.MappingType.INDEX).queryAllAttributes();
  //    LOGGER.info("occAllAttrsQ: {}", occAllAttrsQ.renderSQL());
  //    String occIdName = occurrenceEntity.getIdAttribute().getName();
  //    LOGGER.info("occIdName: {}", occIdName);
  //    return pipeline
  //        .apply(
  //            BigQueryIO.readTableRows()
  //                .fromQuery(occAllAttrsQ.renderSQL())
  //                .withMethod(BigQueryIO.TypedRead.Method.EXPORT)
  //                .usingStandardSql())
  //        .apply(
  //            MapElements.into(
  //                    TypeDescriptors.kvs(TypeDescriptors.longs(),
  // TypeDescriptor.of(TableRow.class)))
  //                .via(
  //                    tableRow -> KV.of(Long.parseLong((String) tableRow.get(occIdName)),
  // tableRow)));
  //  }

  //  private PCollection<KV<Long, Long>> readInIdPairs(
  //      RelationshipMapping relationshipMapping, Pipeline pipeline) {
  //    Query idPairsQ = relationshipMapping.queryIdPairs("idA", "idB");
  //    LOGGER.info("idPairsQ: {}", idPairsQ.renderSQL());
  //    return pipeline
  //        .apply(
  //            BigQueryIO.readTableRows()
  //                .fromQuery(idPairsQ.renderSQL())
  //                .withMethod(BigQueryIO.TypedRead.Method.EXPORT)
  //                .usingStandardSql())
  //        .apply(
  //            MapElements.into(TypeDescriptors.kvs(TypeDescriptors.longs(),
  // TypeDescriptors.longs()))
  //                .via(
  //                    tableRow ->
  //                        KV.of(
  //                            Long.parseLong((String) tableRow.get("idA")),
  //                            Long.parseLong((String) tableRow.get("idB")))));
  //  }

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
        //                            .set("entity_id", idNumericRange.getId())
        //                            .set("attribute_name", numValColName)
        //                            .set("min", idNumericRange.getMin())
        //                            .set("max", idNumericRange.getMax())))
        .apply(
            BigQueryIO.writeTableRows()
                .to(indexTable.getTablePointer().getPathForIndexing())
                .withSchema(
                    BigQuerySchemaUtils.getBigQueryTableSchema(indexTable.getColumnSchemas()))
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
        //                            .set("entity_id", idEnumValue.getId())
        //                            .set("attribute_name", enumValColName)
        //                            .set("enum_value", Long.parseLong(idEnumValue.getEnumValue()))
        //                            .set("enum_display", idEnumValue.getEnumDisplay())
        //                            .set("enum_count", idEnumValue.getCount())))
        .apply(
            BigQueryIO.writeTableRows()
                .to(indexTable.getTablePointer().getPathForIndexing())
                .withSchema(
                    BigQuerySchemaUtils.getBigQueryTableSchema(indexTable.getColumnSchemas()))
                .withCreateDisposition(BigQueryIO.Write.CreateDisposition.CREATE_IF_NEEDED)
                .withWriteDisposition(BigQueryIO.Write.WriteDisposition.WRITE_APPEND)
                .withMethod(BigQueryIO.Write.Method.FILE_LOADS));
  }
}

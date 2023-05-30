package bio.terra.tanagra.service;

import static bio.terra.tanagra.underlay.entitygroup.CriteriaOccurrence.MODIFIER_AUX_DATA_ATTR_COL;
import static bio.terra.tanagra.underlay.entitygroup.CriteriaOccurrence.MODIFIER_AUX_DATA_ENUM_COUNT_COL;
import static bio.terra.tanagra.underlay.entitygroup.CriteriaOccurrence.MODIFIER_AUX_DATA_ENUM_DISPLAY_COL;
import static bio.terra.tanagra.underlay.entitygroup.CriteriaOccurrence.MODIFIER_AUX_DATA_ENUM_VAL_COL;
import static bio.terra.tanagra.underlay.entitygroup.CriteriaOccurrence.MODIFIER_AUX_DATA_ID_COL;
import static bio.terra.tanagra.underlay.entitygroup.CriteriaOccurrence.MODIFIER_AUX_DATA_MAX_COL;
import static bio.terra.tanagra.underlay.entitygroup.CriteriaOccurrence.MODIFIER_AUX_DATA_MIN_COL;

import bio.terra.tanagra.app.configuration.TanagraExportConfiguration;
import bio.terra.tanagra.exception.InvalidQueryException;
import bio.terra.tanagra.exception.SystemException;
import bio.terra.tanagra.query.*;
import bio.terra.tanagra.query.filtervariable.BinaryFilterVariable;
import bio.terra.tanagra.service.instances.*;
import bio.terra.tanagra.underlay.AuxiliaryDataMapping;
import bio.terra.tanagra.underlay.DataPointer;
import bio.terra.tanagra.underlay.DisplayHint;
import bio.terra.tanagra.underlay.Entity;
import bio.terra.tanagra.underlay.EntityGroup;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.ValueDisplay;
import bio.terra.tanagra.underlay.displayhint.EnumVal;
import bio.terra.tanagra.underlay.displayhint.EnumVals;
import bio.terra.tanagra.underlay.displayhint.NumericRange;
import bio.terra.tanagra.underlay.entitygroup.CriteriaOccurrence;
import bio.terra.tanagra.utils.GcsUtils;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalLong;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class QuerysService {
  private static final Logger LOGGER = LoggerFactory.getLogger(QuerysService.class);

  private final UnderlaysService underlaysService;
  private final TanagraExportConfiguration tanagraExportConfiguration;

  @Autowired
  public QuerysService(
      UnderlaysService underlaysService, TanagraExportConfiguration tanagraExportConfiguration) {
    this.underlaysService = underlaysService;
    this.tanagraExportConfiguration = tanagraExportConfiguration;
  }

  public EntityQueryResult listEntityInstances(EntityQueryRequest entityQueryRequest) {
    QueryRequest queryRequest = entityQueryRequest.buildInstancesQuery();
    DataPointer indexDataPointer =
        entityQueryRequest
            .getEntity()
            .getMapping(entityQueryRequest.getMappingType())
            .getTablePointer()
            .getDataPointer();
    QueryResult queryResult = indexDataPointer.getQueryExecutor().execute(queryRequest);

    List<EntityInstance> entityInstances = new ArrayList<>();
    Iterator<RowResult> rowResultsItr = queryResult.getRowResults().iterator();
    while (rowResultsItr.hasNext()) {
      entityInstances.add(
          EntityInstance.fromRowResult(
              rowResultsItr.next(),
              entityQueryRequest.getSelectAttributes(),
              entityQueryRequest.getSelectHierarchyFields(),
              entityQueryRequest.getSelectRelationshipFields()));
    }
    return new EntityQueryResult(
        queryRequest.getSql(), entityInstances, queryResult.getNextPageMarker());
  }

  /** @return GCS signed URL of GCS file containing dataset CSV */
  public String runInstancesQueryAndExportResultsToGcs(
      DataPointer dataPointer, QueryRequest queryRequest) {
    String gcsBucketProjectId = tanagraExportConfiguration.getGcsBucketProjectId();
    String gcsBucketName = tanagraExportConfiguration.getGcsBucketName();

    if (StringUtils.isEmpty(gcsBucketProjectId) || StringUtils.isEmpty(gcsBucketName)) {
      throw new SystemException(
          "For export, gcsBucketProjectId and gcsBucketName properties must be set");
    }

    String fileName =
        dataPointer.getQueryExecutor().executeAndExportResultsToGcs(queryRequest, gcsBucketName);
    return GcsUtils.createSignedUrl(gcsBucketProjectId, gcsBucketName, fileName);
  }

  public EntityCountResult countEntityInstances(EntityCountRequest entityCountRequest) {
    QueryRequest queryRequest = entityCountRequest.buildCountsQuery();
    DataPointer indexDataPointer =
        entityCountRequest
            .getEntity()
            .getMapping(entityCountRequest.getMappingType())
            .getTablePointer()
            .getDataPointer();
    QueryResult queryResult = indexDataPointer.getQueryExecutor().execute(queryRequest);

    List<EntityInstanceCount> instanceCounts = new ArrayList<>();
    Iterator<RowResult> rowResultsItr = queryResult.getRowResults().iterator();
    while (rowResultsItr.hasNext()) {
      instanceCounts.add(
          EntityInstanceCount.fromRowResult(
              rowResultsItr.next(), entityCountRequest.getAttributes()));
    }
    return new EntityCountResult(
        queryRequest.getSql(), instanceCounts, queryResult.getNextPageMarker());
  }

  public QueryRequest buildDisplayHintsQuery(
      Underlay underlay,
      Entity entity,
      Underlay.MappingType mappingType,
      Entity relatedEntity,
      Literal relatedEntityId) {
    // TODO: Support display hints for any relationship, not just for the CRITERIA_OCCURRENCE entity
    // group.
    EntityGroup entityGroup =
        underlaysService
            .getRelationship(underlay.getEntityGroups().values(), entity, relatedEntity)
            .getEntityGroup();
    if (!(entityGroup instanceof CriteriaOccurrence)) {
      throw new InvalidQueryException(
          "Only CRITERIA_OCCURENCE entity groups support display hints queries");
    }
    CriteriaOccurrence criteriaOccurrence = (CriteriaOccurrence) entityGroup;
    AuxiliaryDataMapping auxDataMapping =
        criteriaOccurrence.getModifierAuxiliaryData().getMapping(mappingType);

    TableVariable primaryTableVar = TableVariable.forPrimary(auxDataMapping.getTablePointer());
    List<TableVariable> tableVars = Lists.newArrayList(primaryTableVar);

    List<FieldVariable> selectFieldVars =
        auxDataMapping.getFieldPointers().entrySet().stream()
            .map(
                entry -> entry.getValue().buildVariable(primaryTableVar, tableVars, entry.getKey()))
            .collect(Collectors.toList());
    // TODO: Centralize this schema definition used both here and in the indexing job. Also handle
    // other enum_value data types.
    List<ColumnSchema> columnSchemas =
        List.of(
            new ColumnSchema(MODIFIER_AUX_DATA_ID_COL, CellValue.SQLDataType.INT64),
            new ColumnSchema(MODIFIER_AUX_DATA_ATTR_COL, CellValue.SQLDataType.STRING),
            new ColumnSchema(MODIFIER_AUX_DATA_MIN_COL, CellValue.SQLDataType.FLOAT),
            new ColumnSchema(MODIFIER_AUX_DATA_MAX_COL, CellValue.SQLDataType.FLOAT),
            new ColumnSchema(MODIFIER_AUX_DATA_ENUM_VAL_COL, CellValue.SQLDataType.INT64),
            new ColumnSchema(MODIFIER_AUX_DATA_ENUM_DISPLAY_COL, CellValue.SQLDataType.STRING),
            new ColumnSchema(MODIFIER_AUX_DATA_ENUM_COUNT_COL, CellValue.SQLDataType.INT64));

    // Build the WHERE filter variables from the entity filter.
    FieldVariable entityIdFieldVar =
        selectFieldVars.stream()
            .filter(fv -> fv.getAlias().equals(MODIFIER_AUX_DATA_ID_COL))
            .findFirst()
            .get();
    BinaryFilterVariable filterVar =
        new BinaryFilterVariable(
            entityIdFieldVar, BinaryFilterVariable.BinaryOperator.EQUALS, relatedEntityId);

    Query query =
        new Query.Builder().select(selectFieldVars).tables(tableVars).where(filterVar).build();
    LOGGER.info("Generated display hint query: {}", query.renderSQL());

    return new QueryRequest(query.renderSQL(), new ColumnHeaderSchema(columnSchemas));
  }

  public Map<String, DisplayHint> runDisplayHintsQuery(
      DataPointer dataPointer, QueryRequest queryRequest) {
    QueryResult queryResult = dataPointer.getQueryExecutor().execute(queryRequest);

    Map<String, DisplayHint> displayHints = new HashMap<>();
    Map<String, List<EnumVal>> runningEnumValsMap = new HashMap<>();
    Iterator<RowResult> rowResultsItr = queryResult.getRowResults().iterator();
    while (rowResultsItr.hasNext()) {
      RowResult rowResult = rowResultsItr.next();
      String attrName =
          rowResult.get(MODIFIER_AUX_DATA_ATTR_COL).getLiteral().orElseThrow().getStringVal();

      OptionalDouble min = rowResult.get(MODIFIER_AUX_DATA_MIN_COL).getDouble();
      if (min.isPresent()) {
        // This is a numeric range hint.
        OptionalDouble max = rowResult.get(MODIFIER_AUX_DATA_MAX_COL).getDouble();
        displayHints.put(attrName, new NumericRange(min.getAsDouble(), max.getAsDouble()));
      } else {
        // This is an enum values hint.
        Literal val = rowResult.get(MODIFIER_AUX_DATA_ENUM_VAL_COL).getLiteral().orElseThrow();
        Optional<String> display = rowResult.get(MODIFIER_AUX_DATA_ENUM_DISPLAY_COL).getString();
        OptionalLong count = rowResult.get(MODIFIER_AUX_DATA_ENUM_COUNT_COL).getLong();
        List<EnumVal> runningEnumVals =
            runningEnumValsMap.containsKey(attrName)
                ? runningEnumValsMap.get(attrName)
                : new ArrayList<>();
        runningEnumVals.add(new EnumVal(new ValueDisplay(val, display.get()), count.getAsLong()));
        runningEnumValsMap.put(attrName, runningEnumVals);
      }
    }
    runningEnumValsMap.entrySet().stream()
        .forEach(entry -> displayHints.put(entry.getKey(), new EnumVals(entry.getValue())));

    return displayHints;
  }
}

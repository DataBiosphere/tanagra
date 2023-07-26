package bio.terra.tanagra.service;

import static bio.terra.tanagra.underlay.entitygroup.CriteriaOccurrence.MODIFIER_AUX_DATA_ATTR_COL;
import static bio.terra.tanagra.underlay.entitygroup.CriteriaOccurrence.MODIFIER_AUX_DATA_ENUM_COUNT_COL;
import static bio.terra.tanagra.underlay.entitygroup.CriteriaOccurrence.MODIFIER_AUX_DATA_ENUM_DISPLAY_COL;
import static bio.terra.tanagra.underlay.entitygroup.CriteriaOccurrence.MODIFIER_AUX_DATA_ENUM_VAL_COL;
import static bio.terra.tanagra.underlay.entitygroup.CriteriaOccurrence.MODIFIER_AUX_DATA_MAX_COL;
import static bio.terra.tanagra.underlay.entitygroup.CriteriaOccurrence.MODIFIER_AUX_DATA_MIN_COL;

import bio.terra.tanagra.query.*;
import bio.terra.tanagra.service.instances.*;
import bio.terra.tanagra.underlay.*;
import bio.terra.tanagra.underlay.displayhint.EnumVal;
import bio.terra.tanagra.underlay.displayhint.EnumVals;
import bio.terra.tanagra.underlay.displayhint.NumericRange;
import java.util.*;
import org.springframework.stereotype.Component;

@Component
public class QuerysService {
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

  public EntityHintResult listEntityHints(EntityHintRequest entityHintRequest) {
    QueryRequest queryRequest = entityHintRequest.buildHintsQuery();
    DataPointer dataPointer =
        entityHintRequest
            .getEntity()
            .getMapping(entityHintRequest.getMappingType())
            .getTablePointer()
            .getDataPointer();
    QueryResult queryResult = dataPointer.getQueryExecutor().execute(queryRequest);

    Map<Attribute, DisplayHint> displayHints = new HashMap<>();
    Map<Attribute, List<EnumVal>> runningEnumValsMap = new HashMap<>();
    Iterator<RowResult> rowResultsItr = queryResult.getRowResults().iterator();
    while (rowResultsItr.hasNext()) {
      RowResult rowResult = rowResultsItr.next();

      String attrName =
          rowResult.get(MODIFIER_AUX_DATA_ATTR_COL).getLiteral().orElseThrow().getStringVal();
      Attribute attr = entityHintRequest.getAttribute(attrName);

      OptionalDouble min = rowResult.get(MODIFIER_AUX_DATA_MIN_COL).getDouble();
      if (min.isPresent()) {
        // This is a numeric range hint, which is contained in a single row.
        OptionalDouble max = rowResult.get(MODIFIER_AUX_DATA_MAX_COL).getDouble();
        displayHints.put(attr, new NumericRange(min.getAsDouble(), max.getAsDouble()));
      } else {
        // This is part of an enum values hint, which is spread across multiple rows -- one per enum
        // value.
        Literal val = rowResult.get(MODIFIER_AUX_DATA_ENUM_VAL_COL).getLiteral().orElseThrow();
        Optional<String> display = rowResult.get(MODIFIER_AUX_DATA_ENUM_DISPLAY_COL).getString();
        OptionalLong count = rowResult.get(MODIFIER_AUX_DATA_ENUM_COUNT_COL).getLong();
        List<EnumVal> runningEnumVals =
            runningEnumValsMap.containsKey(attr) ? runningEnumValsMap.get(attr) : new ArrayList<>();
        runningEnumVals.add(new EnumVal(new ValueDisplay(val, display.get()), count.getAsLong()));
        runningEnumValsMap.put(attr, runningEnumVals);
      }
    }
    runningEnumValsMap.entrySet().stream()
        .forEach(
            entry -> {
              // Sort the enum values, so they have a consistent ordering.
              List<EnumVal> enumVals = entry.getValue();
              enumVals.sort(
                  Comparator.comparing(ev -> String.valueOf(ev.getValueDisplay().getDisplay())));

              displayHints.put(entry.getKey(), new EnumVals(enumVals));
            });

    return new EntityHintResult(queryRequest.getSql(), displayHints);
  }
}

package bio.terra.tanagra.service;

import bio.terra.tanagra.api.schemas.EntityLevelDisplayHints;
import bio.terra.tanagra.api.schemas.InstanceLevelDisplayHints;
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
          rowResult
              .get(
                  entityHintRequest.isEntityLevelHints()
                      ? EntityLevelDisplayHints.Columns.ATTRIBUTE_NAME.getSchema().getColumnName()
                      : InstanceLevelDisplayHints.Columns.ATTRIBUTE_NAME
                          .getSchema()
                          .getColumnName())
              .getLiteral()
              .orElseThrow()
              .getStringVal();
      Attribute attr = entityHintRequest.getEntity().getAttribute(attrName);

      OptionalDouble min =
          rowResult
              .get(
                  entityHintRequest.isEntityLevelHints()
                      ? EntityLevelDisplayHints.Columns.MIN.getSchema().getColumnName()
                      : InstanceLevelDisplayHints.Columns.MIN.getSchema().getColumnName())
              .getDouble();
      if (min.isPresent()) {
        // This is a numeric range hint, which is contained in a single row.
        OptionalDouble max =
            rowResult
                .get(
                    entityHintRequest.isEntityLevelHints()
                        ? EntityLevelDisplayHints.Columns.MAX.getSchema().getColumnName()
                        : InstanceLevelDisplayHints.Columns.MAX.getSchema().getColumnName())
                .getDouble();
        displayHints.put(attr, new NumericRange(min.getAsDouble(), max.getAsDouble()));
      } else {
        // This is part of an enum values hint, which is spread across multiple rows -- one per enum
        // value.
        // TODO: Make a static NULL Literal instance, instead of overloading the String value.
        Literal val =
            rowResult
                .get(
                    entityHintRequest.isEntityLevelHints()
                        ? EntityLevelDisplayHints.Columns.ENUM_VALUE.getSchema().getColumnName()
                        : InstanceLevelDisplayHints.Columns.ENUM_VALUE.getSchema().getColumnName())
                .getLiteral()
                .orElse(new Literal(null));
        String display =
            rowResult
                .get(
                    entityHintRequest.isEntityLevelHints()
                        ? EntityLevelDisplayHints.Columns.ENUM_DISPLAY.getSchema().getColumnName()
                        : InstanceLevelDisplayHints.Columns.ENUM_DISPLAY
                            .getSchema()
                            .getColumnName())
                .getString()
                .orElse(null);
        OptionalLong count =
            rowResult
                .get(
                    entityHintRequest.isEntityLevelHints()
                        ? EntityLevelDisplayHints.Columns.ENUM_COUNT.getSchema().getColumnName()
                        : InstanceLevelDisplayHints.Columns.ENUM_COUNT.getSchema().getColumnName())
                .getLong();
        List<EnumVal> runningEnumVals =
            runningEnumValsMap.containsKey(attr) ? runningEnumValsMap.get(attr) : new ArrayList<>();
        runningEnumVals.add(new EnumVal(new ValueDisplay(val, display), count.getAsLong()));
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

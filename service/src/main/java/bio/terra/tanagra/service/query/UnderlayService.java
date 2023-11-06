package bio.terra.tanagra.service.query;

import bio.terra.common.exception.NotFoundException;
import bio.terra.tanagra.api.query.EntityCountRequest;
import bio.terra.tanagra.api.query.EntityCountResult;
import bio.terra.tanagra.api.query.EntityHintRequest;
import bio.terra.tanagra.api.query.EntityHintResult;
import bio.terra.tanagra.api.query.EntityInstance;
import bio.terra.tanagra.api.query.EntityInstanceCount;
import bio.terra.tanagra.api.query.EntityQueryRequest;
import bio.terra.tanagra.api.query.EntityQueryResult;
import bio.terra.tanagra.app.configuration.UnderlayConfiguration;
import bio.terra.tanagra.exception.SystemException;
import bio.terra.tanagra.query.DataPointer;
import bio.terra.tanagra.query.Literal;
import bio.terra.tanagra.query.QueryRequest;
import bio.terra.tanagra.query.QueryResult;
import bio.terra.tanagra.query.RowResult;
import bio.terra.tanagra.service.accesscontrol.ResourceCollection;
import bio.terra.tanagra.service.accesscontrol.ResourceId;
import bio.terra.tanagra.underlay.Attribute;
import bio.terra.tanagra.underlay.DisplayHint;
import bio.terra.tanagra.underlay.Entity;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.ValueDisplay;
import bio.terra.tanagra.underlay.displayhint.EnumVal;
import bio.terra.tanagra.underlay.displayhint.EnumVals;
import bio.terra.tanagra.underlay.displayhint.NumericRange;
import bio.terra.tanagra.underlay2.indextable.ITEntityLevelDisplayHints;
import bio.terra.tanagra.underlay2.indextable.ITInstanceLevelDisplayHints;
import bio.terra.tanagra.utils.FileIO;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.OptionalLong;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/** Service to handle underlay operations. */
@Component
public class UnderlayService {
  private final Map<String, Underlay> underlaysMap;
  private final Map<Entity, EntityHintResult> entityLevelHintsCache;

  @Autowired
  public UnderlayService(UnderlayConfiguration underlayConfiguration) {
    // Read in underlays from resource files.
    Map<String, Underlay> underlaysMapBuilder = new HashMap<>();
    FileIO.setToReadResourceFiles();
    for (String underlayFile : underlayConfiguration.getFiles()) {
      Path resourceConfigPath = Path.of("config").resolve(underlayFile);
      FileIO.setInputParentDir(resourceConfigPath.getParent());
      try {
        Underlay underlay = Underlay.fromJSON(resourceConfigPath.getFileName().toString());
        underlaysMapBuilder.put(underlay.getName(), underlay);
      } catch (IOException ioEx) {
        throw new SystemException(
            "Error reading underlay file from resources: " + resourceConfigPath, ioEx);
      }
    }
    this.underlaysMap = underlaysMapBuilder;

    // Start with an empty entity-level hints cache.
    this.entityLevelHintsCache = new HashMap<>();
  }

  public List<Underlay> listUnderlays(ResourceCollection authorizedIds) {
    if (authorizedIds.isAllResources()) {
      return underlaysMap.values().stream().collect(Collectors.toUnmodifiableList());
    } else {
      // If the incoming list is empty, the caller does not have permission to see any
      // underlays, so we return an empty list.
      if (authorizedIds.isEmpty()) {
        return Collections.emptyList();
      }
      List<String> authorizedNames =
          authorizedIds.getResources().stream()
              .map(ResourceId::getUnderlay)
              .collect(Collectors.toList());
      return underlaysMap.values().stream()
          .filter(underlay -> authorizedNames.contains(underlay.getName()))
          .collect(Collectors.toUnmodifiableList());
    }
  }

  public Underlay getUnderlay(String name) {
    if (!underlaysMap.containsKey(name)) {
      throw new NotFoundException("Underlay not found: " + name);
    }
    return underlaysMap.get(name);
  }

  public List<Entity> listEntities(String underlayName) {
    return getUnderlay(underlayName).getEntities().values().stream().collect(Collectors.toList());
  }

  public Entity getEntity(String underlayName, String entityName) {
    Underlay underlay = getUnderlay(underlayName);
    if (!underlay.getEntities().containsKey(entityName)) {
      throw new NotFoundException("Entity not found: " + underlayName + ", " + entityName);
    }
    return underlay.getEntity(entityName);
  }

  public static EntityQueryResult listEntityInstances(EntityQueryRequest entityQueryRequest) {
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
    // Get the entity-level hints, in case we can avoid grouping by the display fields also.
    EntityHintRequest entityHintRequest =
        new EntityHintRequest.Builder().entity(entityCountRequest.getEntity()).build();
    EntityHintResult entityHintResult = listEntityHints(entityHintRequest);

    // Run the counts query.
    return runCountEntityInstances(entityCountRequest, entityHintResult);
  }

  private static EntityCountResult runCountEntityInstances(
      EntityCountRequest entityCountRequest, EntityHintResult entityHintResult) {
    QueryRequest queryRequest = entityCountRequest.buildCountsQuery(entityHintResult);
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
              rowResultsItr.next(), entityCountRequest.getAttributes(), entityHintResult));
    }
    return new EntityCountResult(
        queryRequest.getSql(), instanceCounts, queryResult.getNextPageMarker());
  }

  public EntityHintResult listEntityHints(EntityHintRequest entityHintRequest) {
    // Check the cache for entity-level hints.
    if (entityHintRequest.isEntityLevelHints()
        && entityLevelHintsCache.containsKey(entityHintRequest.getEntity())) {
      return entityLevelHintsCache.get(entityHintRequest.getEntity());
    }

    // Run the hints query.
    EntityHintResult entityHintResult = runListEntityHints(entityHintRequest);

    // Update the cache with entity-level hints.
    if (entityHintRequest.isEntityLevelHints()) {
      entityLevelHintsCache.put(entityHintRequest.getEntity(), entityHintResult);
    }
    return entityHintResult;
  }

  private static EntityHintResult runListEntityHints(EntityHintRequest entityHintRequest) {
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
                      ? ITEntityLevelDisplayHints.Column.ATTRIBUTE_NAME.getSchema().getColumnName()
                      : ITInstanceLevelDisplayHints.Column.ATTRIBUTE_NAME
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
                      ? ITEntityLevelDisplayHints.Column.MIN.getSchema().getColumnName()
                      : ITInstanceLevelDisplayHints.Column.MIN.getSchema().getColumnName())
              .getDouble();
      if (min.isPresent()) {
        // This is a numeric range hint, which is contained in a single row.
        OptionalDouble max =
            rowResult
                .get(
                    entityHintRequest.isEntityLevelHints()
                        ? ITEntityLevelDisplayHints.Column.MAX.getSchema().getColumnName()
                        : ITInstanceLevelDisplayHints.Column.MAX.getSchema().getColumnName())
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
                        ? ITEntityLevelDisplayHints.Column.ENUM_VALUE.getSchema().getColumnName()
                        : ITInstanceLevelDisplayHints.Column.ENUM_VALUE.getSchema().getColumnName())
                .getLiteral()
                .orElse(new Literal(null));
        String display =
            rowResult
                .get(
                    entityHintRequest.isEntityLevelHints()
                        ? ITEntityLevelDisplayHints.Column.ENUM_DISPLAY.getSchema().getColumnName()
                        : ITInstanceLevelDisplayHints.Column.ENUM_DISPLAY
                            .getSchema()
                            .getColumnName())
                .getString()
                .orElse(null);
        OptionalLong count =
            rowResult
                .get(
                    entityHintRequest.isEntityLevelHints()
                        ? ITEntityLevelDisplayHints.Column.ENUM_COUNT.getSchema().getColumnName()
                        : ITInstanceLevelDisplayHints.Column.ENUM_COUNT.getSchema().getColumnName())
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

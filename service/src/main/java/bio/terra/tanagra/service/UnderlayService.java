package bio.terra.tanagra.service;

import static bio.terra.tanagra.indexing.job.bigquery.WriteEntityLevelDisplayHints.*;

import bio.terra.common.exception.NotFoundException;
import bio.terra.tanagra.api.field.AttributeField;
import bio.terra.tanagra.api.field.ValueDisplayField;
import bio.terra.tanagra.api.filter.EntityFilter;
import bio.terra.tanagra.api.query.PageMarker;
import bio.terra.tanagra.api.query.count.CountQueryRequest;
import bio.terra.tanagra.api.query.count.CountQueryResult;
import bio.terra.tanagra.api.query.hint.HintInstance;
import bio.terra.tanagra.api.query.hint.HintQueryRequest;
import bio.terra.tanagra.api.query.hint.HintQueryResult;
import bio.terra.tanagra.api.shared.DataType;
import bio.terra.tanagra.api.shared.Literal;
import bio.terra.tanagra.api.shared.OrderByDirection;
import bio.terra.tanagra.api.shared.ValueDisplay;
import bio.terra.tanagra.app.configuration.*;
import bio.terra.tanagra.indexing.job.bigquery.WriteEntityLevelDisplayHints;
import bio.terra.tanagra.query.bigquery.translator.BQApiTranslator;
import bio.terra.tanagra.query.sql.SqlParams;
import bio.terra.tanagra.query.sql.SqlQueryRequest;
import bio.terra.tanagra.query.sql.SqlQueryResult;
import bio.terra.tanagra.service.accesscontrol.ResourceCollection;
import bio.terra.tanagra.service.accesscontrol.ResourceId;
import bio.terra.tanagra.underlay.ConfigReader;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.entitymodel.*;
import bio.terra.tanagra.underlay.serialization.*;
import com.google.common.collect.ImmutableMap;
import jakarta.annotation.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

/** Service to handle underlay operations. */
@Component
public class UnderlayService {
  private static final Logger LOGGER = LoggerFactory.getLogger(UnderlayService.class);

  private final ImmutableMap<String, CachedUnderlay> underlayCache;

  @Autowired
  public UnderlayService(
      UnderlayConfiguration underlayConfiguration, ExportConfiguration exportConfiguration) {
    // Read in underlays from resource files.
    Map<String, CachedUnderlay> underlayCacheBuilder = new HashMap<>();
    for (String serviceConfig : underlayConfiguration.getFiles()) {
      ConfigReader configReader = ConfigReader.fromJarResources();
      SZService szService = configReader.readService(serviceConfig);
      SZUnderlay szUnderlay = configReader.readUnderlay(szService.underlay);

      // Default the export infrastructure pointers to those set by the application properties.
      if (szService.bigQuery.queryProjectId == null) {
        szService.bigQuery.queryProjectId = exportConfiguration.getShared().getGcpProjectId();
      }
      if (CollectionUtils.isEmpty(szService.bigQuery.exportDatasetIds)) {
        szService.bigQuery.exportDatasetIds = exportConfiguration.getShared().getBqDatasetIds();
      }
      if (CollectionUtils.isEmpty(szService.bigQuery.exportBucketNames)) {
        szService.bigQuery.exportBucketNames = exportConfiguration.getShared().getGcsBucketNames();
      }

      Underlay underlay = Underlay.fromConfig(szService.bigQuery, szUnderlay, configReader);
      underlayCacheBuilder.put(underlay.getName(), new CachedUnderlay(underlay));
    }
    this.underlayCache = ImmutableMap.copyOf(underlayCacheBuilder);
  }

  public List<Underlay> listUnderlays(ResourceCollection authorizedIds) {
    if (authorizedIds.isAllResources()) {
      return underlayCache.values().stream()
          .map(CachedUnderlay::getUnderlay)
          .collect(Collectors.toUnmodifiableList());
    } else {
      // If the incoming list is empty, the caller does not have permission to see any
      // underlays, so we return an empty list.
      if (authorizedIds.isEmpty()) {
        return Collections.emptyList();
      }
      List<String> authorizedNames =
          authorizedIds.getResources().stream().map(ResourceId::getUnderlay).toList();
      return underlayCache.values().stream()
          .map(CachedUnderlay::getUnderlay)
          .filter(underlay -> authorizedNames.contains(underlay.getName()))
          .collect(Collectors.toUnmodifiableList());
    }
  }

  public Underlay getUnderlay(String underlayName) {
    if (!underlayCache.containsKey(underlayName)) {
      throw new NotFoundException("Underlay not found: " + underlayName);
    }
    return underlayCache.get(underlayName).getUnderlay();
  }

  private static class CachedUnderlay {
    private final Underlay underlay;
    private final Map<String, HintQueryResult> entityLevelHints;

    CachedUnderlay(Underlay underlay) {
      this.underlay = underlay;
      this.entityLevelHints = new HashMap<>();
    }

    Underlay getUnderlay() {
      return underlay;
    }

    Optional<HintQueryResult> getEntityLevelHints(String entityName) {
      return Optional.ofNullable(entityLevelHints.get(entityName));
    }

    void putEntityLevelHints(String entityName, HintQueryResult hintQueryResult) {
      entityLevelHints.put(entityName, hintQueryResult);
    }
  }

  private Optional<HintQueryResult> getEntityLevelHints(String underlayName, String entityName) {
    return underlayCache.get(underlayName).getEntityLevelHints(entityName);
  }

  private void cacheEntityLevelHints(
      String underlayName, String entityName, HintQueryResult hintQueryResult) {
    underlayCache.get(underlayName).putEntityLevelHints(entityName, hintQueryResult);
  }

  @SuppressWarnings("checkstyle:ParameterNumber")
  public CountQueryResult runCountQuery(
      Underlay underlay,
      Entity entity,
      @Nullable String countDistinctAttributeName,
      @Nullable List<String> groupByAttributeNames,
      EntityFilter entityFilter,
      OrderByDirection orderByDirection,
      Integer limit,
      PageMarker pageMarker,
      Integer pageSize) {
    HintQueryResult entityLevelHints = getEntityLevelHints(underlay, entity);

    // Build the attribute fields for select and group by.
    List<ValueDisplayField> attributeFields =
        (groupByAttributeNames != null)
            ? groupByAttributeNames.stream()
                .map(
                    attributeName ->
                        (ValueDisplayField)
                            new AttributeField(
                                underlay, entity, entity.getAttribute(attributeName), false))
                .toList()
            : List.of();

    CountQueryRequest countQueryRequest =
        new CountQueryRequest(
            underlay,
            entity,
            countDistinctAttributeName == null
                ? null
                : entity.getAttribute(countDistinctAttributeName),
            attributeFields,
            entityFilter,
            orderByDirection,
            limit,
            pageMarker,
            pageSize,
            entityLevelHints,
            false);
    return underlay.getQueryRunner().run(countQueryRequest);
  }

  public HintQueryResult getEntityLevelHints(Underlay underlay, Entity entity) {
    // Get the entity level hints, either via query or from the cache.
    Optional<HintQueryResult> entityLevelHintsCached =
        getEntityLevelHints(underlay.getName(), entity.getName());
    HintQueryResult entityLevelHints;
    if (entityLevelHintsCached.isPresent()) {
      entityLevelHints = entityLevelHintsCached.get();
    } else {
      HintQueryRequest hintQueryRequest =
          new HintQueryRequest(underlay, entity, null, null, null, false);
      entityLevelHints = underlay.getQueryRunner().run(hintQueryRequest);
      cacheEntityLevelHints(underlay.getName(), entity.getName(), entityLevelHints);
    }
    return entityLevelHints;
  }

  public HintQueryResult getEntityLevelHints(
      Underlay underlay, Entity entity, EntityFilter entityFilter) {
    // Get the list of attributes with hints already computed for this entity
    HintQueryResult entityLevelHints = getEntityLevelHints(underlay, entity);
    if (entityFilter == null) {
      return entityLevelHints;
    }

    // Generate SQL to select for entity filter
    BQApiTranslator bqTranslator = new BQApiTranslator();
    SqlParams sqlParams = new SqlParams();
    String bqFilterSql = bqTranslator.translator(entityFilter).buildSql(sqlParams, null);

    // For each attribute with a hint, calculate new hints with the filter
    StringBuilder allSql = new StringBuilder();
    List<HintInstance> allHintInstances = new ArrayList<>();
    entityLevelHints.getHintInstances().stream()
        .map(HintInstance::getAttribute)
        .parallel()
        .map(
            attribute -> {
              if (isRangeHint(attribute)) {
                String sql =
                    WriteEntityLevelDisplayHints.buildRangeHintSql(
                        underlay.getIndexSchema().getEntityMain(entity.getName()),
                        attribute,
                        bqFilterSql);
                SqlQueryResult sqlQueryResult =
                    underlay
                        .getQueryRunner()
                        .run(new SqlQueryRequest(sql, sqlParams, null, null, false));

                List<HintInstance> attrHintInstances = new ArrayList<>();
                sqlQueryResult
                    .getRowResults()
                    .iterator()
                    .forEachRemaining(
                        sqlRowResult -> {
                          Double min =
                              sqlRowResult.get(MIN_VAL_ALIAS, DataType.DOUBLE).getDoubleVal();
                          Double max =
                              sqlRowResult.get(MAX_VAL_ALIAS, DataType.DOUBLE).getDoubleVal();
                          if (min != null && max != null) {
                            attrHintInstances.add(new HintInstance(attribute, min, max));
                          }
                        });
                return new HintQueryResult(sql, attrHintInstances);

              } else if (isEnumHintForValueDisplay(attribute)) {
                String sql =
                    WriteEntityLevelDisplayHints.buildEnumHintForValueDisplaySql(
                        underlay.getIndexSchema().getEntityMain(entity.getName()),
                        attribute,
                        bqFilterSql);
                SqlQueryResult sqlQueryResult =
                    underlay
                        .getQueryRunner()
                        .run(new SqlQueryRequest(sql, sqlParams, null, null, false));

                Map<ValueDisplay, Long> attrEnumValues = new HashMap<>();
                sqlQueryResult
                    .getRowResults()
                    .iterator()
                    .forEachRemaining(
                        sqlRowResult -> {
                          Literal enumVal = sqlRowResult.get(ENUM_VAL_ALIAS, DataType.INT64);
                          String enumDisplay =
                              sqlRowResult.get(ENUM_DISP_ALIAS, DataType.STRING).getStringVal();
                          Long enumCount =
                              sqlRowResult.get(ENUM_COUNT_ALIAS, DataType.INT64).getInt64Val();
                          attrEnumValues.put(new ValueDisplay(enumVal, enumDisplay), enumCount);
                        });
                return new HintQueryResult(
                    sql, List.of(new HintInstance(attribute, attrEnumValues)));

              } else if (isEnumHintForRepeatedStringValue(attribute)) {
                String sql =
                    WriteEntityLevelDisplayHints.buildEnumHintForRepeatedStringValueSql(
                        underlay.getIndexSchema().getEntityMain(entity.getName()),
                        attribute,
                        bqFilterSql);
                SqlQueryResult sqlQueryResult =
                    underlay
                        .getQueryRunner()
                        .run(new SqlQueryRequest(sql, sqlParams, null, null, false));

                Map<ValueDisplay, Long> attrEnumValues = new HashMap<>();
                sqlQueryResult
                    .getRowResults()
                    .iterator()
                    .forEachRemaining(
                        sqlRowResult -> {
                          Literal enumVal = sqlRowResult.get(ENUM_VAL_ALIAS, DataType.STRING);
                          Long enumCount =
                              sqlRowResult.get(ENUM_COUNT_ALIAS, DataType.INT64).getInt64Val();
                          attrEnumValues.put(new ValueDisplay(enumVal), enumCount);
                        });
                return new HintQueryResult(
                    sql, List.of(new HintInstance(attribute, attrEnumValues)));

              } else {
                LOGGER.info(
                    "Attribute {} data type {} not yet supported for computing dynamic hint",
                    attribute.getName(),
                    attribute.getDataType());
                return null;
              }
            })
        .toList()
        .forEach(
            hqr -> {
              allSql.append(hqr.getSql()).append(';');
              allHintInstances.addAll(hqr.getHintInstances());
            });

    return new HintQueryResult(allSql.toString(), allHintInstances);
  }
}

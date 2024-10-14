package bio.terra.tanagra.service;

import static bio.terra.tanagra.indexing.job.bigquery.WriteEntityLevelDisplayHints.isEnumHintForRepeatedStringValue;
import static bio.terra.tanagra.indexing.job.bigquery.WriteEntityLevelDisplayHints.isEnumHintForValueDisplay;
import static bio.terra.tanagra.indexing.job.bigquery.WriteEntityLevelDisplayHints.isRangeHint;

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
import bio.terra.tanagra.api.shared.OrderByDirection;
import bio.terra.tanagra.app.configuration.*;
import bio.terra.tanagra.indexing.job.bigquery.WriteEntityLevelDisplayHints;
import bio.terra.tanagra.query.bigquery.translator.BQApiTranslator;
import bio.terra.tanagra.query.sql.SqlParams;
import bio.terra.tanagra.query.sql.SqlQueryRequest;
import bio.terra.tanagra.service.accesscontrol.ResourceCollection;
import bio.terra.tanagra.service.accesscontrol.ResourceId;
import bio.terra.tanagra.underlay.ConfigReader;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.entitymodel.*;
import bio.terra.tanagra.underlay.serialization.*;
import com.google.common.collect.ImmutableMap;
import jakarta.annotation.*;
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
      if (szService.bigQuery.exportDatasetIds == null
          || szService.bigQuery.exportDatasetIds.isEmpty()) {
        szService.bigQuery.exportDatasetIds = exportConfiguration.getShared().getBqDatasetIds();
      }
      if (szService.bigQuery.exportBucketNames == null
          || szService.bigQuery.exportBucketNames.isEmpty()) {
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
    if (entityFilter == null) {
      return getEntityLevelHints(underlay, entity);
    }

    // Get the list of attributes with hints already computed for this entity
    HintQueryResult entityLevelHints = getEntityLevelHints(underlay, entity);
    List<Attribute> hintAttributes =
        entityLevelHints.getHintInstances().stream().map(HintInstance::getAttribute).toList();

    // Generate SQL to select for entity filter
    BQApiTranslator bqTranslator = new BQApiTranslator();
    SqlParams sqlParams = new SqlParams();
    String bqFilterSql = bqTranslator.translator(entityFilter).buildSql(sqlParams, null);
    LOGGER.info("Generated sql for filter: {}", bqFilterSql);

    hintAttributes.parallelStream()
        .map(
            attribute -> {
              if (isRangeHint(attribute)) {
                String sql =
                    WriteEntityLevelDisplayHints.buildRangeHintSql(
                        underlay.getIndexSchema().getEntityMain(entity.getName()),
                        attribute,
                        bqFilterSql);

                SqlQueryRequest sqlQueryRequest =
                    new SqlQueryRequest(sql, sqlParams, null, null, false);

                HintQueryRequest hintQueryRequest =
                    new HintQueryRequest(underlay, entity, sqlQueryRequest, false);

                return underlay.getQueryRunner().run(hintQueryRequest);

              } else if (isEnumHintForValueDisplay(attribute)) {
                return null; // TODO-dex
              } else if (isEnumHintForRepeatedStringValue(attribute)) {
                return null; // TODO-dex
              }
              return null; // TODO-dex
            })
        .toList();

    return null; // TODO-dex
  }
}

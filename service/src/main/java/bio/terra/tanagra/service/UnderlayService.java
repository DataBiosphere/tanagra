package bio.terra.tanagra.service;

import static bio.terra.tanagra.indexing.job.bigquery.WriteEntityLevelDisplayHints.ENUM_COUNT_ALIAS;
import static bio.terra.tanagra.indexing.job.bigquery.WriteEntityLevelDisplayHints.ENUM_DISP_ALIAS;
import static bio.terra.tanagra.indexing.job.bigquery.WriteEntityLevelDisplayHints.ENUM_VAL_ALIAS;
import static bio.terra.tanagra.indexing.job.bigquery.WriteEntityLevelDisplayHints.MAX_VAL_ALIAS;
import static bio.terra.tanagra.indexing.job.bigquery.WriteEntityLevelDisplayHints.MIN_VAL_ALIAS;
import static bio.terra.tanagra.indexing.job.bigquery.WriteEntityLevelDisplayHints.isEnumHintForRepeatedStringValue;
import static bio.terra.tanagra.indexing.job.bigquery.WriteEntityLevelDisplayHints.isEnumHintForValueDisplay;
import static bio.terra.tanagra.indexing.job.bigquery.WriteEntityLevelDisplayHints.isRangeHint;

import bio.terra.common.exception.NotFoundException;
import bio.terra.tanagra.api.field.AttributeField;
import bio.terra.tanagra.api.field.ValueDisplayField;
import bio.terra.tanagra.api.filter.EntityFilter;
import bio.terra.tanagra.api.query.PageMarker;
import bio.terra.tanagra.api.query.count.CountInstance;
import bio.terra.tanagra.api.query.count.CountQueryRequest;
import bio.terra.tanagra.api.query.count.CountQueryResult;
import bio.terra.tanagra.api.query.hint.HintInstance;
import bio.terra.tanagra.api.query.hint.HintQueryRequest;
import bio.terra.tanagra.api.query.hint.HintQueryResult;
import bio.terra.tanagra.api.shared.DataType;
import bio.terra.tanagra.api.shared.Literal;
import bio.terra.tanagra.api.shared.OrderByDirection;
import bio.terra.tanagra.api.shared.ValueDisplay;
import bio.terra.tanagra.app.configuration.ExportConfiguration;
import bio.terra.tanagra.app.configuration.UnderlayConfiguration;
import bio.terra.tanagra.exception.InvalidConfigException;
import bio.terra.tanagra.indexing.job.bigquery.WriteEntityLevelDisplayHints;
import bio.terra.tanagra.query.bigquery.translator.BQApiTranslator;
import bio.terra.tanagra.query.sql.SqlParams;
import bio.terra.tanagra.query.sql.SqlQueryRequest;
import bio.terra.tanagra.query.sql.SqlQueryResult;
import bio.terra.tanagra.service.accesscontrol.ResourceCollection;
import bio.terra.tanagra.service.accesscontrol.ResourceId;
import bio.terra.tanagra.underlay.ConfigReader;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.entitymodel.Attribute;
import bio.terra.tanagra.underlay.entitymodel.Entity;
import bio.terra.tanagra.underlay.serialization.SZService;
import bio.terra.tanagra.underlay.serialization.SZUnderlay;
import bio.terra.tanagra.underlay.serialization.SZVisualization;
import bio.terra.tanagra.underlay.serialization.SZVisualization.SZVisualizationDataConfig;
import bio.terra.tanagra.underlay.serialization.SZVisualization.SZVisualizationDataConfig.SZVisualizationDCSource;
import bio.terra.tanagra.underlay.visualization.Visualization;
import bio.terra.tanagra.underlay.visualization.VisualizationData;
import com.google.common.collect.ImmutableMap;
import jakarta.annotation.Nullable;
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

      SZUnderlay szUnderlay = configReader.readUnderlay(szService.underlay);
      Underlay underlay = Underlay.fromConfig(szService.bigQuery, szUnderlay, configReader);
      underlayCacheBuilder.put(underlay.getName(), new CachedUnderlay(underlay));
    }
    this.underlayCache = ImmutableMap.copyOf(underlayCacheBuilder);

    // pre-compute underlay-wide visualizations (underlayCache must be initialized before this)
    underlayCache.entrySet().parallelStream()
        .forEach(
            cachedUnderlayEntry -> {
              Underlay underlay = cachedUnderlayEntry.getValue().getUnderlay();

              underlay.getClientConfig().getVisualizations().parallelStream()
                  .forEach(
                      szViz -> {
                        cachedUnderlayEntry
                            .getValue()
                            .putUnderlayLevelVisualization(buildVisualization(underlay, szViz));
                      });
            });
  }

  private Visualization buildVisualization(Underlay underlay, SZVisualization szViz) {
    // TODO(dexamundsen): move config to openApi spec
    SZVisualizationDataConfig vizDataConfig = szViz.dataConfigObj;

    // Remove these limitations once the backend sufficiently supports the query generation.
    if (vizDataConfig.sources == null || vizDataConfig.sources.size() != 1) {
      throw new InvalidConfigException(
          "Only 1 visualization source is supported in viz: " + szViz.name);
    }

    SZVisualizationDCSource vizDCSource = vizDataConfig.sources.get(0);
    String selectorEntityName =
        switch (vizDCSource.criteriaSelector) {
            // TODO(dexamundsen): copied from vizContainer.tsx:selectorToEntity
          case "demographics" -> "person"; // default
          case "conditions" -> "conditionOccurrence";
          case "procedures" -> "procedureOccurrence";
          case "ingredients" -> "ingredientOccurrence";
          case "measurements" -> "measurementOccurrence";
          default ->
              throw new InvalidConfigException(
                  String.format(
                      "Visualizations of criteriaSelector: %s are not supported in viz: %s",
                      vizDCSource.criteriaSelector, szViz.name));
        };
    Entity selectorEntity = underlay.getEntity(selectorEntityName);

    // TODO(dexamundsen): copied from vizContainer.tsx:fetchVizData
    String countDistinctAttributeName = null;
    if (vizDCSource.joins != null && !vizDCSource.joins.isEmpty()) {
      Entity joinEntity = underlay.getPrimaryEntity();
      if (vizDCSource.joins.stream()
          .anyMatch(j -> !joinEntity.getName().equals(j.entity) || (j.aggregation != null))) {
        throw new InvalidConfigException(
            String.format(
                "Only unique joins to primaryEntity: %s are supported in viz: %s",
                joinEntity.getName(), szViz.name));
      }
      countDistinctAttributeName = joinEntity.getIdAttribute().getSourceQuery().getValueFieldName();
    }

    int attrCount = vizDCSource.attributes == null ? 0 : vizDCSource.attributes.size();
    if (attrCount < 1 || attrCount > 2) {
      throw new InvalidConfigException(
          "Only 1 or 2 attributes are supported in viz: " + szViz.name);
    }
    List<String> groupByAttributeNames =
        vizDCSource.attributes.stream().map(a -> a.attribute).toList();

    List<CountInstance> countInstances = new ArrayList<>();
    PageMarker pageMarker;
    do {
      CountQueryResult countQueryResult =
          runCountQuery(
              underlay,
              selectorEntity,
              countDistinctAttributeName,
              groupByAttributeNames,
              /* entityFilter= */ null,
              OrderByDirection.DESCENDING,
              /* limit= */ 1_000_000,
              /* pageMarker= */ null,
              /* pageSize= */ null);
      pageMarker = countQueryResult.getPageMarker();
      countInstances.addAll(countQueryResult.getCountInstances());
    } while (pageMarker != null);

    /*
    countInstances.stream().map(
      instance -> {

        Map<String, ApiValueDisplay> attributes = new HashMap<>();
        instance
            .getEntityFieldValues()
            .forEach(
                (field, value) -> {
                  if (field instanceof AttributeField) {
                    attributes.put(
                        ((AttributeField) field).getAttribute().getName(),
                        ToApiUtils.toApiObject(value));
                  }
                });

        attributes.forEach(
            (k, v) -> {

              DataValue value = null;
              String display =  v.getDisplay();

              if (v.isIsRepeatedValue()) {
                if (v.getRepeatedValue() != null) {
                  display = v.getRepeatedValue().stream().map(
                      l -> l.

                  )
                }

                display = v.getRepeatedValue()
              } else {

              }

            }


        );


        FilterCountValue fc =
            new FilterCountValue(Math.toIntExact(instance.getCount()),
                null);



      }


    );  */

    List<VisualizationData> visualizationDataList = new ArrayList<>();
    return new Visualization(szViz.name, szViz.title, visualizationDataList);
  }

  /*
    public record DataValue(String key, Object val) {}
    public record FilterCountValue(int count, Map<String, DataValue> value) {}

    /*
    export type FilterCountValue = {
    count: number;
    [x: string]: DataValue;
  };

       const value: FilterCountValue = {
          count: count.count ?? 0,
        };

        processAttributes(value, count.attributes);

     */

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

  public Visualization getVisualization(String underlayName, String visualizationName) {
    if (!underlayCache.containsKey(underlayName)) {
      throw new NotFoundException("Underlay not found: " + underlayName);
    }
    return underlayCache.get(underlayName).getUnderlayLevelVisualization(visualizationName);
  }

  private static class CachedUnderlay {
    private final Underlay underlay;
    private final Map<String, HintQueryResult> entityLevelHints;
    private final Map<String, Visualization> underlayLevelVisualizations;

    CachedUnderlay(Underlay underlay) {
      this.underlay = underlay;
      this.entityLevelHints = new HashMap<>();
      this.underlayLevelVisualizations = new HashMap<>();
    }

    Underlay getUnderlay() {
      return underlay;
    }

    Optional<HintQueryResult> getEntityLevelHints(String entityName) {
      return Optional.ofNullable(entityLevelHints.get(entityName));
    }

    public Visualization getUnderlayLevelVisualization(String vizName) {
      return Optional.ofNullable(underlayLevelVisualizations.get(vizName)).orElseThrow();
    }

    void putEntityLevelHints(String entityName, HintQueryResult hintQueryResult) {
      entityLevelHints.put(entityName, hintQueryResult);
    }

    public void putUnderlayLevelVisualization(Visualization visualization) {
      underlayLevelVisualizations.put(visualization.getName(), visualization);
    }
  }

  private Optional<HintQueryResult> getEntityLevelHints(String underlayName, String entityName) {
    return underlayCache.get(underlayName).getEntityLevelHints(entityName);
  }

  private void cacheEntityLevelHints(
      String underlayName, String entityName, HintQueryResult hintQueryResult) {
    underlayCache.get(underlayName).putEntityLevelHints(entityName, hintQueryResult);
  }

  private void cacheUnderlayLevelVisualizations(String underlayName, Visualization visualization) {
    underlayCache.get(underlayName).putUnderlayLevelVisualization(visualization);
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
    entityLevelHints.getHintInstances().parallelStream()
        .map(
            hintInstance -> {
              Attribute attribute = hintInstance.getAttribute();
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

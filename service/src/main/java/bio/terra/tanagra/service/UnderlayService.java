package bio.terra.tanagra.service;

import bio.terra.common.exception.NotFoundException;
import bio.terra.tanagra.api.field.AttributeField;
import bio.terra.tanagra.api.field.ValueDisplayField;
import bio.terra.tanagra.api.filter.EntityFilter;
import bio.terra.tanagra.api.query.PageMarker;
import bio.terra.tanagra.api.query.count.CountQueryRequest;
import bio.terra.tanagra.api.query.count.CountQueryResult;
import bio.terra.tanagra.api.query.hint.HintQueryRequest;
import bio.terra.tanagra.api.query.hint.HintQueryResult;
import bio.terra.tanagra.api.shared.OrderByDirection;
import bio.terra.tanagra.app.configuration.UnderlayConfiguration;
import bio.terra.tanagra.service.accesscontrol.ResourceCollection;
import bio.terra.tanagra.service.accesscontrol.ResourceId;
import bio.terra.tanagra.underlay.ConfigReader;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.entitymodel.Entity;
import bio.terra.tanagra.underlay.serialization.SZService;
import bio.terra.tanagra.underlay.serialization.SZUnderlay;
import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/** Service to handle underlay operations. */
@Component
public class UnderlayService {
  private final ImmutableMap<String, CachedUnderlay> underlayCache;

  @Autowired
  public UnderlayService(UnderlayConfiguration underlayConfiguration) {
    // Read in underlays from resource files.
    Map<String, CachedUnderlay> underlayCacheBuilder = new HashMap<>();
    for (String serviceConfig : underlayConfiguration.getFiles()) {
      ConfigReader configReader = ConfigReader.fromJarResources();
      SZService szService = configReader.readService(serviceConfig);
      SZUnderlay szUnderlay = configReader.readUnderlay(szService.underlay);
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
          authorizedIds.getResources().stream()
              .map(ResourceId::getUnderlay)
              .collect(Collectors.toList());
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

  public Optional<HintQueryResult> getEntityLevelHints(String underlayName, String entityName) {
    return underlayCache.get(underlayName).getEntityLevelHints(entityName);
  }

  public void cacheEntityLevelHints(
      String underlayName, String entityName, HintQueryResult hintQueryResult) {
    underlayCache.get(underlayName).putEntityLevelHints(entityName, hintQueryResult);
  }

  @SuppressWarnings("checkstyle:ParameterNumber")
  public CountQueryResult runCountQuery(
      Underlay underlay,
      Entity entity,
      List<String> groupByAttributeNames,
      EntityFilter entityFilter,
      OrderByDirection orderByDirection,
      Integer limit,
      PageMarker pageMarker,
      Integer pageSize) {
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

    // Build the attribute fields for select and group by.
    List<ValueDisplayField> attributeFields = new ArrayList<>();
    groupByAttributeNames.forEach(
        attributeName ->
            attributeFields.add(
                new AttributeField(underlay, entity, entity.getAttribute(attributeName), false)));

    CountQueryRequest countQueryRequest =
        new CountQueryRequest(
            underlay,
            entity,
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
}

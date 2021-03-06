package bio.terra.tanagra.service.query.api;

import bio.terra.common.exception.NotFoundException;
import bio.terra.tanagra.generated.model.ApiEntityCounts;
import bio.terra.tanagra.generated.model.ApiEntityDataset;
import bio.terra.tanagra.generated.model.ApiEntityFilter;
import bio.terra.tanagra.generated.model.ApiOrderByDirection;
import bio.terra.tanagra.service.query.EntityCounts;
import bio.terra.tanagra.service.query.EntityDataset;
import bio.terra.tanagra.service.query.EntityFilter;
import bio.terra.tanagra.service.search.Attribute;
import bio.terra.tanagra.service.search.Entity;
import bio.terra.tanagra.service.search.EntityVariable;
import bio.terra.tanagra.service.search.Filter;
import bio.terra.tanagra.service.search.OrderByDirection;
import bio.terra.tanagra.service.underlay.Underlay;
import bio.terra.tanagra.service.underlay.UnderlayService;
import com.google.common.collect.ImmutableList;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/** A service for converting API models to Tanagra queries. */
@Service
public class ApiConversionService {

  private final UnderlayService underlayService;

  @Autowired
  public ApiConversionService(UnderlayService underlayService) {
    this.underlayService = underlayService;
  }

  public EntityFilter convertEntityFilter(
      String underlayName, String entityName, ApiEntityFilter apiEntityFilter) {
    Underlay underlay = getUnderlay(underlayName);
    Entity primaryEntity = getEntity(entityName, underlay);
    EntityVariable primaryVariable =
        EntityVariable.create(
            primaryEntity,
            ConversionUtils.createAndValidateVariable(apiEntityFilter.getEntityVariable()));

    VariableScope scope = new VariableScope().add(primaryVariable);
    Filter filter = new FilterConverter(underlay).convert(apiEntityFilter.getFilter(), scope);

    return EntityFilter.builder().primaryEntity(primaryVariable).filter(filter).build();
  }

  public EntityDataset convertEntityDataset(
      String underlayName, String entityName, ApiEntityDataset apiEntityDataset) {
    validateLimitSize(apiEntityDataset);
    Underlay underlay = getUnderlay(underlayName);
    Entity primaryEntity = getEntity(entityName, underlay);
    EntityVariable primaryVariable =
        EntityVariable.create(
            primaryEntity,
            ConversionUtils.createAndValidateVariable(apiEntityDataset.getEntityVariable()));

    VariableScope scope = new VariableScope().add(primaryVariable);
    Filter filter = new FilterConverter(underlay).convert(apiEntityDataset.getFilter(), scope);

    ImmutableList<Attribute> selectedAttributes =
        apiEntityDataset.getSelectedAttributes().stream()
            .map(attributeName -> getAttribute(attributeName, primaryEntity, underlay))
            .collect(ImmutableList.toImmutableList());

    Attribute orderByAttribute =
        apiEntityDataset.getOrderByAttribute() == null
            ? null
            : getAttribute(apiEntityDataset.getOrderByAttribute(), primaryEntity, underlay);

    return EntityDataset.builder()
        .primaryEntity(primaryVariable)
        .selectedAttributes(selectedAttributes)
        .orderByAttribute(orderByAttribute)
        .orderByDirection(convertOrderByDirection(apiEntityDataset.getOrderByDirection()))
        .filter(filter)
        .limit(apiEntityDataset.getLimit())
        .build();
  }

  public EntityCounts convertEntityCounts(
      String underlayName, String entityName, ApiEntityCounts apiEntityCounts) {
    Underlay underlay = getUnderlay(underlayName);
    Entity primaryEntity = getEntity(entityName, underlay);
    EntityVariable primaryVariable =
        EntityVariable.create(
            primaryEntity,
            ConversionUtils.createAndValidateVariable(apiEntityCounts.getEntityVariable()));

    VariableScope scope = new VariableScope().add(primaryVariable);
    Filter filter = new FilterConverter(underlay).convert(apiEntityCounts.getFilter(), scope);

    ImmutableList<Attribute> groupByAttributes = ImmutableList.of();
    if (apiEntityCounts.getGroupByAttributes() != null) {
      groupByAttributes =
          apiEntityCounts.getGroupByAttributes().stream()
              .map(attributeName -> getAttribute(attributeName, primaryEntity, underlay))
              .collect(ImmutableList.toImmutableList());
    }

    ImmutableList<Attribute> additionalSelectedAttributes = ImmutableList.of();
    if (apiEntityCounts.getAdditionalSelectedAttributes() != null) {
      additionalSelectedAttributes =
          apiEntityCounts.getAdditionalSelectedAttributes().stream()
              .map(attributeName -> getAttribute(attributeName, primaryEntity, underlay))
              .collect(ImmutableList.toImmutableList());
    }

    return EntityCounts.builder()
        .primaryEntity(primaryVariable)
        .groupByAttributes(groupByAttributes)
        .additionalSelectedAttributes(additionalSelectedAttributes)
        .filter(filter)
        .build();
  }

  private Underlay getUnderlay(String underlayName) {
    Optional<Underlay> underlay = underlayService.getUnderlay(underlayName);
    if (underlay.isEmpty()) {
      throw new NotFoundException(String.format("No known underlay with name '%s'", underlayName));
    }
    return underlay.get();
  }

  private static Entity getEntity(String entityName, Underlay underlay) {
    Entity entity = underlay.entities().get(entityName);
    if (entity == null) {
      throw new NotFoundException(
          String.format(
              "No known entity with name '%s' in underlay '%s'", entityName, underlay.name()));
    }
    return entity;
  }

  private static Attribute getAttribute(String attributeName, Entity entity, Underlay underlay) {
    Attribute attribute = underlay.attributes().get(entity, attributeName);
    if (attribute == null) {
      throw new NotFoundException(
          String.format(
              "No known attribute with name '%s' in entity '%s' in underlay '%s'",
              attributeName, entity.name(), underlay.name()));
    }
    return attribute;
  }

  private OrderByDirection convertOrderByDirection(ApiOrderByDirection apiOrderByDirection) {
    if (apiOrderByDirection == null) {
      return null;
    }
    switch (apiOrderByDirection) {
      case ASC:
        return OrderByDirection.ASC;
      case DESC:
        return OrderByDirection.DESC;
      default:
        throw new IllegalArgumentException(
            "Unknown order by direction enum value: " + apiOrderByDirection);
    }
  }

  private void validateLimitSize(ApiEntityDataset apiEntityDataset) {
    if (apiEntityDataset.getLimit() != null && apiEntityDataset.getLimit() <= 0) {
      throw new IllegalArgumentException(
          String.format(
              "The provided limit '%d' is not a positive integer", apiEntityDataset.getLimit()));
    }
  }
}

package bio.terra.tanagra.service.query.api;

import bio.terra.common.exception.NotFoundException;
import bio.terra.tanagra.generated.model.ApiEntityDataset;
import bio.terra.tanagra.generated.model.ApiEntityFilter;
import bio.terra.tanagra.model.Attribute;
import bio.terra.tanagra.model.Entity;
import bio.terra.tanagra.service.query.EntityDataset;
import bio.terra.tanagra.service.query.EntityFilter;
import bio.terra.tanagra.service.search.EntityVariable;
import bio.terra.tanagra.service.search.Filter;
import bio.terra.tanagra.service.underlay.UnderlayService;
import bio.terra.tanagra.underlay.Underlay;
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

    return EntityDataset.builder()
        .primaryEntity(primaryVariable)
        .selectedAttributes(selectedAttributes)
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
}

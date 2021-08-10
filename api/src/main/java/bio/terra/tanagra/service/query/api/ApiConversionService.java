package bio.terra.tanagra.service.query.api;

import bio.terra.common.exception.NotFoundException;
import bio.terra.tanagra.generated.model.ApiEntityFilter;
import bio.terra.tanagra.service.query.EntityFilter;
import bio.terra.tanagra.service.search.Entity;
import bio.terra.tanagra.service.search.EntityVariable;
import bio.terra.tanagra.service.search.Filter;
import bio.terra.tanagra.service.underlay.Underlay;
import bio.terra.tanagra.service.underlay.UnderlayService;
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
    Optional<Underlay> underlay = underlayService.getUnderlay(underlayName);
    if (underlay.isEmpty()) {
      throw new NotFoundException(String.format("No known underlay with name '%s'", underlayName));
    }
    Entity primaryEntity = underlay.get().entities().get(entityName);
    if (primaryEntity == null) {
      throw new NotFoundException(
          String.format(
              "No known entity with name '%s' in underlay '%s'", entityName, underlayName));
    }
    EntityVariable primaryVariable =
        EntityVariable.create(
            primaryEntity,
            ConversionUtils.createAndValidateVariable(apiEntityFilter.getEntityVariable()));

    VariableScope scope = new VariableScope().add(primaryVariable);
    Filter filter = new FilterConverter(underlay.get()).convert(apiEntityFilter.getFilter(), scope);

    return EntityFilter.builder().primaryEntity(primaryVariable).filter(filter).build();
  }
}

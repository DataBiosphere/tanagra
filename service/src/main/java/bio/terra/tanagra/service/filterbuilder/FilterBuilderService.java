package bio.terra.tanagra.service.filterbuilder;

import bio.terra.tanagra.api.filter.BooleanAndOrFilter;
import bio.terra.tanagra.api.filter.EntityFilter;
import bio.terra.tanagra.api.filter.RelationshipFilter;
import bio.terra.tanagra.app.controller.objmapping.FromApiUtils;
import bio.terra.tanagra.exception.InvalidConfigException;
import bio.terra.tanagra.exception.SystemException;
import bio.terra.tanagra.generated.model.ApiFilter;
import bio.terra.tanagra.query.filtervariable.BinaryFilterVariable;
import bio.terra.tanagra.query.filtervariable.BooleanAndOrFilterVariable;
import bio.terra.tanagra.service.UnderlayService;
import bio.terra.tanagra.service.accesscontrol.ResourceCollection;
import bio.terra.tanagra.service.accesscontrol.ResourceType;
import bio.terra.tanagra.service.artifact.model.CohortRevision;
import bio.terra.tanagra.service.artifact.model.Criteria;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.entitymodel.Entity;
import bio.terra.tanagra.underlay.entitymodel.Relationship;
import bio.terra.tanagra.underlay.entitymodel.entitygroup.EntityGroup;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;

public class FilterBuilderService {
  // Map of (underlay, pluginName) -> filter builder implementation class instance.
  private final Map<Pair<String, String>, FilterBuilder> pluginToImpl = new HashMap<>();
  private final UnderlayService underlayService;

  @Autowired
  public FilterBuilderService(UnderlayService underlayService) {
    this.underlayService = underlayService;
    underlayService
        .listUnderlays(ResourceCollection.allResourcesAllPermissions(ResourceType.UNDERLAY, null))
        .stream()
        .forEach(
            underlay ->
                underlay.getUiPlugins().stream()
                    .forEach(
                        pluginName ->
                            pluginToImpl.put(
                                Pair.of(underlay.getName(), pluginName),
                                FilterBuilder.Type.valueOf(pluginName).createNewInstance())));
  }

  public EntityFilter forConceptSet(String underlay, String entity, List<Criteria> criteria) {
    return null;
  }

  public EntityFilter forCohortRevision(
      String underlay, List<CohortRevision.CriteriaGroupSection> sections) {
    return null;
  }

  public EntityFilter forCriteriaGroupSection(
      String underlay, CohortRevision.CriteriaGroupSection criteriaGroupSection) {
    return null;
  }

  public EntityFilter forCriteriaGroup(
      Underlay underlay, CohortRevision.CriteriaGroup criteriaGroup) {
    // Criteria will all produce an entity filter for the same entity, which may be the primary
    // entity or not.
    List<EntityFilter> criteriaFilters =
        criteriaGroup.getCriteria().stream()
            .map(criteria -> forCriteria(underlay, criteriaGroup.getEntity(), criteria))
            .collect(Collectors.toList());
    EntityFilter allCriteriaFilter =
        criteriaFilters.size() == 1
            ? criteriaFilters.get(0)
            : new BooleanAndOrFilter(
                BooleanAndOrFilterVariable.LogicalOperator.AND, criteriaFilters);

    // Criteria groups always produce an entity filter for the primary entity.
    if (criteriaGroup.getEntity().equals(underlay.getPrimaryEntity().getName())) {
      // Criteria filters are already for the primary entity, just return them.
      return allCriteriaFilter;
    }

    // Use a relationship to connect the all criteria filter to the primary entity.
    Entity entity = underlay.getEntity(criteriaGroup.getEntity());
    EntityGroup entityGroup = underlay.getEntityGroup(criteriaGroup.getEntityGroup());
    Relationship relationship =
        entityGroup.getRelationships().stream()
            .filter(rel -> rel.matchesEntities(entity, underlay.getPrimaryEntity()))
            .findAny()
            .orElseThrow(
                () ->
                    new SystemException(
                        "Relationship to primary entity not found for entity: "
                            + entity.getName()
                            + ", entity group: "
                            + entityGroup.getName()));
    if (criteriaGroup.getGroupByCountOperator() == null) {
      return new RelationshipFilter(
          underlay,
          entityGroup,
          underlay.getPrimaryEntity(),
          relationship,
          allCriteriaFilter,
          null,
          null,
          null);
    } else {
      // Add a group by filter.
      return new RelationshipFilter(
          underlay,
          entityGroup,
          underlay.getPrimaryEntity(),
          relationship,
          allCriteriaFilter,
          // TODO: Pull this from the group by criteria.
          entity.getAttribute("start_date"),
          BinaryFilterVariable.BinaryOperator.GREATER_THAN,
          12);
    }
  }

  public EntityFilter forCriteria(Underlay underlay, String entity, Criteria criteria) {
    FilterBuilder filterBuilder =
        pluginToImpl.get(Pair.of(underlay.getName(), criteria.getPluginName()));
    if (filterBuilder == null) {
      throw new InvalidConfigException(
          "Plugin implementation class not found: underlay="
              + underlay.getName()
              + ", pluginName="
              + criteria.getPluginName());
    }
    FilterBuilderInput filterBuilderInput =
        new FilterBuilderInput(underlay.getName(), entity, criteria.getPluginVersion());
    ApiFilter apiFilter = filterBuilder.upgradeVersionAndBuildFilter(filterBuilderInput);
    return FromApiUtils.fromApiObject(apiFilter, underlay);
  }
}

package bio.terra.tanagra.service.filterbuilder;

import bio.terra.tanagra.api.filter.EntityFilter;
import bio.terra.tanagra.app.controller.objmapping.FromApiUtils;
import bio.terra.tanagra.app.controller.objmapping.ToApiUtils;
import bio.terra.tanagra.exception.InvalidConfigException;
import bio.terra.tanagra.generated.model.ApiFilter;
import bio.terra.tanagra.service.UnderlayService;
import bio.terra.tanagra.service.accesscontrol.ResourceCollection;
import bio.terra.tanagra.service.accesscontrol.ResourceType;
import bio.terra.tanagra.service.artifact.model.CohortRevision;
import bio.terra.tanagra.service.artifact.model.Criteria;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
      String underlay, CohortRevision.CriteriaGroup criteriaGroup) {
    return null;
  }

  public EntityFilter forCriteria(String underlay, Criteria criteria) {
    FilterBuilder filterBuilder = pluginToImpl.get(Pair.of(underlay, criteria.getPluginName()));
    if (filterBuilder == null) {
      throw new InvalidConfigException(
          "Plugin implementation class not found: underlay="
              + underlay
              + ", pluginName="
              + criteria.getPluginName());
    }
    FilterBuilderInput filterBuilderInput =
        new FilterBuilderInput(
            criteria.getPluginVersion(), ToApiUtils.toApiObject(criteria.getPluginData()));
    ApiFilter apiFilter = filterBuilder.upgradeVersionAndBuildFilter(filterBuilderInput);
    return FromApiUtils.fromApiObject(apiFilter, underlayService.getUnderlay(underlay));
  }
}

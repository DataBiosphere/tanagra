package bio.terra.tanagra.service.filterbuilder;

import bio.terra.tanagra.api.filter.BooleanAndOrFilter;
import bio.terra.tanagra.api.filter.BooleanNotFilter;
import bio.terra.tanagra.api.filter.EntityFilter;
import bio.terra.tanagra.filterbuilder.EntityOutput;
import bio.terra.tanagra.filterbuilder.FilterBuilder;
import bio.terra.tanagra.service.UnderlayService;
import bio.terra.tanagra.service.artifact.model.Cohort;
import bio.terra.tanagra.service.artifact.model.CohortRevision;
import bio.terra.tanagra.service.artifact.model.ConceptSet;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.entitymodel.Entity;
import bio.terra.tanagra.underlay.uiplugin.SelectionData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;

public class FilterBuilderService {
  private final UnderlayService underlayService;

  @Autowired
  public FilterBuilderService(UnderlayService underlayService) {
    this.underlayService = underlayService;
  }

  public EntityFilter buildFilterForCriteriaGroup(
      String underlayName, CohortRevision.CriteriaGroup criteriaGroup) {
    if (criteriaGroup.getCriteria().isEmpty()) {
      return null;
    }

    // TODO: Replace plugin name with criteria selector name, once we're storing that.
    String criteriaSelectorName = criteriaGroup.getCriteria().get(0).getPluginName();
    List<SelectionData> selectionData =
        criteriaGroup.getCriteria().stream()
            .map(
                criteria ->
                    new SelectionData(criteria.getPluginName(), criteria.getSelectionData()))
            .collect(Collectors.toList());

    Underlay underlay = underlayService.getUnderlay(underlayName);
    FilterBuilder filterBuilder =
        underlay.getCriteriaSelector(criteriaSelectorName).getFilterBuilder();
    return filterBuilder.buildForCohort(underlay, selectionData);
  }

  public EntityFilter buildFilterForCriteriaGroupSection(
      String underlayName, CohortRevision.CriteriaGroupSection criteriaGroupSection) {
    List<EntityFilter> criteriaGroupFilters = new ArrayList<>();
    criteriaGroupSection.getCriteriaGroups().stream()
        .forEach(
            criteriaGroup -> {
              EntityFilter criteriaGroupFilter =
                  buildFilterForCriteriaGroup(underlayName, criteriaGroup);
              if (criteriaGroupFilter != null) {
                criteriaGroupFilters.add(criteriaGroupFilter);
              }
            });
    if (criteriaGroupFilters.isEmpty()) {
      return null;
    }

    EntityFilter combinedFilter =
        criteriaGroupFilters.size() == 1
            ? criteriaGroupFilters.get(0)
            : new BooleanAndOrFilter(criteriaGroupSection.getOperator(), criteriaGroupFilters);
    return criteriaGroupSection.isExcluded()
        ? new BooleanNotFilter(combinedFilter)
        : combinedFilter;
  }

  public EntityFilter buildFilterForCohortRevision(
      String underlayName, CohortRevision cohortRevision) {
    List<EntityFilter> criteriaGroupSectionFilters = new ArrayList<>();
    cohortRevision.getSections().stream()
        .forEach(
            criteriaGroupSection -> {
              EntityFilter criteriaGroupSectionFilter =
                  buildFilterForCriteriaGroupSection(underlayName, criteriaGroupSection);
              if (criteriaGroupSectionFilter != null) {
                criteriaGroupSectionFilters.add(criteriaGroupSectionFilter);
              }
            });
    if (criteriaGroupSectionFilters.isEmpty()) {
      return null;
    }

    return criteriaGroupSectionFilters.size() == 1
        ? criteriaGroupSectionFilters.get(0)
        : new BooleanAndOrFilter(
            BooleanAndOrFilter.LogicalOperator.AND, criteriaGroupSectionFilters);
  }

  public List<EntityOutput> buildOutputsForConceptSet(ConceptSet conceptSet) {
    if (conceptSet.getCriteria().isEmpty()) {
      return List.of();
    }

    // TODO: Replace plugin name with criteria selector name, once we're storing that.
    String criteriaSelectorName = conceptSet.getCriteria().get(0).getPluginName();
    List<SelectionData> selectionData =
        conceptSet.getCriteria().stream()
            .map(
                criteria ->
                    new SelectionData(criteria.getPluginName(), criteria.getSelectionData()))
            .collect(Collectors.toList());

    Underlay underlay = underlayService.getUnderlay(conceptSet.getUnderlay());
    FilterBuilder filterBuilder =
        underlay.getCriteriaSelector(criteriaSelectorName).getFilterBuilder();
    return filterBuilder.buildForDataFeature(underlay, selectionData);
  }

  public List<EntityOutput> buildOutputsForExport(
      List<Cohort> cohorts, List<ConceptSet> conceptSets) {
    // Build a single filter on the primary entity by OR-ing all the individual cohort filters
    // together.
    List<EntityFilter> cohortFilters = new ArrayList<>();
    cohorts.stream()
        .forEach(
            cohort -> {
              EntityFilter cohortFilter =
                  buildFilterForCohortRevision(
                      cohort.getUnderlay(), cohort.getMostRecentRevision());
              if (cohortFilter != null) {
                cohortFilters.add(cohortFilter);
              }
            });
    EntityFilter primaryEntityFilter;
    if (cohortFilters.isEmpty()) {
      primaryEntityFilter = null;
    } else if (cohortFilters.size() == 1) {
      primaryEntityFilter = cohortFilters.get(0);
    } else {
      primaryEntityFilter =
          new BooleanAndOrFilter(BooleanAndOrFilter.LogicalOperator.OR, cohortFilters);
    }

    // Build a list of output entities and the filters on them from the data feature sets.
    Map<Entity, List<EntityFilter>> outputEntitiesAndFilters = new HashMap<>();
    conceptSets.stream()
        .forEach(
            conceptSet -> {
              List<EntityOutput> entityOutputs = buildOutputsForConceptSet(conceptSet);
              entityOutputs.stream()
                  .forEach(
                      entityOutput -> {
                        List<EntityFilter> entityFilters =
                            outputEntitiesAndFilters.containsKey(entityOutput.getEntity())
                                ? outputEntitiesAndFilters.get(entityOutput.getEntity())
                                : new ArrayList<>();
                        if (entityOutput.hasDataFeatureFilter()) {
                          entityFilters.add(entityOutput.getDataFeatureFilter());
                        }
                        outputEntitiesAndFilters.put(entityOutput.getEntity(), entityFilters);
                      });
            });

    // Build a single filter for each output entity that includes the data feature filters and the
    // primary entity filter.
    List<EntityOutput> entityOutputs = new ArrayList<>();
    outputEntitiesAndFilters.entrySet().stream()
        .forEach(
            entry -> {
              // Find the relationship between this output entity and the primary entity.
              // TODO
            });
    return entityOutputs;
  }
}

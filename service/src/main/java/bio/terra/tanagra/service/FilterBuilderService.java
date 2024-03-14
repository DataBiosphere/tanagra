package bio.terra.tanagra.service;

import bio.terra.tanagra.api.filter.BooleanAndOrFilter;
import bio.terra.tanagra.api.filter.BooleanNotFilter;
import bio.terra.tanagra.api.filter.EntityFilter;
import bio.terra.tanagra.api.filter.RelationshipFilter;
import bio.terra.tanagra.exception.InvalidQueryException;
import bio.terra.tanagra.filterbuilder.EntityOutput;
import bio.terra.tanagra.filterbuilder.FilterBuilder;
import bio.terra.tanagra.service.artifact.model.Cohort;
import bio.terra.tanagra.service.artifact.model.CohortRevision;
import bio.terra.tanagra.service.artifact.model.ConceptSet;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.entitymodel.Entity;
import bio.terra.tanagra.underlay.entitymodel.Relationship;
import bio.terra.tanagra.underlay.entitymodel.entitygroup.EntityGroup;
import bio.terra.tanagra.underlay.uiplugin.SelectionData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
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

    String criteriaSelectorOrModifierName =
        criteriaGroup.getCriteria().get(0).getSelectorOrModifierName();
    List<SelectionData> selectionData =
        criteriaGroup.getCriteria().stream()
            .map(
                criteria ->
                    new SelectionData(
                        criteria.getSelectorOrModifierName(), criteria.getSelectionData()))
            .collect(Collectors.toList());

    Underlay underlay = underlayService.getUnderlay(underlayName);
    FilterBuilder filterBuilder =
        underlay.getCriteriaSelector(criteriaSelectorOrModifierName).getFilterBuilder();
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

  public List<EntityOutput> buildOutputsForConceptSets(List<ConceptSet> conceptSets) {
    // All data feature sets must be for the same underlay.
    String underlayName = conceptSets.get(0).getUnderlay();
    conceptSets.stream()
        .forEach(
            conceptSet -> {
              if (!conceptSet.getUnderlay().equals(underlayName)) {
                throw new InvalidQueryException(
                    "All data feature sets must be for the same underlay: "
                        + underlayName
                        + ", "
                        + conceptSet.getUnderlay());
              }
            });
    Underlay underlay = underlayService.getUnderlay(underlayName);

    // Build a list of output entities and the filters on them from the data feature sets.
    Map<Entity, List<EntityFilter>> outputEntitiesAndDataFeatureFilters = new HashMap<>();
    conceptSets.stream()
        .forEach(
            conceptSet -> {
              if (conceptSet.getCriteria().isEmpty()) {
                return;
              }
              conceptSet.getCriteria().stream()
                  .forEach(
                      criteria -> {
                        String criteriaSelectorOrModifierName =
                            criteria.getSelectorOrModifierName();
                        FilterBuilder filterBuilder =
                            underlay
                                .getCriteriaSelector(criteriaSelectorOrModifierName)
                                .getFilterBuilder();

                        // Generate the entity outputs for each concept set criteria.
                        List<SelectionData> selectionData =
                            criteria.getSelectionData() == null
                                    || criteria.getSelectionData().isEmpty()
                                ? List.of()
                                : List.of(new SelectionData(null, criteria.getSelectionData()));
                        List<EntityOutput> entityOutputs =
                            filterBuilder.buildForDataFeature(underlay, selectionData);

                        // Break apart the entity outputs into entity-filter[] pairs, so we can
                        // combine filters for the same entity across concept set criteria.
                        entityOutputs.stream()
                            .forEach(
                                entityOutput -> {
                                  List<EntityFilter> entityFilters =
                                      outputEntitiesAndDataFeatureFilters.containsKey(
                                              entityOutput.getEntity())
                                          ? outputEntitiesAndDataFeatureFilters.get(
                                              entityOutput.getEntity())
                                          : new ArrayList<>();
                                  if (entityOutput.hasDataFeatureFilter()) {
                                    entityFilters.add(entityOutput.getDataFeatureFilter());
                                  }
                                  outputEntitiesAndDataFeatureFilters.put(
                                      entityOutput.getEntity(), entityFilters);
                                });
                      });
            });

    // Build a single filter per output entity that includes all relevant data feature
    // sets, by OR-ing the individual data feature set filters.
    // e.g. data feature set 1 = condition diabetes, data feature set 2 = condition
    // hypertension, output entity condition_occurrence filtered on condition diabetes or
    // hypertension
    List<EntityOutput> entityOutputs = new ArrayList<>();
    outputEntitiesAndDataFeatureFilters.entrySet().stream()
        .forEach(
            entry -> {
              Entity outputEntity = entry.getKey();
              List<EntityFilter> filters = entry.getValue();
              if (filters.isEmpty()) {
                entityOutputs.add(EntityOutput.unfiltered(outputEntity));
              } else if (filters.size() == 1) {
                entityOutputs.add(EntityOutput.filtered(outputEntity, filters.get(0)));
              } else {
                entityOutputs.add(
                    EntityOutput.filtered(
                        outputEntity,
                        new BooleanAndOrFilter(BooleanAndOrFilter.LogicalOperator.OR, filters)));
              }
            });
    return entityOutputs;
  }

  public List<EntityOutput> buildOutputsForExport(
      List<Cohort> cohorts, List<ConceptSet> conceptSets) {
    // All cohorts must be for the same underlay.
    String underlayName = cohorts.get(0).getUnderlay();
    cohorts.stream()
        .forEach(
            cohort -> {
              if (!cohort.getUnderlay().equals(underlayName)) {
                throw new InvalidQueryException(
                    "All cohorts must be for the same underlay: "
                        + underlayName
                        + ", "
                        + cohort.getUnderlay());
              }
            });

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
    EntityFilter combinedCohortFilter;
    if (cohortFilters.isEmpty()) {
      combinedCohortFilter = null;
    } else if (cohortFilters.size() == 1) {
      combinedCohortFilter = cohortFilters.get(0);
    } else {
      combinedCohortFilter =
          new BooleanAndOrFilter(BooleanAndOrFilter.LogicalOperator.OR, cohortFilters);
    }

    // Build a combined filter per output entity from all the data feature sets.
    List<EntityOutput> dataFeatureOutputs = buildOutputsForConceptSets(conceptSets);

    Underlay underlay = underlayService.getUnderlay(underlayName);
    List<EntityOutput> entityOutputs = new ArrayList<>();
    dataFeatureOutputs.stream()
        .forEach(
            dataFeatureOutput -> {
              Entity outputEntity = dataFeatureOutput.getEntity();
              List<EntityFilter> outputEntitySubFilters = new ArrayList<>();
              if (dataFeatureOutput.hasDataFeatureFilter()) {
                outputEntitySubFilters.add(dataFeatureOutput.getDataFeatureFilter());
              }

              if (combinedCohortFilter != null) {
                // Find the relationship between this output entity and the primary entity.
                Pair<EntityGroup, Relationship> outputToPrimary =
                    underlay.getRelationship(outputEntity, underlay.getPrimaryEntity());

                // Build a single relationship filter per output entity that has the combined cohort
                // filter as the primary entity sub-filter.
                outputEntitySubFilters.add(
                    new RelationshipFilter(
                        underlay,
                        outputToPrimary.getLeft(),
                        outputEntity,
                        outputToPrimary.getRight(),
                        combinedCohortFilter,
                        null,
                        null,
                        null));
              }

              // Build a single filter per output entity with the combined cohort and data feature
              // filters.
              if (outputEntitySubFilters.isEmpty()) {
                entityOutputs.add(EntityOutput.unfiltered(outputEntity));
              } else if (outputEntitySubFilters.size() == 1) {
                entityOutputs.add(
                    EntityOutput.filtered(outputEntity, outputEntitySubFilters.get(0)));
              } else {
                entityOutputs.add(
                    EntityOutput.filtered(
                        outputEntity,
                        new BooleanAndOrFilter(
                            BooleanAndOrFilter.LogicalOperator.AND, outputEntitySubFilters)));
              }
            });
    return entityOutputs;
  }
}

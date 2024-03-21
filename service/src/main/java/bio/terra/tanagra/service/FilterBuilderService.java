package bio.terra.tanagra.service;

import bio.terra.tanagra.api.filter.BooleanAndOrFilter;
import bio.terra.tanagra.api.filter.BooleanNotFilter;
import bio.terra.tanagra.api.filter.EntityFilter;
import bio.terra.tanagra.api.filter.GroupHasItemsFilter;
import bio.terra.tanagra.api.filter.ItemInGroupFilter;
import bio.terra.tanagra.api.filter.OccurrenceForPrimaryFilter;
import bio.terra.tanagra.exception.InvalidQueryException;
import bio.terra.tanagra.exception.SystemException;
import bio.terra.tanagra.filterbuilder.EntityOutput;
import bio.terra.tanagra.filterbuilder.FilterBuilder;
import bio.terra.tanagra.service.artifact.model.Cohort;
import bio.terra.tanagra.service.artifact.model.CohortRevision;
import bio.terra.tanagra.service.artifact.model.ConceptSet;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.entitymodel.Attribute;
import bio.terra.tanagra.underlay.entitymodel.Entity;
import bio.terra.tanagra.underlay.entitymodel.entitygroup.CriteriaOccurrence;
import bio.terra.tanagra.underlay.entitymodel.entitygroup.EntityGroup;
import bio.terra.tanagra.underlay.entitymodel.entitygroup.GroupItems;
import bio.terra.tanagra.underlay.uiplugin.PrepackagedCriteria;
import bio.terra.tanagra.underlay.uiplugin.SelectionData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class FilterBuilderService {
  private static final Logger LOGGER = LoggerFactory.getLogger(FilterBuilderService.class);
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

    String criteriaSelectorName = criteriaGroup.getCriteria().get(0).getSelectorOrModifierName();
    List<SelectionData> selectionData =
        criteriaGroup.getCriteria().stream()
            .map(
                criteria ->
                    new SelectionData(
                        criteria.getSelectorOrModifierName(), criteria.getSelectionData()))
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

  public EntityFilter buildFilterForCohortRevisions(
      String underlayName, List<CohortRevision> cohortRevisions) {
    List<EntityFilter> cohortRevisionFilters = new ArrayList<>();
    cohortRevisions.stream()
        .forEach(
            cohortRevision -> {
              EntityFilter entityFilter =
                  buildFilterForCohortRevision(underlayName, cohortRevision);
              if (entityFilter != null) {
                cohortRevisionFilters.add(entityFilter);
              }
            });
    if (cohortRevisionFilters.isEmpty()) {
      return null;
    } else if (cohortRevisionFilters.size() == 1) {
      return cohortRevisionFilters.get(0);
    } else {
      return new BooleanAndOrFilter(BooleanAndOrFilter.LogicalOperator.OR, cohortRevisionFilters);
    }
  }

  public List<EntityOutput> buildOutputsForConceptSets(List<ConceptSet> conceptSets) {
    // No concept sets = no entity outputs.
    if (conceptSets.isEmpty()) {
      return List.of();
    }

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

    // Build a list of output entities, the filters on them, and the attributes to include from the
    // data feature sets.
    Map<Entity, Pair<List<EntityFilter>, Set<Attribute>>> outputEntitiesAndFiltersAndAttributes =
        new HashMap<>();
    conceptSets.stream()
        .forEach(
            conceptSet -> {
              if (conceptSet.getCriteria().isEmpty()) {
                LOGGER.debug("Concept set has no criteria: {}", conceptSet.getId());
                return;
              }
              conceptSet.getCriteria().stream()
                  .forEach(
                      criteria -> {
                        String criteriaSelectorName;
                        List<SelectionData> selectionData;
                        if (criteria.getPredefinedId() != null
                            && !criteria.getPredefinedId().isEmpty()) {
                          LOGGER.debug(
                              "Concept set contains prepackaged criteria: {}",
                              criteria.getPredefinedId());
                          // This is a prepackaged data feature.
                          // The criteria selector and plugin data are specified in the prepackaged
                          // data feature definition.
                          PrepackagedCriteria prepackagedCriteria =
                              underlay.getPrepackagedDataFeature(criteria.getPredefinedId());
                          criteriaSelectorName = prepackagedCriteria.getCriteriaSelector();
                          selectionData =
                              prepackagedCriteria.hasSelectionData()
                                  ? List.of(prepackagedCriteria.getSelectionData())
                                  : List.of();
                        } else {
                          // This is Not a prepackaged data feature.
                          // The criteria selector and plugin data are specified on the criteria
                          // itself.
                          criteriaSelectorName = criteria.getSelectorOrModifierName();
                          selectionData =
                              criteria.getSelectionData() == null
                                      || criteria.getSelectionData().isEmpty()
                                  ? List.of()
                                  : List.of(new SelectionData(null, criteria.getSelectionData()));
                        }

                        // Generate the entity outputs for each concept set criteria.
                        FilterBuilder filterBuilder =
                            underlay.getCriteriaSelector(criteriaSelectorName).getFilterBuilder();
                        List<EntityOutput> entityOutputs =
                            filterBuilder.buildForDataFeature(underlay, selectionData);

                        entityOutputs.stream()
                            .forEach(
                                entityOutput -> {
                                  LOGGER.debug(
                                      "Entity output for export: {}",
                                      entityOutput.getEntity().getName());
                                  // Break apart the entity outputs into entity-filter[] pairs, so
                                  // we can combine filters for the same entity across concept sets
                                  // and criteria.
                                  List<EntityFilter> entityFilters =
                                      outputEntitiesAndFiltersAndAttributes.containsKey(
                                              entityOutput.getEntity())
                                          ? outputEntitiesAndFiltersAndAttributes
                                              .get(entityOutput.getEntity())
                                              .getLeft()
                                          : new ArrayList<>();
                                  if (entityOutput.hasDataFeatureFilter()) {
                                    entityFilters.add(entityOutput.getDataFeatureFilter());
                                  }

                                  // Add to the list of attributes to include for this entity.
                                  List<String> excludeAttrNames =
                                      conceptSet.containsExcludeOutputAttributes(
                                              entityOutput.getEntity())
                                          ? conceptSet.getExcludeOutputAttributes(
                                              entityOutput.getEntity())
                                          : List.of();

                                  Set<Attribute> includeAttributes =
                                      outputEntitiesAndFiltersAndAttributes.containsKey(
                                              entityOutput.getEntity())
                                          ? outputEntitiesAndFiltersAndAttributes
                                              .get(entityOutput.getEntity())
                                              .getRight()
                                          : new HashSet<>();
                                  entityOutput.getEntity().getAttributes().stream()
                                      .filter(
                                          attribute ->
                                              !excludeAttrNames.contains(attribute.getName()))
                                      .forEach(attribute -> includeAttributes.add(attribute));

                                  outputEntitiesAndFiltersAndAttributes.put(
                                      entityOutput.getEntity(),
                                      Pair.of(entityFilters, includeAttributes));
                                });
                      });
            });

    // Build a single filter per output entity that includes all relevant data feature
    // sets, by OR-ing the individual data feature set filters.
    // e.g. data feature set 1 = condition diabetes, data feature set 2 = condition
    // hypertension, output entity condition_occurrence filtered on condition diabetes or
    // hypertension
    List<EntityOutput> entityOutputs = new ArrayList<>();
    outputEntitiesAndFiltersAndAttributes.entrySet().stream()
        .forEach(
            entry -> {
              Entity outputEntity = entry.getKey();
              List<EntityFilter> filters = entry.getValue().getLeft();
              List<Attribute> includeAttributes = new ArrayList<>(entry.getValue().getRight());
              if (filters.isEmpty()) {
                entityOutputs.add(EntityOutput.unfiltered(outputEntity, includeAttributes));
              } else if (filters.size() == 1) {
                entityOutputs.add(
                    EntityOutput.filtered(outputEntity, filters.get(0), includeAttributes));
              } else {
                entityOutputs.add(
                    EntityOutput.filtered(
                        outputEntity,
                        new BooleanAndOrFilter(BooleanAndOrFilter.LogicalOperator.OR, filters),
                        includeAttributes));
              }
            });
    return entityOutputs;
  }

  public List<EntityOutput> buildOutputsForExport(
      List<Cohort> cohorts, List<ConceptSet> conceptSets) {
    // No cohorts and no concept sets = no entity outputs.
    if (cohorts.isEmpty() && conceptSets.isEmpty()) {
      return List.of();
    }
    String underlayName =
        cohorts.isEmpty() ? conceptSets.get(0).getUnderlay() : cohorts.get(0).getUnderlay();

    // All cohorts must be for the same underlay.
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
    EntityFilter combinedCohortFilter =
        buildFilterForCohortRevisions(
            underlayName,
            cohorts.stream().map(Cohort::getMostRecentRevision).collect(Collectors.toList()));

    // Build a combined filter per output entity from all the data feature sets.
    List<EntityOutput> dataFeatureOutputs = buildOutputsForConceptSets(conceptSets);

    // If there's no cohort filter, just return the entity output from the concept sets.
    if (combinedCohortFilter == null) {
      return dataFeatureOutputs;
    }

    Underlay underlay = underlayService.getUnderlay(underlayName);
    return dataFeatureOutputs.stream()
        .map(
            dataFeatureOutput -> {
              Entity outputEntity = dataFeatureOutput.getEntity();

              // If the output entity is the primary entity, just add the cohort filter.
              if (outputEntity.isPrimary()) {
                return EntityOutput.filtered(
                    outputEntity, combinedCohortFilter, dataFeatureOutput.getAttributes());
              }

              // Find the relationship between this output entity and the primary entity.
              EntityGroup outputToPrimaryEntityGroup =
                  underlay.getRelationship(outputEntity, underlay.getPrimaryEntity()).getLeft();

              // Build a single relationship filter per output entity that has the combined cohort
              // filter as the primary entity sub-filter.
              switch (outputToPrimaryEntityGroup.getType()) {
                case CRITERIA_OCCURRENCE:
                  return EntityOutput.filtered(
                      outputEntity,
                      new OccurrenceForPrimaryFilter(
                          underlay,
                          (CriteriaOccurrence) outputToPrimaryEntityGroup,
                          outputEntity,
                          combinedCohortFilter,
                          dataFeatureOutput.getDataFeatureFilter()),
                      dataFeatureOutput.getAttributes());
                case GROUP_ITEMS:
                  GroupItems groupItems = (GroupItems) outputToPrimaryEntityGroup;
                  EntityFilter occurrenceForPrimaryFilter;
                  if (groupItems.getGroupEntity().isPrimary()) {
                    occurrenceForPrimaryFilter =
                        new ItemInGroupFilter(
                            underlay, groupItems, combinedCohortFilter, null, null, null);
                  } else {
                    occurrenceForPrimaryFilter =
                        new GroupHasItemsFilter(
                            underlay, groupItems, combinedCohortFilter, null, null, null);
                  }
                  // Build a single filter per output entity with the combined cohort and data
                  // feature filters.
                  if (!dataFeatureOutput.hasDataFeatureFilter()) {
                    return EntityOutput.filtered(
                        outputEntity,
                        occurrenceForPrimaryFilter,
                        dataFeatureOutput.getAttributes());
                  } else {
                    return EntityOutput.filtered(
                        outputEntity,
                        new BooleanAndOrFilter(
                            BooleanAndOrFilter.LogicalOperator.AND,
                            List.of(
                                dataFeatureOutput.getDataFeatureFilter(),
                                occurrenceForPrimaryFilter)),
                        dataFeatureOutput.getAttributes());
                  }
                default:
                  throw new SystemException(
                      "Unsupported entity group type: " + outputToPrimaryEntityGroup.getType());
              }
            })
        .collect(Collectors.toList());
  }
}

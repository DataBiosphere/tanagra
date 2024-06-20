package bio.terra.tanagra.service.filter;

import bio.terra.tanagra.api.field.AttributeField;
import bio.terra.tanagra.api.field.ValueDisplayField;
import bio.terra.tanagra.api.filter.AttributeFilter;
import bio.terra.tanagra.api.filter.BooleanAndOrFilter;
import bio.terra.tanagra.api.filter.BooleanNotFilter;
import bio.terra.tanagra.api.filter.EntityFilter;
import bio.terra.tanagra.api.filter.GroupHasItemsFilter;
import bio.terra.tanagra.api.filter.ItemInGroupFilter;
import bio.terra.tanagra.api.filter.OccurrenceForPrimaryFilter;
import bio.terra.tanagra.api.filter.RelationshipFilter;
import bio.terra.tanagra.api.shared.BinaryOperator;
import bio.terra.tanagra.api.shared.Literal;
import bio.terra.tanagra.exception.InvalidQueryException;
import bio.terra.tanagra.exception.SystemException;
import bio.terra.tanagra.filterbuilder.EntityOutput;
import bio.terra.tanagra.filterbuilder.FilterBuilder;
import bio.terra.tanagra.service.UnderlayService;
import bio.terra.tanagra.service.artifact.model.Cohort;
import bio.terra.tanagra.service.artifact.model.CohortRevision;
import bio.terra.tanagra.service.artifact.model.ConceptSet;
import bio.terra.tanagra.service.artifact.model.Criteria;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.entitymodel.Attribute;
import bio.terra.tanagra.underlay.entitymodel.Entity;
import bio.terra.tanagra.underlay.entitymodel.Relationship;
import bio.terra.tanagra.underlay.entitymodel.entitygroup.CriteriaOccurrence;
import bio.terra.tanagra.underlay.entitymodel.entitygroup.EntityGroup;
import bio.terra.tanagra.underlay.entitymodel.entitygroup.GroupItems;
import bio.terra.tanagra.underlay.uiplugin.PrepackagedCriteria;
import bio.terra.tanagra.underlay.uiplugin.SelectionData;
import jakarta.annotation.Nullable;
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
    criteriaGroupSection
        .getCriteriaGroups()
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
    cohortRevision
        .getSections()
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
    cohortRevisions.forEach(
        cohortRevision -> {
          EntityFilter entityFilter = buildFilterForCohortRevision(underlayName, cohortRevision);
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

  public List<EntityOutputPreview> buildOutputPreviewsForConceptSets(
      List<ConceptSet> conceptSets, boolean includeAllAttributes) {
    // No concept sets = no entity outputs.
    if (conceptSets.isEmpty()) {
      return List.of();
    }

    // All data feature sets must be for the same underlay.
    String underlayName = conceptSets.get(0).getUnderlay();
    conceptSets.forEach(
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
    // Keep track of the specific criteria that each output entity is attributed to.
    Map<Entity, List<Pair<ConceptSet, Criteria>>> outputEntitiesAndAttributedCriteria =
        new HashMap<>();
    conceptSets.forEach(
        conceptSet -> {
          if (conceptSet.getCriteria().isEmpty()) {
            LOGGER.debug("Concept set has no criteria: {}", conceptSet.getId());
            return;
          }
          conceptSet
              .getCriteria()
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

                    entityOutputs.forEach(
                        entityOutput -> {
                          LOGGER.debug(
                              "Entity output for export: {}", entityOutput.getEntity().getName());
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
                              conceptSet.getExcludeOutputAttributes(entityOutput.getEntity());

                          Set<Attribute> includeAttributes =
                              outputEntitiesAndFiltersAndAttributes.containsKey(
                                      entityOutput.getEntity())
                                  ? outputEntitiesAndFiltersAndAttributes
                                      .get(entityOutput.getEntity())
                                      .getRight()
                                  : new HashSet<>();
                          entityOutput.getEntity().getAttributes().stream()
                              .filter(attribute -> !excludeAttrNames.contains(attribute.getName()))
                              .forEach(includeAttributes::add);

                          outputEntitiesAndFiltersAndAttributes.put(
                              entityOutput.getEntity(), Pair.of(entityFilters, includeAttributes));

                          // Add to the list of criteria to attribute to this entity.
                          List<Pair<ConceptSet, Criteria>> attributedCriteria =
                              outputEntitiesAndAttributedCriteria.containsKey(
                                      entityOutput.getEntity())
                                  ? outputEntitiesAndAttributedCriteria.get(
                                      entityOutput.getEntity())
                                  : new ArrayList<>();
                          attributedCriteria.add(Pair.of(conceptSet, criteria));
                          outputEntitiesAndAttributedCriteria.put(
                              entityOutput.getEntity(), attributedCriteria);
                        });
                  });
        });

    // Build a single filter per output entity that includes all relevant data feature
    // sets, by OR-ing the individual data feature set filters.
    // e.g. data feature set 1 = condition diabetes, data feature set 2 = condition
    // hypertension, output entity condition_occurrence filtered on condition diabetes or
    // hypertension
    List<EntityOutputPreview> entityOutputs = new ArrayList<>();
    outputEntitiesAndFiltersAndAttributes.forEach(
        (outputEntity, value) -> {
          List<EntityFilter> filters = value.getLeft();
          List<Attribute> includeAttributes =
              includeAllAttributes
                  ? outputEntity.getAttributes().stream()
                      .filter(attribute -> !attribute.isSuppressedForExport())
                      .collect(Collectors.toList())
                  : new ArrayList<>(value.getRight());
          EntityOutput entityOutput;
          if (filters.isEmpty()) {
            entityOutput = EntityOutput.unfiltered(outputEntity, includeAttributes);
          } else if (filters.size() == 1) {
            entityOutput = EntityOutput.filtered(outputEntity, filters.get(0), includeAttributes);
          } else {
            entityOutput =
                EntityOutput.filtered(
                    outputEntity,
                    new BooleanAndOrFilter(BooleanAndOrFilter.LogicalOperator.OR, filters),
                    includeAttributes);
          }

          // Build the attribute fields to select.
          List<ValueDisplayField> selectedAttributeFields = new ArrayList<>();
          entityOutput
              .getAttributes()
              .forEach(
                  includeAttribute ->
                      selectedAttributeFields.add(
                          new AttributeField(underlay, outputEntity, includeAttribute, false)));
          entityOutputs.add(
              new EntityOutputPreview()
                  .setEntityOutput(entityOutput)
                  .setAttributedCriteria(outputEntitiesAndAttributedCriteria.get(outputEntity))
                  .setSelectedFields(selectedAttributeFields));
        });
    return entityOutputs;
  }

  public List<EntityOutputPreview> buildOutputPreviewsForExport(
      List<Cohort> cohorts, List<ConceptSet> conceptSets, boolean includeAllAttributes) {
    // No cohorts and no concept sets = no entity outputs.
    if (cohorts.isEmpty() && conceptSets.isEmpty()) {
      return List.of();
    }
    String underlayName =
        cohorts.isEmpty() ? conceptSets.get(0).getUnderlay() : cohorts.get(0).getUnderlay();

    // All cohorts must be for the same underlay.
    cohorts.forEach(
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
    List<EntityOutputPreview> dataFeatureOutputPreviews =
        buildOutputPreviewsForConceptSets(conceptSets, includeAllAttributes);

    // If there's no cohort filter, just return the entity output from the concept sets.
    if (combinedCohortFilter == null) {
      return dataFeatureOutputPreviews;
    }

    Underlay underlay = underlayService.getUnderlay(underlayName);
    return dataFeatureOutputPreviews.stream()
        .map(
            dataFeatureOutputPreview -> {
              EntityOutput entityOutput = dataFeatureOutputPreview.getEntityOutput();
              EntityFilter outputEntityWithCohortFilter =
                  filterOutputByPrimaryEntity(
                      underlay,
                      entityOutput.getEntity(),
                      entityOutput.getDataFeatureFilter(),
                      combinedCohortFilter);
              return dataFeatureOutputPreview.setEntityOutput(
                  EntityOutput.filtered(
                      entityOutput.getEntity(),
                      outputEntityWithCohortFilter,
                      entityOutput.getAttributes()));
            })
        .collect(Collectors.toList());
  }

  public EntityFilter filterOutputByPrimaryEntity(
      Underlay underlay,
      Entity outputEntity,
      @Nullable EntityFilter outputEntityFilter,
      @Nullable EntityFilter primaryEntityFilter) {
    // If the output entity is the primary entity, just return the cohort filter.
    if (outputEntity.isPrimary()) {
      if (outputEntityFilter != null) {
        throw new SystemException("Unsupported data feature filter for primary entity");
      }
      return primaryEntityFilter;
    }

    // If there is no primary entity filter, just return the data feature filter.
    if (primaryEntityFilter == null) {
      return outputEntityFilter;
    }

    // Find the relationship between this output entity and the primary entity.
    EntityGroup outputToPrimaryEntityGroup =
        underlay.getRelationship(outputEntity, underlay.getPrimaryEntity()).getLeft();

    // Build a single relationship filter per output entity that has the combined cohort
    // filter as the primary entity sub-filter.
    switch (outputToPrimaryEntityGroup.getType()) {
      case CRITERIA_OCCURRENCE:
        return new OccurrenceForPrimaryFilter(
            underlay,
            (CriteriaOccurrence) outputToPrimaryEntityGroup,
            outputEntity,
            primaryEntityFilter,
            outputEntityFilter);
      case GROUP_ITEMS:
        GroupItems groupItems = (GroupItems) outputToPrimaryEntityGroup;
        EntityFilter wrappedPrimaryEntityFilter;
        if (groupItems.getGroupEntity().isPrimary()) {
          wrappedPrimaryEntityFilter =
              new ItemInGroupFilter(underlay, groupItems, primaryEntityFilter, null, null, null);
        } else {
          wrappedPrimaryEntityFilter =
              new GroupHasItemsFilter(underlay, groupItems, primaryEntityFilter, null, null, null);
        }
        // Build a single filter per output entity with the combined cohort and data
        // feature filters.
        return outputEntityFilter == null
            ? wrappedPrimaryEntityFilter
            : new BooleanAndOrFilter(
                BooleanAndOrFilter.LogicalOperator.AND,
                List.of(outputEntityFilter, wrappedPrimaryEntityFilter));
      default:
        throw new SystemException(
            "Unsupported entity group type: " + outputToPrimaryEntityGroup.getType());
    }
  }

  public List<EntityOutput> buildOutputsForExport(
      List<Cohort> cohorts, List<ConceptSet> conceptSets) {
    return buildOutputPreviewsForExport(cohorts, conceptSets, false).stream()
        .map(EntityOutputPreview::getEntityOutput)
        .collect(Collectors.toList());
  }

  public EntityFilter buildFilterForPrimaryEntityId(
      String underlayName, String outputEntityName, Literal primaryEntityId) {
    // Find the relationship between the output and primary entities.
    Underlay underlay = underlayService.getUnderlay(underlayName);
    Entity outputEntity = underlay.getEntity(outputEntityName);
    Pair<EntityGroup, Relationship> outputToPrimary =
        underlay.getRelationship(outputEntity, underlay.getPrimaryEntity());

    // Build a filter on the output entity.
    AttributeFilter primaryIdFilter =
        new AttributeFilter(
            underlay,
            underlay.getPrimaryEntity(),
            underlay.getPrimaryEntity().getIdAttribute(),
            BinaryOperator.EQUALS,
            primaryEntityId);
    return new RelationshipFilter(
        underlay,
        outputToPrimary.getLeft(),
        outputEntity,
        outputToPrimary.getRight(),
        primaryIdFilter,
        null,
        null,
        null);
  }
}

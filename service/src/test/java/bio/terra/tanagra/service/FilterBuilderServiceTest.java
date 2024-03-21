package bio.terra.tanagra.service;

import static bio.terra.tanagra.service.criteriaconstants.sd.CohortRevision.CR_CONDITION_EXCLUDED;
import static bio.terra.tanagra.service.criteriaconstants.sd.CohortRevision.CR_CONDITION_EXCLUDED_AND_GENDER;
import static bio.terra.tanagra.service.criteriaconstants.sd.CohortRevision.CR_EMPTY;
import static bio.terra.tanagra.service.criteriaconstants.sd.CohortRevision.C_CONDITION_EXCLUDED;
import static bio.terra.tanagra.service.criteriaconstants.sd.CohortRevision.C_CONDITION_EXCLUDED_AND_GENDER;
import static bio.terra.tanagra.service.criteriaconstants.sd.CohortRevision.C_EMPTY;
import static bio.terra.tanagra.service.criteriaconstants.sd.CohortRevision.C_PROCEDURE;
import static bio.terra.tanagra.service.criteriaconstants.sd.ConceptSet.CS_CONDITION_AND_PROCEDURE;
import static bio.terra.tanagra.service.criteriaconstants.sd.ConceptSet.CS_DEMOGRAPHICS;
import static bio.terra.tanagra.service.criteriaconstants.sd.ConceptSet.CS_DEMOGRAPHICS_EXCLUDE_ID_AGE;
import static bio.terra.tanagra.service.criteriaconstants.sd.ConceptSet.CS_DEMOGRAPHICS_EXCLUDE_ID_GENDER;
import static bio.terra.tanagra.service.criteriaconstants.sd.ConceptSet.CS_EMPTY;
import static bio.terra.tanagra.service.criteriaconstants.sd.CriteriaGroup.CG_CONDITION_WITH_MODIFIER;
import static bio.terra.tanagra.service.criteriaconstants.sd.CriteriaGroup.CG_EMPTY;
import static bio.terra.tanagra.service.criteriaconstants.sd.CriteriaGroup.CG_GENDER;
import static bio.terra.tanagra.service.criteriaconstants.sd.CriteriaGroupSection.CGS_CONDITION_EXCLUDED;
import static bio.terra.tanagra.service.criteriaconstants.sd.CriteriaGroupSection.CGS_EMPTY;
import static bio.terra.tanagra.service.criteriaconstants.sd.CriteriaGroupSection.CGS_GENDER_AND_CONDITION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bio.terra.tanagra.api.filter.AttributeFilter;
import bio.terra.tanagra.api.filter.BooleanAndOrFilter;
import bio.terra.tanagra.api.filter.BooleanNotFilter;
import bio.terra.tanagra.api.filter.EntityFilter;
import bio.terra.tanagra.api.filter.HierarchyHasAncestorFilter;
import bio.terra.tanagra.api.filter.OccurrenceForPrimaryFilter;
import bio.terra.tanagra.api.filter.PrimaryWithCriteriaFilter;
import bio.terra.tanagra.api.shared.BinaryOperator;
import bio.terra.tanagra.api.shared.Literal;
import bio.terra.tanagra.app.Main;
import bio.terra.tanagra.filterbuilder.EntityOutput;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.entitymodel.Hierarchy;
import bio.terra.tanagra.underlay.entitymodel.entitygroup.CriteriaOccurrence;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = Main.class)
@SpringBootTest
@ActiveProfiles("test")
public class FilterBuilderServiceTest {
  private static final String UNDERLAY_NAME = "sd";

  @Autowired private UnderlayService underlayService;
  @Autowired private FilterBuilderService filterBuilderService;
  private Underlay underlay;

  @BeforeEach
  void lookupUnderlay() {
    underlay = underlayService.getUnderlay(UNDERLAY_NAME);
  }

  @Test
  void criteriaGroup() {
    // No criteria = null filter on primary entity.
    EntityFilter cohortFilter =
        filterBuilderService.buildFilterForCriteriaGroup(UNDERLAY_NAME, CG_EMPTY);
    assertNull(cohortFilter);

    // Single criteria = no modifiers.
    cohortFilter = filterBuilderService.buildFilterForCriteriaGroup(UNDERLAY_NAME, CG_GENDER);
    assertEquals(genderEqWomanCohortFilter(), cohortFilter);

    // Multiple criteria = with modifiers.
    cohortFilter =
        filterBuilderService.buildFilterForCriteriaGroup(UNDERLAY_NAME, CG_CONDITION_WITH_MODIFIER);
    assertEquals(conditionWithModifierCohortFilter(), cohortFilter);
  }

  @Test
  void criteriaGroupSection() {
    // No criteria groups = null filter on primary entity.
    EntityFilter cohortFilter =
        filterBuilderService.buildFilterForCriteriaGroupSection(UNDERLAY_NAME, CGS_EMPTY);
    assertNull(cohortFilter);

    // Single criteria group, isExcluded.
    cohortFilter =
        filterBuilderService.buildFilterForCriteriaGroupSection(
            UNDERLAY_NAME, CGS_CONDITION_EXCLUDED);
    assertEquals(conditionExcludedCohortFilter(), cohortFilter);

    // Multiple criteria groups, operator = AND.
    cohortFilter =
        filterBuilderService.buildFilterForCriteriaGroupSection(
            UNDERLAY_NAME, CGS_GENDER_AND_CONDITION);
    assertEquals(genderAndConditionWithModifierCohortFilter(), cohortFilter);
  }

  @Test
  void cohortRevision() {
    // Single cohort revision, no criteria group sections = null filter on primary entity.
    EntityFilter cohortFilter =
        filterBuilderService.buildFilterForCohortRevision(UNDERLAY_NAME, CR_EMPTY);
    assertNull(cohortFilter);

    // Single cohort revision, single criteria group section.
    cohortFilter =
        filterBuilderService.buildFilterForCohortRevision(UNDERLAY_NAME, CR_CONDITION_EXCLUDED);
    assertEquals(conditionExcludedCohortFilter(), cohortFilter);

    // Single cohort revision, multiple criteria group sections.
    cohortFilter =
        filterBuilderService.buildFilterForCohortRevision(
            UNDERLAY_NAME, CR_CONDITION_EXCLUDED_AND_GENDER);
    assertEquals(conditionExcludedAndGenderCohortFilter(), cohortFilter);

    // Multiple cohort revisions, empty list = null filter on primary entity.
    cohortFilter = filterBuilderService.buildFilterForCohortRevisions(UNDERLAY_NAME, List.of());
    assertNull(cohortFilter);

    // Multiple cohort revisions.
    cohortFilter =
        filterBuilderService.buildFilterForCohortRevisions(
            UNDERLAY_NAME, List.of(CR_EMPTY, CR_CONDITION_EXCLUDED_AND_GENDER));
    assertEquals(conditionExcludedAndGenderCohortFilter(), cohortFilter);
  }

  @Test
  void conceptSet() {
    // No concept sets = no entity outputs.
    List<EntityOutput> entityOutputs = filterBuilderService.buildOutputsForConceptSets(List.of());
    assertTrue(entityOutputs.isEmpty());

    // Single empty concept set, no excluded attributes.
    entityOutputs = filterBuilderService.buildOutputsForConceptSets(List.of(CS_EMPTY));
    assertTrue(entityOutputs.isEmpty());

    // Single concept set, no excluded attributes.
    entityOutputs = filterBuilderService.buildOutputsForConceptSets(List.of(CS_DEMOGRAPHICS));
    assertEquals(1, entityOutputs.size());
    EntityOutput expectedOutput = EntityOutput.unfiltered(underlay.getPrimaryEntity());
    assertEquals(expectedOutput, entityOutputs.get(0));

    // Multiple concept sets, with overlapping excluded attributes.
    entityOutputs =
        filterBuilderService.buildOutputsForConceptSets(
            List.of(
                CS_EMPTY,
                CS_DEMOGRAPHICS_EXCLUDE_ID_AGE,
                CS_DEMOGRAPHICS_EXCLUDE_ID_GENDER,
                CS_CONDITION_AND_PROCEDURE));
    assertEquals(3, entityOutputs.size());
    EntityOutput expectedOutput1 =
        EntityOutput.unfiltered(
            underlay.getPrimaryEntity(),
            underlay.getPrimaryEntity().getAttributes().stream()
                .filter(attribute -> !attribute.isId())
                .collect(Collectors.toList()));
    assertTrue(entityOutputs.contains(expectedOutput1));
    EntityOutput expectedOutput2 =
        EntityOutput.filtered(
            underlay.getEntity("conditionOccurrence"), conditionEqType2DiabetesDataFeatureFilter());
    assertTrue(entityOutputs.contains(expectedOutput2));
    EntityOutput expectedOutput3 =
        EntityOutput.filtered(
            underlay.getEntity("procedureOccurrence"), procedureEqAmputationDataFeatureFilter());
    assertTrue(entityOutputs.contains(expectedOutput3));
  }

  @Test
  void export() {
    // No cohorts or concept sets = no entity outputs.
    List<EntityOutput> entityOutputs =
        filterBuilderService.buildOutputsForExport(List.of(), List.of());
    assertTrue(entityOutputs.isEmpty());

    // One cohort, no concept sets = no entity outputs.
    entityOutputs =
        filterBuilderService.buildOutputsForExport(
            List.of(C_EMPTY, C_CONDITION_EXCLUDED), List.of());
    assertTrue(entityOutputs.isEmpty());

    // No cohorts, one concept set.
    entityOutputs =
        filterBuilderService.buildOutputsForExport(List.of(), List.of(CS_EMPTY, CS_DEMOGRAPHICS));
    assertEquals(1, entityOutputs.size());
    EntityOutput expectedOutput = EntityOutput.unfiltered(underlay.getPrimaryEntity());
    assertEquals(expectedOutput, entityOutputs.get(0));

    // Cohort with null filter, concept set with not-null filter.
    entityOutputs =
        filterBuilderService.buildOutputsForExport(
            List.of(C_EMPTY), List.of(CS_CONDITION_AND_PROCEDURE));
    assertEquals(2, entityOutputs.size());
    EntityOutput expectedOutput1 =
        EntityOutput.filtered(
            underlay.getEntity("conditionOccurrence"), conditionEqType2DiabetesDataFeatureFilter());
    EntityOutput expectedOutput2 =
        EntityOutput.filtered(
            underlay.getEntity("procedureOccurrence"), procedureEqAmputationDataFeatureFilter());
    assertTrue(entityOutputs.contains(expectedOutput1));
    assertTrue(entityOutputs.contains(expectedOutput2));

    // Both cohort and concept set with not-null filters.
    entityOutputs =
        filterBuilderService.buildOutputsForExport(
            List.of(C_PROCEDURE, C_CONDITION_EXCLUDED_AND_GENDER),
            List.of(
                CS_DEMOGRAPHICS_EXCLUDE_ID_AGE,
                CS_DEMOGRAPHICS_EXCLUDE_ID_GENDER,
                CS_CONDITION_AND_PROCEDURE));
    assertEquals(3, entityOutputs.size());
    expectedOutput1 =
        EntityOutput.filtered(
            underlay.getPrimaryEntity(),
            procCondGendCohortFilter(),
            underlay.getPrimaryEntity().getAttributes().stream()
                .filter(attribute -> !attribute.isId())
                .collect(Collectors.toList()));
    expectedOutput2 =
        EntityOutput.filtered(
            underlay.getEntity("conditionOccurrence"),
            procCondGendCohortAndConditionEqType2DiabetesDataFeatureFilter());
    EntityOutput expectedOutput3 =
        EntityOutput.filtered(
            underlay.getEntity("procedureOccurrence"),
            procCondGendCohortAndProcedureEqAmputationDataFeatureFilter());
    assertTrue(entityOutputs.contains(expectedOutput1));
    assertTrue(entityOutputs.contains(expectedOutput2));
    assertTrue(entityOutputs.contains(expectedOutput3));
  }

  private EntityFilter genderEqWomanCohortFilter() {
    return new AttributeFilter(
        underlay,
        underlay.getPrimaryEntity(),
        underlay.getPrimaryEntity().getAttribute("gender"),
        BinaryOperator.EQUALS,
        Literal.forInt64(8_532L));
  }

  private EntityFilter conditionWithModifierCohortFilter() {
    EntityFilter expectedCriteriaSubFilter =
        new HierarchyHasAncestorFilter(
            underlay,
            underlay.getEntity("condition"),
            underlay.getEntity("condition").getHierarchy(Hierarchy.DEFAULT_NAME),
            Literal.forInt64(201_826L));
    EntityFilter expectedAgeAtOccurrenceSubFilter =
        new AttributeFilter(
            underlay,
            underlay.getEntity("conditionOccurrence"),
            underlay.getEntity("conditionOccurrence").getAttribute("age_at_occurrence"),
            BinaryOperator.EQUALS,
            Literal.forInt64(65L));
    return new PrimaryWithCriteriaFilter(
        underlay,
        (CriteriaOccurrence) underlay.getEntityGroup("conditionPerson"),
        expectedCriteriaSubFilter,
        Map.of(
            underlay.getEntity("conditionOccurrence"), List.of(expectedAgeAtOccurrenceSubFilter)),
        null,
        null,
        null);
  }

  private EntityFilter conditionExcludedCohortFilter() {
    return new BooleanNotFilter(conditionWithModifierCohortFilter());
  }

  private EntityFilter genderAndConditionWithModifierCohortFilter() {
    return new BooleanAndOrFilter(
        BooleanAndOrFilter.LogicalOperator.AND,
        List.of(genderEqWomanCohortFilter(), conditionWithModifierCohortFilter()));
  }

  private EntityFilter conditionExcludedAndGenderCohortFilter() {
    return new BooleanAndOrFilter(
        BooleanAndOrFilter.LogicalOperator.AND,
        List.of(conditionExcludedCohortFilter(), genderEqWomanCohortFilter()));
  }

  private EntityFilter conditionEqType2DiabetesDataFeatureFilter() {
    EntityFilter expectedCriteriaSubFilter =
        new HierarchyHasAncestorFilter(
            underlay,
            underlay.getEntity("condition"),
            underlay.getEntity("condition").getHierarchy(Hierarchy.DEFAULT_NAME),
            Literal.forInt64(201_826L));
    return new OccurrenceForPrimaryFilter(
        underlay,
        (CriteriaOccurrence) underlay.getEntityGroup("conditionPerson"),
        underlay.getEntity("conditionOccurrence"),
        null,
        expectedCriteriaSubFilter);
  }

  private EntityFilter procedureEqAmputationDataFeatureFilter() {
    EntityFilter expectedCriteriaSubFilter =
        new HierarchyHasAncestorFilter(
            underlay,
            underlay.getEntity("procedure"),
            underlay.getEntity("procedure").getHierarchy(Hierarchy.DEFAULT_NAME),
            Literal.forInt64(234_523L));
    return new OccurrenceForPrimaryFilter(
        underlay,
        (CriteriaOccurrence) underlay.getEntityGroup("procedurePerson"),
        underlay.getEntity("procedureOccurrence"),
        null,
        expectedCriteriaSubFilter);
  }

  private EntityFilter procCondGendCohortFilter() {
    EntityFilter procedureEqAmputation =
        new HierarchyHasAncestorFilter(
            underlay,
            underlay.getEntity("procedure"),
            underlay.getEntity("procedure").getHierarchy(Hierarchy.DEFAULT_NAME),
            Literal.forInt64(234_523L));
    EntityFilter procedureCohortFilter =
        new PrimaryWithCriteriaFilter(
            underlay,
            (CriteriaOccurrence) underlay.getEntityGroup("procedurePerson"),
            procedureEqAmputation,
            null,
            null,
            null,
            null);
    return new BooleanAndOrFilter(
        BooleanAndOrFilter.LogicalOperator.OR,
        List.of(procedureCohortFilter, conditionExcludedAndGenderCohortFilter()));
  }

  private EntityFilter procCondGendCohortAndConditionEqType2DiabetesDataFeatureFilter() {
    return new OccurrenceForPrimaryFilter(
        underlay,
        (CriteriaOccurrence) underlay.getEntityGroup("icd10cmPerson"),
        underlay.getEntity("conditionOccurrence"),
        procCondGendCohortFilter(),
        conditionEqType2DiabetesDataFeatureFilter());
  }

  private EntityFilter procCondGendCohortAndProcedureEqAmputationDataFeatureFilter() {
    return new OccurrenceForPrimaryFilter(
        underlay,
        (CriteriaOccurrence) underlay.getEntityGroup("icd10cmPerson"),
        underlay.getEntity("procedureOccurrence"),
        procCondGendCohortFilter(),
        procedureEqAmputationDataFeatureFilter());
  }
}

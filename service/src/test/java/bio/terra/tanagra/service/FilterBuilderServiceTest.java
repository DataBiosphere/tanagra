package bio.terra.tanagra.service;

import static bio.terra.tanagra.service.criteriaconstants.sd.CohortRevision.CR_CONDITION_EXCLUDED;
import static bio.terra.tanagra.service.criteriaconstants.sd.CohortRevision.CR_CONDITION_EXCLUDED_AND_GENDER;
import static bio.terra.tanagra.service.criteriaconstants.sd.CohortRevision.CR_EMPTY;
import static bio.terra.tanagra.service.criteriaconstants.sd.CohortRevision.CR_GENDER_AND_DISABLED_CONDITION;
import static bio.terra.tanagra.service.criteriaconstants.sd.CohortRevision.C_CONDITION_EXCLUDED;
import static bio.terra.tanagra.service.criteriaconstants.sd.CohortRevision.C_CONDITION_EXCLUDED_AND_GENDER;
import static bio.terra.tanagra.service.criteriaconstants.sd.CohortRevision.C_EMPTY;
import static bio.terra.tanagra.service.criteriaconstants.sd.CohortRevision.C_PROCEDURE;
import static bio.terra.tanagra.service.criteriaconstants.sd.Criteria.CONDITION_EQ_TYPE_2_DIABETES;
import static bio.terra.tanagra.service.criteriaconstants.sd.Criteria.PROCEDURE_EQ_AMPUTATION;
import static bio.terra.tanagra.service.criteriaconstants.sd.CriteriaGroup.CG_CONDITION_WITH_MODIFIER;
import static bio.terra.tanagra.service.criteriaconstants.sd.CriteriaGroup.CG_EMPTY;
import static bio.terra.tanagra.service.criteriaconstants.sd.CriteriaGroup.CG_GENDER;
import static bio.terra.tanagra.service.criteriaconstants.sd.CriteriaGroup.DISABLED_CG_GENDER;
import static bio.terra.tanagra.service.criteriaconstants.sd.CriteriaGroupSection.CGS_CONDITION_AND_DISABLED_GENDER;
import static bio.terra.tanagra.service.criteriaconstants.sd.CriteriaGroupSection.CGS_CONDITION_EXCLUDED;
import static bio.terra.tanagra.service.criteriaconstants.sd.CriteriaGroupSection.CGS_EMPTY;
import static bio.terra.tanagra.service.criteriaconstants.sd.CriteriaGroupSection.CGS_GENDER_AND_CONDITION;
import static bio.terra.tanagra.service.criteriaconstants.sd.CriteriaGroupSection.CGS_NON_TEMPORAL_GROUPS_IN_SECOND_CONDITION;
import static bio.terra.tanagra.service.criteriaconstants.sd.CriteriaGroupSection.CGS_TEMPORAL_MULTIPLE_GROUPS_PER_CONDITION;
import static bio.terra.tanagra.service.criteriaconstants.sd.CriteriaGroupSection.CGS_TEMPORAL_NO_SUPPORTED_GROUPS_IN_SECOND_CONDITION;
import static bio.terra.tanagra.service.criteriaconstants.sd.CriteriaGroupSection.CGS_TEMPORAL_SINGLE_GROUP_PER_CONDITION;
import static bio.terra.tanagra.service.criteriaconstants.sd.CriteriaGroupSection.CGS_TEMPORAL_UNSUPPORTED_CRITERIA_SELECTOR;
import static bio.terra.tanagra.service.criteriaconstants.sd.CriteriaGroupSection.CGS_TEMPORAL_UNSUPPORTED_MODIFIER;
import static bio.terra.tanagra.service.criteriaconstants.sd.CriteriaGroupSection.DISABLED_CGS_CONDITION;
import static bio.terra.tanagra.service.criteriaconstants.sd.FeatureSet.CS_CONDITION_AND_PROCEDURE;
import static bio.terra.tanagra.service.criteriaconstants.sd.FeatureSet.CS_DEMOGRAPHICS;
import static bio.terra.tanagra.service.criteriaconstants.sd.FeatureSet.CS_DEMOGRAPHICS_EXCLUDE_ID_AGE;
import static bio.terra.tanagra.service.criteriaconstants.sd.FeatureSet.CS_DEMOGRAPHICS_EXCLUDE_ID_GENDER;
import static bio.terra.tanagra.service.criteriaconstants.sd.FeatureSet.CS_EMPTY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bio.terra.tanagra.api.filter.*;
import bio.terra.tanagra.api.shared.*;
import bio.terra.tanagra.app.Main;
import bio.terra.tanagra.exception.*;
import bio.terra.tanagra.filterbuilder.EntityOutput;
import bio.terra.tanagra.service.artifact.model.Criteria;
import bio.terra.tanagra.service.artifact.model.FeatureSet;
import bio.terra.tanagra.service.filter.EntityOutputPreview;
import bio.terra.tanagra.service.filter.FilterBuilderService;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.entitymodel.Hierarchy;
import bio.terra.tanagra.underlay.entitymodel.entitygroup.CriteriaOccurrence;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;
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
        filterBuilderService.buildCohortFilterForCriteriaGroup(UNDERLAY_NAME, CG_EMPTY);
    assertNull(cohortFilter);

    // Single criteria = no modifiers.
    cohortFilter = filterBuilderService.buildCohortFilterForCriteriaGroup(UNDERLAY_NAME, CG_GENDER);
    assertEquals(genderEqWomanCohortFilter(), cohortFilter);

    // Multiple criteria = with modifiers.
    cohortFilter =
        filterBuilderService.buildCohortFilterForCriteriaGroup(
            UNDERLAY_NAME, CG_CONDITION_WITH_MODIFIER);
    assertEquals(conditionWithModifierCohortFilter(), cohortFilter);

    // Disabled group = null filter.
    cohortFilter =
        filterBuilderService.buildCohortFilterForCriteriaGroup(UNDERLAY_NAME, DISABLED_CG_GENDER);
    assertNull(cohortFilter);
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

    // Temporal section, single criteria group in each condition.
    cohortFilter =
        filterBuilderService.buildFilterForCriteriaGroupSection(
            UNDERLAY_NAME, CGS_TEMPORAL_SINGLE_GROUP_PER_CONDITION);
    assertEquals(temporalSingleGroupPerCondition(), cohortFilter);

    // Temporal section, multiple criteria groups in each condition.
    cohortFilter =
        filterBuilderService.buildFilterForCriteriaGroupSection(
            UNDERLAY_NAME, CGS_TEMPORAL_MULTIPLE_GROUPS_PER_CONDITION);
    assertEquals(temporalMultipleGroupsPerCondition(), cohortFilter);

    // Temporal section, criteria selectors that don't support temporal queries.
    cohortFilter =
        filterBuilderService.buildFilterForCriteriaGroupSection(
            UNDERLAY_NAME, CGS_TEMPORAL_UNSUPPORTED_CRITERIA_SELECTOR);
    assertEquals(temporalSingleGroupPerCondition(), cohortFilter);

    // Temporal section, modifiers that don't support temporal queries.
    cohortFilter =
        filterBuilderService.buildFilterForCriteriaGroupSection(
            UNDERLAY_NAME, CGS_TEMPORAL_UNSUPPORTED_MODIFIER);
    assertEquals(temporalSingleGroupPerCondition(), cohortFilter);

    // Temporal section, no supported criteria groups in second condition.
    assertThrows(
        InvalidQueryException.class,
        () ->
            filterBuilderService.buildFilterForCriteriaGroupSection(
                UNDERLAY_NAME, CGS_TEMPORAL_NO_SUPPORTED_GROUPS_IN_SECOND_CONDITION));

    // Non-temporal section, criteria groups in both temporal conditions.
    cohortFilter =
        filterBuilderService.buildFilterForCriteriaGroupSection(
            UNDERLAY_NAME, CGS_NON_TEMPORAL_GROUPS_IN_SECOND_CONDITION);
    assertEquals(genderAndConditionWithModifierCohortFilter(), cohortFilter);

    // Disabled section = null filter.
    cohortFilter =
        filterBuilderService.buildFilterForCriteriaGroupSection(
            UNDERLAY_NAME, DISABLED_CGS_CONDITION);
    assertNull(cohortFilter);

    // Enabled section with one disabled group and one enabled group = filter for only enabled
    // group.
    cohortFilter =
        filterBuilderService.buildFilterForCriteriaGroupSection(
            UNDERLAY_NAME, CGS_CONDITION_AND_DISABLED_GENDER);
    assertEquals(conditionWithModifierCohortFilter(), cohortFilter);
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

    // One disabled section and one enabled section = filter for only enabled section.
    cohortFilter =
        filterBuilderService.buildFilterForCohortRevision(
            UNDERLAY_NAME, CR_GENDER_AND_DISABLED_CONDITION);
    assertEquals(genderEqWomanCohortFilter(), cohortFilter);
  }

  @Test
  void featureSet() {
    // No feature sets = no entity outputs.
    List<EntityOutputPreview> entityOutputs =
        filterBuilderService.buildOutputPreviewsForFeatureSets(List.of(), false);
    assertTrue(entityOutputs.isEmpty());

    // Single empty feature set, no excluded attributes.
    entityOutputs =
        filterBuilderService.buildOutputPreviewsForFeatureSets(List.of(CS_EMPTY), false);
    assertTrue(entityOutputs.isEmpty());

    // Single feature set, no excluded attributes.
    entityOutputs =
        filterBuilderService.buildOutputPreviewsForFeatureSets(List.of(CS_DEMOGRAPHICS), false);
    assertEquals(1, entityOutputs.size());
    EntityOutput expectedOutput =
        EntityOutput.unfiltered(
            underlay.getPrimaryEntity(),
            underlay.getPrimaryEntity().getAttributes().stream()
                .filter(attribute -> !attribute.isSuppressedForExport())
                .collect(Collectors.toList()));
    assertEquals(expectedOutput, entityOutputs.get(0).getEntityOutput());
    List<Pair<FeatureSet, Criteria>> expectedAttributedCriteria = new ArrayList<>();
    CS_DEMOGRAPHICS
        .getCriteria()
        .forEach(criteria -> expectedAttributedCriteria.add(Pair.of(CS_DEMOGRAPHICS, criteria)));
    assertEquals(expectedAttributedCriteria, entityOutputs.get(0).getAttributedCriteria());

    // Single feature set, excluded attributes but with override flag to include all attributes.
    entityOutputs =
        filterBuilderService.buildOutputPreviewsForFeatureSets(
            List.of(CS_DEMOGRAPHICS_EXCLUDE_ID_AGE), true);
    assertEquals(1, entityOutputs.size());
    expectedOutput =
        EntityOutput.unfiltered(
            underlay.getPrimaryEntity(),
            underlay.getPrimaryEntity().getAttributes().stream()
                .filter(attribute -> !attribute.isSuppressedForExport())
                .collect(Collectors.toList()));
    assertEquals(expectedOutput, entityOutputs.get(0).getEntityOutput());

    // Multiple feature sets, with overlapping excluded attributes.
    entityOutputs =
        filterBuilderService.buildOutputPreviewsForFeatureSets(
            List.of(
                CS_EMPTY,
                CS_DEMOGRAPHICS_EXCLUDE_ID_AGE,
                CS_DEMOGRAPHICS_EXCLUDE_ID_GENDER,
                CS_CONDITION_AND_PROCEDURE),
            false);
    assertEquals(3, entityOutputs.size());

    EntityOutput expectedOutput1 =
        EntityOutput.unfiltered(
            underlay.getPrimaryEntity(),
            underlay.getPrimaryEntity().getAttributes().stream()
                .filter(attribute -> !attribute.isId() && !attribute.isSuppressedForExport())
                .collect(Collectors.toList()));
    Optional<EntityOutputPreview> entityOutputAndAttributedCriteria1 =
        entityOutputs.stream()
            .filter(
                entityOutputPreview ->
                    entityOutputPreview.getEntityOutput().equals(expectedOutput1))
            .findAny();
    assertTrue(entityOutputAndAttributedCriteria1.isPresent());
    List<Pair<FeatureSet, Criteria>> expectedAttributedCriteria1 = new ArrayList<>();
    CS_DEMOGRAPHICS_EXCLUDE_ID_AGE
        .getCriteria()
        .forEach(
            criteria ->
                expectedAttributedCriteria1.add(Pair.of(CS_DEMOGRAPHICS_EXCLUDE_ID_AGE, criteria)));
    CS_DEMOGRAPHICS_EXCLUDE_ID_GENDER
        .getCriteria()
        .forEach(
            criteria ->
                expectedAttributedCriteria1.add(
                    Pair.of(CS_DEMOGRAPHICS_EXCLUDE_ID_GENDER, criteria)));
    assertEquals(
        expectedAttributedCriteria1,
        entityOutputAndAttributedCriteria1.get().getAttributedCriteria());

    EntityOutput expectedOutput2 =
        EntityOutput.filtered(
            underlay.getEntity("conditionOccurrence"),
            conditionEqType2DiabetesDataFeatureFilter(),
            underlay.getEntity("conditionOccurrence").getAttributes().stream()
                .filter(attribute -> !attribute.isSuppressedForExport())
                .collect(Collectors.toList()));
    Optional<EntityOutputPreview> entityOutputAndAttributedCriteria2 =
        entityOutputs.stream()
            .filter(
                entityOutputPreview ->
                    entityOutputPreview.getEntityOutput().equals(expectedOutput2))
            .findAny();
    assertTrue(entityOutputAndAttributedCriteria2.isPresent());
    List<Pair<FeatureSet, Criteria>> expectedAttributedCriteria2 =
        List.of(Pair.of(CS_CONDITION_AND_PROCEDURE, CONDITION_EQ_TYPE_2_DIABETES));
    assertEquals(
        expectedAttributedCriteria2,
        entityOutputAndAttributedCriteria2.get().getAttributedCriteria());

    EntityOutput expectedOutput3 =
        EntityOutput.filtered(
            underlay.getEntity("procedureOccurrence"),
            procedureEqAmputationDataFeatureFilter(),
            underlay.getEntity("procedureOccurrence").getAttributes().stream()
                .filter(attribute -> !attribute.isSuppressedForExport())
                .collect(Collectors.toList()));
    Optional<EntityOutputPreview> entityOutputAndAttributedCriteria3 =
        entityOutputs.stream()
            .filter(
                entityOutputPreview ->
                    entityOutputPreview.getEntityOutput().equals(expectedOutput3))
            .findAny();
    assertTrue(entityOutputAndAttributedCriteria3.isPresent());
    List<Pair<FeatureSet, Criteria>> expectedAttributedCriteria3 =
        List.of(Pair.of(CS_CONDITION_AND_PROCEDURE, PROCEDURE_EQ_AMPUTATION));
    assertEquals(
        expectedAttributedCriteria3,
        entityOutputAndAttributedCriteria3.get().getAttributedCriteria());
  }

  @Test
  @SuppressWarnings("PMD.UnnecessaryFullyQualifiedName")
  void suppressedAttribute() {
    Underlay cmssynpuf = underlayService.getUnderlay("cmssynpuf");

    // One suppressed attribute, includeAllAttributes=true for data feature set page.
    List<EntityOutputPreview> entityOutputsForDataFeatureSetPage =
        filterBuilderService.buildOutputPreviewsForFeatureSets(
            List.of(
                bio.terra.tanagra.service.criteriaconstants.cmssynpuf.FeatureSet.CS_DEMOGRAPHICS),
            true);
    assertEquals(1, entityOutputsForDataFeatureSetPage.size());
    assertEquals(
        cmssynpuf.getPrimaryEntity().getAttributes().size() - 1,
        entityOutputsForDataFeatureSetPage.get(0).getEntityOutput().getAttributes().size());
    EntityOutput expectedOutput =
        EntityOutput.unfiltered(
            cmssynpuf.getPrimaryEntity(),
            cmssynpuf.getPrimaryEntity().getAttributes().stream()
                .filter(attribute -> !attribute.isSuppressedForExport())
                .collect(Collectors.toList()));
    assertEquals(expectedOutput, entityOutputsForDataFeatureSetPage.get(0).getEntityOutput());

    // One suppressed attribute, includeAllAttributes=false for export page.
    List<EntityOutputPreview> entityOutputsForExportPage =
        filterBuilderService.buildOutputPreviewsForFeatureSets(
            List.of(
                bio.terra.tanagra.service.criteriaconstants.cmssynpuf.FeatureSet.CS_DEMOGRAPHICS),
            false);
    assertEquals(1, entityOutputsForExportPage.size());
    assertEquals(
        cmssynpuf.getPrimaryEntity().getAttributes().size() - 1,
        entityOutputsForExportPage.get(0).getEntityOutput().getAttributes().size());
    assertEquals(expectedOutput, entityOutputsForExportPage.get(0).getEntityOutput());
  }

  @Test
  void export() {
    // No cohorts or feature sets = no entity outputs.
    List<EntityOutput> entityOutputs =
        filterBuilderService.buildOutputsForExport(List.of(), List.of());
    assertTrue(entityOutputs.isEmpty());

    // One cohort, no feature sets = no entity outputs.
    entityOutputs =
        filterBuilderService.buildOutputsForExport(
            List.of(C_EMPTY, C_CONDITION_EXCLUDED), List.of());
    assertTrue(entityOutputs.isEmpty());

    // No cohorts, one feature set.
    entityOutputs =
        filterBuilderService.buildOutputsForExport(List.of(), List.of(CS_EMPTY, CS_DEMOGRAPHICS));
    assertEquals(1, entityOutputs.size());
    EntityOutput expectedOutput =
        EntityOutput.unfiltered(
            underlay.getPrimaryEntity(),
            underlay.getPrimaryEntity().getAttributes().stream()
                .filter(attribute -> !attribute.isSuppressedForExport())
                .collect(Collectors.toList()));
    assertEquals(expectedOutput, entityOutputs.get(0));

    // Cohort with null filter, feature set with not-null filter.
    entityOutputs =
        filterBuilderService.buildOutputsForExport(
            List.of(C_EMPTY), List.of(CS_CONDITION_AND_PROCEDURE));
    assertEquals(2, entityOutputs.size());
    EntityOutput expectedOutput1 =
        EntityOutput.filtered(
            underlay.getEntity("conditionOccurrence"),
            conditionEqType2DiabetesDataFeatureFilter(),
            underlay.getEntity("conditionOccurrence").getAttributes().stream()
                .filter(attribute -> !attribute.isSuppressedForExport())
                .collect(Collectors.toList()));
    EntityOutput expectedOutput2 =
        EntityOutput.filtered(
            underlay.getEntity("procedureOccurrence"),
            procedureEqAmputationDataFeatureFilter(),
            underlay.getEntity("procedureOccurrence").getAttributes().stream()
                .filter(attribute -> !attribute.isSuppressedForExport())
                .collect(Collectors.toList()));
    assertTrue(entityOutputs.contains(expectedOutput1));
    assertTrue(entityOutputs.contains(expectedOutput2));

    // Both cohort and feature set with not-null filters.
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
                .filter(attribute -> !attribute.isId() && !attribute.isSuppressedForExport())
                .collect(Collectors.toList()));
    expectedOutput2 =
        EntityOutput.filtered(
            underlay.getEntity("conditionOccurrence"),
            procCondGendCohortAndConditionEqType2DiabetesDataFeatureFilter(),
            underlay.getEntity("conditionOccurrence").getAttributes().stream()
                .filter(attribute -> !attribute.isSuppressedForExport())
                .collect(Collectors.toList()));
    EntityOutput expectedOutput3 =
        EntityOutput.filtered(
            underlay.getEntity("procedureOccurrence"),
            procCondGendCohortAndProcedureEqAmputationDataFeatureFilter(),
            underlay.getEntity("procedureOccurrence").getAttributes().stream()
                .filter(attribute -> !attribute.isSuppressedForExport())
                .collect(Collectors.toList()));
    assertTrue(entityOutputs.contains(expectedOutput1));
    assertTrue(entityOutputs.contains(expectedOutput2));
    assertTrue(entityOutputs.contains(expectedOutput3));
  }

  @Test
  void primaryEntityId() {
    EntityFilter filterOnPrimaryEntityId =
        filterBuilderService.buildFilterForPrimaryEntityId(
            UNDERLAY_NAME, "conditionOccurrence", Literal.forInt64(123L));
    assertNotNull(filterOnPrimaryEntityId);

    AttributeFilter expectedPrimaryEntitySubFilter =
        new AttributeFilter(
            underlay,
            underlay.getPrimaryEntity(),
            underlay.getPrimaryEntity().getIdAttribute(),
            BinaryOperator.EQUALS,
            Literal.forInt64(123L));
    RelationshipFilter expectedOutputEntityFilter =
        new RelationshipFilter(
            underlay,
            underlay.getEntityGroup("icd10cmPerson"),
            underlay.getEntity("conditionOccurrence"),
            ((CriteriaOccurrence) underlay.getEntityGroup("icd10cmPerson"))
                .getOccurrencePrimaryRelationship("conditionOccurrence"),
            expectedPrimaryEntitySubFilter,
            null,
            null,
            null);
    assertEquals(expectedOutputEntityFilter, filterOnPrimaryEntityId);
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

  private EntityFilter temporalSingleGroupPerCondition() {
    List<EntityOutput> firstCondition =
        List.of(
            EntityOutput.filtered(
                underlay.getEntity("conditionOccurrence"),
                conditionEqType2DiabetesDataFeatureFilter()));
    List<EntityOutput> secondCondition =
        List.of(
            EntityOutput.filtered(
                underlay.getEntity("procedureOccurrence"),
                procedureEqAmputationDataFeatureFilter()));
    return new TemporalPrimaryFilter(
        underlay,
        ReducingOperator.FIRST_MENTION_OF,
        firstCondition,
        JoinOperator.NUM_DAYS_BEFORE,
        10,
        null,
        secondCondition);
  }

  private EntityFilter temporalMultipleGroupsPerCondition() {
    List<EntityOutput> firstCondition =
        List.of(
            EntityOutput.filtered(
                underlay.getEntity("conditionOccurrence"),
                conditionEqType2DiabetesDataFeatureFilter()),
            EntityOutput.filtered(
                underlay.getEntity("procedureOccurrence"),
                procedureEqAmputationAgeAtOccurrenceEq45DataFeatureFilter()));
    List<EntityOutput> secondCondition =
        List.of(
            EntityOutput.filtered(
                underlay.getEntity("procedureOccurrence"),
                procedureEqAmputationDataFeatureFilter()),
            EntityOutput.filtered(
                underlay.getEntity("conditionOccurrence"),
                conditionEqType2DiabetesAgeAtOccurrenceEq65DataFeatureFilter()));
    return new TemporalPrimaryFilter(
        underlay,
        ReducingOperator.LAST_MENTION_OF,
        firstCondition,
        JoinOperator.DURING_SAME_ENCOUNTER,
        null,
        ReducingOperator.FIRST_MENTION_OF,
        secondCondition);
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

  private EntityFilter conditionEqType2DiabetesAgeAtOccurrenceEq65DataFeatureFilter() {
    EntityFilter expectedAgeAtOccurrenceSubFilter =
        new AttributeFilter(
            underlay,
            underlay.getEntity("conditionOccurrence"),
            underlay.getEntity("conditionOccurrence").getAttribute("age_at_occurrence"),
            BinaryOperator.EQUALS,
            Literal.forInt64(65L));
    return new BooleanAndOrFilter(
        BooleanAndOrFilter.LogicalOperator.AND,
        List.of(conditionEqType2DiabetesDataFeatureFilter(), expectedAgeAtOccurrenceSubFilter));
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

  private EntityFilter procedureEqAmputationAgeAtOccurrenceEq45DataFeatureFilter() {
    EntityFilter expectedAgeAtOccurrenceSubFilter =
        new AttributeFilter(
            underlay,
            underlay.getEntity("procedureOccurrence"),
            underlay.getEntity("procedureOccurrence").getAttribute("age_at_occurrence"),
            BinaryOperator.EQUALS,
            Literal.forInt64(45L));
    return new BooleanAndOrFilter(
        BooleanAndOrFilter.LogicalOperator.AND,
        List.of(procedureEqAmputationDataFeatureFilter(), expectedAgeAtOccurrenceSubFilter));
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

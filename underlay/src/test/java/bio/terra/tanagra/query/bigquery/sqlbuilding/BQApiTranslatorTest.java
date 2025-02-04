package bio.terra.tanagra.query.bigquery.sqlbuilding;

import static bio.terra.tanagra.UnderlayTestConfigs.CMSSYNPUF;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bio.terra.tanagra.api.filter.AttributeFilter;
import bio.terra.tanagra.api.filter.BooleanAndOrFilter.LogicalOperator;
import bio.terra.tanagra.api.filter.BooleanNotFilter;
import bio.terra.tanagra.api.filter.EntityFilter;
import bio.terra.tanagra.api.filter.HierarchyHasAncestorFilter;
import bio.terra.tanagra.api.filter.HierarchyHasParentFilter;
import bio.terra.tanagra.api.filter.HierarchyIsLeafFilter;
import bio.terra.tanagra.api.filter.HierarchyIsMemberFilter;
import bio.terra.tanagra.api.filter.HierarchyIsRootFilter;
import bio.terra.tanagra.api.filter.PrimaryWithCriteriaFilter;
import bio.terra.tanagra.api.filter.TextSearchFilter;
import bio.terra.tanagra.api.filter.TextSearchFilter.TextSearchOperator;
import bio.terra.tanagra.api.shared.BinaryOperator;
import bio.terra.tanagra.api.shared.Literal;
import bio.terra.tanagra.query.bigquery.translator.BQApiTranslator;
import bio.terra.tanagra.query.bigquery.translator.filter.BQAttributeFilterTranslator;
import bio.terra.tanagra.underlay.ConfigReader;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.entitymodel.Attribute;
import bio.terra.tanagra.underlay.entitymodel.Entity;
import bio.terra.tanagra.underlay.entitymodel.Hierarchy;
import bio.terra.tanagra.underlay.entitymodel.entitygroup.CriteriaOccurrence;
import bio.terra.tanagra.underlay.serialization.SZService;
import bio.terra.tanagra.underlay.serialization.SZUnderlay;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class BQApiTranslatorTest {
  private static Underlay underlay;
  private static Entity primaryEntity;
  private static Entity conditionEntity;
  private final BQApiTranslator bqApiTranslator = new BQApiTranslator();

  @BeforeAll
  static void setup() {
    ConfigReader configReader = ConfigReader.fromJarResources();
    SZService szService = configReader.readService(CMSSYNPUF.fileName());
    SZUnderlay szUnderlay = configReader.readUnderlay(szService.underlay);
    underlay = Underlay.fromConfig(szService.bigQuery, szUnderlay, configReader);
    primaryEntity = underlay.getPrimaryEntity();

    Entity originalEntity = underlay.getEntity("condition");
    conditionEntity =
        new Entity(
            originalEntity.getName(),
            originalEntity.getDisplayName(),
            originalEntity.getDescription(),
            originalEntity.isPrimary(),
            originalEntity.getAttributes(),
            List.of(
                originalEntity.getHierarchies().get(0),
                new Hierarchy("hierarchy2", 1, false, null, true)),
            originalEntity.getOptimizeGroupByAttributes(),
            List.of(),
            originalEntity.hasTextSearch(),
            originalEntity.getOptimizeTextSearchAttributes(),
            originalEntity.getSourceQueryTableName());
  }

  @Test
  void listSizeAndEntityTests() {
    // cannotMergeSingleFilter
    EntityFilter filter1 =
        new AttributeFilter(
            underlay,
            primaryEntity,
            primaryEntity.getIdAttribute(),
            BinaryOperator.EQUALS,
            Literal.forInt64(11L));
    assertTrue(bqApiTranslator.mergedTranslator(List.of(filter1), null, null).isEmpty());

    // cannotMergeWithDifferentEntity
    EntityFilter filter2 =
        new AttributeFilter(
            underlay,
            conditionEntity,
            conditionEntity.getIdAttribute(),
            BinaryOperator.EQUALS,
            Literal.forInt64(23L));
    assertFalse(
        bqApiTranslator.mergedTranslator(List.of(filter1, filter2), null, null).isPresent());
  }

  @Test
  void attributeFilterTest() {
    Entity entity =
        new Entity(
            primaryEntity.getName(),
            primaryEntity.getDisplayName(),
            primaryEntity.getDescription(),
            primaryEntity.isPrimary(),
            primaryEntity.getAttributes(),
            primaryEntity.getHierarchies(),
            primaryEntity.getOptimizeGroupByAttributes(),
            List.of(
                List.of(
                    primaryEntity.getAttribute("ethnicity"), primaryEntity.getAttribute("gender")),
                List.of(primaryEntity.getAttribute("year_of_birth"))),
            primaryEntity.hasTextSearch(),
            primaryEntity.getOptimizeTextSearchAttributes(),
            primaryEntity.getSourceQueryTableName());

    // cannotMergeWithNotAllAttrOptTogether
    AttributeFilter filter1 =
        new AttributeFilter(
            underlay,
            entity,
            entity.getAttribute("ethnicity"),
            BinaryOperator.EQUALS,
            Literal.forInt64(12L));
    assertTrue(
        BQAttributeFilterTranslator.mergedTranslator(bqApiTranslator, List.of(filter1), null, null)
            .isPresent());

    AttributeFilter filter2 =
        new AttributeFilter(
            underlay,
            entity,
            entity.getAttribute("year_of_birth"),
            BinaryOperator.EQUALS,
            Literal.forInt64(14L));
    assertFalse(
        BQAttributeFilterTranslator.mergedTranslator(
                bqApiTranslator, List.of(filter1, filter2), null, null)
            .isPresent());

    AttributeFilter filter3 =
        new AttributeFilter(
            underlay,
            entity,
            entity.getAttribute("gender"),
            BinaryOperator.EQUALS,
            Literal.forInt64(13L));
    assertFalse(
        BQAttributeFilterTranslator.mergedTranslator(
                bqApiTranslator, List.of(filter1, filter2, filter3), null, null)
            .isPresent());

    // canMergeWithAllAttrOptTogether
    assertTrue(
        BQAttributeFilterTranslator.mergedTranslator(
                bqApiTranslator, List.of(filter1, filter3), LogicalOperator.OR, null)
            .isPresent());
    assertTrue(
        BQAttributeFilterTranslator.mergedTranslator(bqApiTranslator, List.of(filter2), null, null)
            .isPresent());
  }

  @Test
  void hierarchyHasAncestorFilterTest() {
    // cannotMergeWithAndOperator
    HierarchyHasAncestorFilter filter1a =
        new HierarchyHasAncestorFilter(
            underlay,
            conditionEntity,
            conditionEntity.getHierarchies().get(0),
            Literal.forInt64(201_826L));
    HierarchyHasAncestorFilter filter1b =
        new HierarchyHasAncestorFilter(
            underlay,
            conditionEntity,
            conditionEntity.getHierarchies().get(0),
            Literal.forInt64(10_826L));
    assertFalse(
        bqApiTranslator
            .mergedTranslator(List.of(filter1a, filter1b), LogicalOperator.AND, null)
            .isPresent());

    // cannotMergeWithDifferentHierarchy
    HierarchyHasAncestorFilter filter2 =
        new HierarchyHasAncestorFilter(
            underlay,
            conditionEntity,
            conditionEntity.getHierarchies().get(1),
            Literal.forInt64(10_826L));
    assertFalse(
        bqApiTranslator
            .mergedTranslator(List.of(filter1a, filter2), LogicalOperator.OR, null)
            .isPresent());

    // canMergeWithSameHierarchy
    assertTrue(
        bqApiTranslator
            .mergedTranslator(List.of(filter1a, filter1b), LogicalOperator.OR, null)
            .isPresent());
  }

  @Test
  void hierarchyHasParentFilterTest() {
    // cannotMergeWithAndOperator
    HierarchyHasParentFilter filter1a =
        new HierarchyHasParentFilter(
            underlay,
            conditionEntity,
            conditionEntity.getHierarchies().get(0),
            Literal.forInt64(201_826L));
    HierarchyHasParentFilter filter1b =
        new HierarchyHasParentFilter(
            underlay,
            conditionEntity,
            conditionEntity.getHierarchies().get(0),
            Literal.forInt64(10_826L));
    assertFalse(
        bqApiTranslator
            .mergedTranslator(List.of(filter1a, filter1b), LogicalOperator.AND, null)
            .isPresent());

    // cannotMergeWithDifferentHierarchy
    HierarchyHasParentFilter filter2 =
        new HierarchyHasParentFilter(
            underlay,
            conditionEntity,
            conditionEntity.getHierarchies().get(1),
            Literal.forInt64(10_826L));
    assertFalse(
        bqApiTranslator
            .mergedTranslator(List.of(filter1a, filter2), LogicalOperator.OR, null)
            .isPresent());

    // canMergeWithSameHierarchy
    assertTrue(
        bqApiTranslator
            .mergedTranslator(List.of(filter1a, filter1b), LogicalOperator.OR, null)
            .isPresent());
  }

  @Test
  void hierarchyIsLeafFilterTest() {
    // cannotMergeWithAndOperator
    HierarchyIsLeafFilter filter1 =
        new HierarchyIsLeafFilter(
            underlay, conditionEntity, conditionEntity.getHierarchies().get(0));
    HierarchyIsLeafFilter filter2 =
        new HierarchyIsLeafFilter(
            underlay, conditionEntity, conditionEntity.getHierarchies().get(1));
    assertFalse(
        bqApiTranslator
            .mergedTranslator(List.of(filter1, filter2), LogicalOperator.AND, null)
            .isPresent());

    // cannotMergeWithDifferentHierarchy
    assertFalse(
        bqApiTranslator
            .mergedTranslator(List.of(filter1, filter2), LogicalOperator.OR, null)
            .isPresent());

    // canMergeWithSameHierarchy
    assertTrue(
        bqApiTranslator
            .mergedTranslator(List.of(filter1, filter1), LogicalOperator.OR, null)
            .isPresent());
  }

  @Test
  void hierarchyIsMemberFilterTest() {
    // cannotMergeWithAndOperator
    HierarchyIsMemberFilter filter1 =
        new HierarchyIsMemberFilter(
            underlay, conditionEntity, conditionEntity.getHierarchies().get(0));
    HierarchyIsMemberFilter filter2 =
        new HierarchyIsMemberFilter(
            underlay, conditionEntity, conditionEntity.getHierarchies().get(1));
    assertFalse(
        bqApiTranslator
            .mergedTranslator(List.of(filter1, filter2), LogicalOperator.AND, null)
            .isPresent());

    // cannotMergeWithDifferentHierarchy
    assertFalse(
        bqApiTranslator
            .mergedTranslator(List.of(filter1, filter2), LogicalOperator.OR, null)
            .isPresent());

    // canMergeWithSameHierarchy
    assertTrue(
        bqApiTranslator
            .mergedTranslator(List.of(filter1, filter1), LogicalOperator.OR, null)
            .isPresent());
  }

  @Test
  void hierarchyIsRootFilterTest() {
    // cannotMergeWithAndOperator
    HierarchyIsRootFilter filter1 =
        new HierarchyIsRootFilter(
            underlay, conditionEntity, conditionEntity.getHierarchies().get(0));
    HierarchyIsRootFilter filter2 =
        new HierarchyIsRootFilter(
            underlay, conditionEntity, conditionEntity.getHierarchies().get(1));
    assertFalse(
        bqApiTranslator
            .mergedTranslator(List.of(filter1, filter2), LogicalOperator.AND, null)
            .isPresent());

    // cannotMergeWithDifferentHierarchy
    assertFalse(
        bqApiTranslator
            .mergedTranslator(List.of(filter1, filter2), LogicalOperator.OR, null)
            .isPresent());

    // canMergeWithSameHierarchy
    assertTrue(
        bqApiTranslator
            .mergedTranslator(List.of(filter1, filter1), LogicalOperator.OR, null)
            .isPresent());
  }

  @Test
  void primaryWithCriteriaFilterTest() {
    // cannotMergeWithGroupByOnFirstFilter
    Entity occurrenceEntity1 = underlay.getEntity("conditionOccurrence");
    CriteriaOccurrence criteriaOccurrence1 =
        (CriteriaOccurrence) underlay.getEntityGroup("conditionPerson");
    TextSearchFilter subFilterOccurrenceEntity1a =
        new TextSearchFilter(
            underlay, occurrenceEntity1, TextSearchOperator.EXACT_MATCH, "text1a", null);
    Map<Entity, List<EntityFilter>> subFiltersPerOccurrenceEntity1 =
        Map.of(occurrenceEntity1, List.of(subFilterOccurrenceEntity1a));
    Map<Entity, List<Attribute>> groupByAttributesPerOccurrenceEntity1 =
        Map.of(occurrenceEntity1, List.of(occurrenceEntity1.getIdAttribute()));
    HierarchyIsRootFilter criteriaSubFilter1 =
        new HierarchyIsRootFilter(underlay, primaryEntity, null);
    PrimaryWithCriteriaFilter filter1a =
        new PrimaryWithCriteriaFilter(
            underlay,
            criteriaOccurrence1,
            criteriaSubFilter1,
            subFiltersPerOccurrenceEntity1,
            groupByAttributesPerOccurrenceEntity1,
            BinaryOperator.GREATER_THAN,
            2);
    PrimaryWithCriteriaFilter filter1b =
        new PrimaryWithCriteriaFilter(
            underlay,
            criteriaOccurrence1,
            criteriaSubFilter1,
            subFiltersPerOccurrenceEntity1,
            groupByAttributesPerOccurrenceEntity1,
            null,
            null);
    assertFalse(
        bqApiTranslator
            .mergedTranslator(List.of(filter1a, filter1b), LogicalOperator.OR, null)
            .isPresent());

    // cannotMergeWithGroupByOnOtherFilter
    assertFalse(
        bqApiTranslator
            .mergedTranslator(List.of(filter1b, filter1a), LogicalOperator.OR, null)
            .isPresent());

    // cannotMergeWithDifferentCriteriaOccurrence
    Entity occurrenceEntity2 = underlay.getEntity("deviceOccurrence");
    CriteriaOccurrence criteriaOccurrence2 =
        (CriteriaOccurrence) underlay.getEntityGroup("devicePerson");
    PrimaryWithCriteriaFilter filter2a =
        new PrimaryWithCriteriaFilter(
            underlay,
            criteriaOccurrence2,
            criteriaSubFilter1,
            Map.of(
                occurrenceEntity2,
                List.of(
                    new TextSearchFilter(
                        underlay,
                        occurrenceEntity2,
                        TextSearchOperator.EXACT_MATCH,
                        "text2",
                        null))),
            Map.of(occurrenceEntity2, List.of(occurrenceEntity2.getIdAttribute())),
            null,
            null);
    assertFalse(
        bqApiTranslator
            .mergedTranslator(List.of(filter1b, filter2a), LogicalOperator.OR, null)
            .isPresent());

    // cannotMergeWithDifferentSubFiltersPerOccurrenceEntity
    PrimaryWithCriteriaFilter filter1c =
        new PrimaryWithCriteriaFilter(
            underlay,
            criteriaOccurrence1,
            criteriaSubFilter1,
            Map.of(occurrenceEntity1, List.of(new BooleanNotFilter(subFilterOccurrenceEntity1a))),
            groupByAttributesPerOccurrenceEntity1,
            null,
            null);
    assertFalse(
        bqApiTranslator
            .mergedTranslator(List.of(filter1b, filter1c), LogicalOperator.OR, null)
            .isPresent());

    // cannotMergeWithDifferentGroupByAttributesPerOccurrenceEntity
    PrimaryWithCriteriaFilter filter1d =
        new PrimaryWithCriteriaFilter(
            underlay,
            criteriaOccurrence1,
            criteriaSubFilter1,
            subFiltersPerOccurrenceEntity1,
            Map.of(occurrenceEntity1, List.of(occurrenceEntity1.getAttribute("age_at_occurrence"))),
            null,
            null);
    assertFalse(
        bqApiTranslator
            .mergedTranslator(List.of(filter1b, filter1d), LogicalOperator.OR, null)
            .isPresent());

    // cannotMergeWithUnMergeableCriteriaSubFilters
    PrimaryWithCriteriaFilter filter1e =
        new PrimaryWithCriteriaFilter(
            underlay,
            criteriaOccurrence1,
            new BooleanNotFilter(criteriaSubFilter1),
            subFiltersPerOccurrenceEntity1,
            groupByAttributesPerOccurrenceEntity1,
            null,
            null);
    assertFalse(
        bqApiTranslator
            .mergedTranslator(List.of(filter1b, filter1e), LogicalOperator.OR, null)
            .isPresent());

    // canMergeWithMergeableCriteriaSubFilters
    assertTrue(
        bqApiTranslator
            .mergedTranslator(List.of(filter1b, filter1b), LogicalOperator.OR, null)
            .isPresent());
  }
}

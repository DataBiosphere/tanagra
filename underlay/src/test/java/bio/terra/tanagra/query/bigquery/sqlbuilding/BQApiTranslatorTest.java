package bio.terra.tanagra.query.bigquery.sqlbuilding;

import static bio.terra.tanagra.UnderlayTestConfigs.CMSSYNPUF;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bio.terra.tanagra.api.filter.AttributeFilter;
import bio.terra.tanagra.api.filter.BooleanAndOrFilter.LogicalOperator;
import bio.terra.tanagra.api.filter.EntityFilter;
import bio.terra.tanagra.api.filter.HierarchyHasAncestorFilter;
import bio.terra.tanagra.api.filter.HierarchyHasParentFilter;
import bio.terra.tanagra.api.filter.HierarchyIsLeafFilter;
import bio.terra.tanagra.api.filter.HierarchyIsMemberFilter;
import bio.terra.tanagra.api.filter.HierarchyIsRootFilter;
import bio.terra.tanagra.api.shared.BinaryOperator;
import bio.terra.tanagra.api.shared.Literal;
import bio.terra.tanagra.query.bigquery.translator.BQApiTranslator;
import bio.terra.tanagra.query.bigquery.translator.filter.BQAttributeFilterTranslator;
import bio.terra.tanagra.underlay.ConfigReader;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.entitymodel.Entity;
import bio.terra.tanagra.underlay.entitymodel.Hierarchy;
import bio.terra.tanagra.underlay.serialization.SZService;
import bio.terra.tanagra.underlay.serialization.SZUnderlay;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class BQApiTranslatorTest {
  private static Underlay underlay;
  private static Entity primaryEntity;
  private static Entity conditionEntity;

  private final BQApiTranslator bqApiTranslator = new BQApiTranslator();

  private Entity getConditionEntity() {
    if (conditionEntity == null) {
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
    return conditionEntity;
  }

  @BeforeAll
  static void setup() {
    ConfigReader configReader = ConfigReader.fromJarResources();
    SZService szService = configReader.readService(CMSSYNPUF.fileName());
    SZUnderlay szUnderlay = configReader.readUnderlay(szService.underlay);
    underlay = Underlay.fromConfig(szService.bigQuery, szUnderlay, configReader);
    primaryEntity = underlay.getPrimaryEntity();
  }

  @Test
  void listSizeAndEntityTests() {
    EntityFilter filter1 =
        new AttributeFilter(
            underlay,
            primaryEntity,
            primaryEntity.getIdAttribute(),
            BinaryOperator.EQUALS,
            Literal.forInt64(11L));

    // cannotMergeSingleFilter
    assertTrue(bqApiTranslator.mergedTranslator(List.of(filter1), null, null).isEmpty());

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
    Entity entity2 =
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

    AttributeFilter filter1 =
        new AttributeFilter(
            underlay,
            entity2,
            entity2.getAttribute("ethnicity"),
            BinaryOperator.EQUALS,
            Literal.forInt64(12L));
    AttributeFilter filter2 =
        new AttributeFilter(
            underlay,
            entity2,
            entity2.getAttribute("gender"),
            BinaryOperator.EQUALS,
            Literal.forInt64(13L));
    AttributeFilter filter3 =
        new AttributeFilter(
            underlay,
            entity2,
            entity2.getAttribute("year_of_birth"),
            BinaryOperator.EQUALS,
            Literal.forInt64(14L));

    // cannotMergeForNotAllAttrOptTogether
    assertTrue(
        BQAttributeFilterTranslator.mergedTranslator(bqApiTranslator, List.of(filter1), null, null)
            .isPresent());

    assertFalse(
        BQAttributeFilterTranslator.mergedTranslator(
                bqApiTranslator, List.of(filter1, filter3), null, null)
            .isPresent());

    assertFalse(
        BQAttributeFilterTranslator.mergedTranslator(
                bqApiTranslator, List.of(filter1, filter2, filter3), null, null)
            .isPresent());

    // canMergeForAllAttrOptTogether
    assertTrue(
        BQAttributeFilterTranslator.mergedTranslator(
                bqApiTranslator, List.of(filter1, filter2), LogicalOperator.OR, null)
            .isPresent());
    assertTrue(
        BQAttributeFilterTranslator.mergedTranslator(bqApiTranslator, List.of(filter3), null, null)
            .isPresent());
  }

  @Test
  void hierarchyHasAncestorFilterTest() {
    Entity testEntity = getConditionEntity();
    HierarchyHasAncestorFilter filter1a =
        new HierarchyHasAncestorFilter(
            underlay, testEntity, testEntity.getHierarchies().get(0), Literal.forInt64(201_826L));
    HierarchyHasAncestorFilter filter1b =
        new HierarchyHasAncestorFilter(
            underlay, testEntity, testEntity.getHierarchies().get(0), Literal.forInt64(10_826L));
    HierarchyHasAncestorFilter filter2 =
        new HierarchyHasAncestorFilter(
            underlay, testEntity, testEntity.getHierarchies().get(1), Literal.forInt64(10_826L));

    // cannotMergeForAndOperator
    assertFalse(
        bqApiTranslator
            .mergedTranslator(List.of(filter1a, filter1b), LogicalOperator.AND, null)
            .isPresent());

    // cannotMergeForDifferentHierarchy
    assertFalse(
        bqApiTranslator
            .mergedTranslator(List.of(filter1a, filter2), LogicalOperator.OR, null)
            .isPresent());

    // canMergeForSameHierarchy
    assertTrue(
        bqApiTranslator
            .mergedTranslator(List.of(filter1a, filter1b), LogicalOperator.OR, null)
            .isPresent());
  }

  @Test
  void hierarchyHasParentFilterTest() {
    Entity testEntity = getConditionEntity();
    HierarchyHasParentFilter filter1a =
        new HierarchyHasParentFilter(
            underlay, testEntity, testEntity.getHierarchies().get(0), Literal.forInt64(201_826L));
    HierarchyHasParentFilter filter1b =
        new HierarchyHasParentFilter(
            underlay, testEntity, testEntity.getHierarchies().get(0), Literal.forInt64(10_826L));
    HierarchyHasParentFilter filter2 =
        new HierarchyHasParentFilter(
            underlay, testEntity, testEntity.getHierarchies().get(1), Literal.forInt64(10_826L));

    // cannotMergeForAndOperator
    assertFalse(
        bqApiTranslator
            .mergedTranslator(List.of(filter1a, filter1b), LogicalOperator.AND, null)
            .isPresent());

    // cannotMergeForDifferentHierarchy
    assertFalse(
        bqApiTranslator
            .mergedTranslator(List.of(filter1a, filter2), LogicalOperator.OR, null)
            .isPresent());

    // canMergeForSameHierarchy
    assertTrue(
        bqApiTranslator
            .mergedTranslator(List.of(filter1a, filter1b), LogicalOperator.OR, null)
            .isPresent());
  }

  @Test
  void hierarchyIsLeafFilterTest() {
    Entity testEntity = getConditionEntity();
    HierarchyIsLeafFilter filter1 =
        new HierarchyIsLeafFilter(underlay, testEntity, testEntity.getHierarchies().get(0));
    HierarchyIsLeafFilter filter2 =
        new HierarchyIsLeafFilter(underlay, testEntity, testEntity.getHierarchies().get(1));

    // cannotMergeForAndOperator
    assertFalse(
        bqApiTranslator
            .mergedTranslator(List.of(filter1, filter2), LogicalOperator.AND, null)
            .isPresent());

    // cannotMergeForDifferentHierarchy
    assertFalse(
        bqApiTranslator
            .mergedTranslator(List.of(filter1, filter2), LogicalOperator.OR, null)
            .isPresent());

    // canMergeForSameHierarchy
    assertTrue(
        bqApiTranslator
            .mergedTranslator(List.of(filter1, filter1), LogicalOperator.OR, null)
            .isPresent());
  }

  @Test
  void hierarchyIsMemberFilterTest() {
    Entity testEntity = getConditionEntity();
    HierarchyIsMemberFilter filter1 =
        new HierarchyIsMemberFilter(underlay, testEntity, testEntity.getHierarchies().get(0));
    HierarchyIsMemberFilter filter2 =
        new HierarchyIsMemberFilter(underlay, testEntity, testEntity.getHierarchies().get(1));

    // cannotMergeForAndOperator
    assertFalse(
        bqApiTranslator
            .mergedTranslator(List.of(filter1, filter2), LogicalOperator.AND, null)
            .isPresent());

    // cannotMergeForDifferentHierarchy
    assertFalse(
        bqApiTranslator
            .mergedTranslator(List.of(filter1, filter2), LogicalOperator.OR, null)
            .isPresent());

    // canMergeForSameHierarchy
    assertTrue(
        bqApiTranslator
            .mergedTranslator(List.of(filter1, filter1), LogicalOperator.OR, null)
            .isPresent());
  }

  @Test
  void hierarchyIsRootFilterTest() {
    Entity testEntity = getConditionEntity();
    HierarchyIsRootFilter filter1 =
        new HierarchyIsRootFilter(underlay, testEntity, testEntity.getHierarchies().get(0));
    HierarchyIsRootFilter filter2 =
        new HierarchyIsRootFilter(underlay, testEntity, testEntity.getHierarchies().get(1));

    // cannotMergeForAndOperator
    assertFalse(
        bqApiTranslator
            .mergedTranslator(List.of(filter1, filter2), LogicalOperator.AND, null)
            .isPresent());

    // cannotMergeForDifferentHierarchy
    assertFalse(
        bqApiTranslator
            .mergedTranslator(List.of(filter1, filter2), LogicalOperator.OR, null)
            .isPresent());

    // canMergeForSameHierarchy
    assertTrue(
        bqApiTranslator
            .mergedTranslator(List.of(filter1, filter1), LogicalOperator.OR, null)
            .isPresent());
  }
}

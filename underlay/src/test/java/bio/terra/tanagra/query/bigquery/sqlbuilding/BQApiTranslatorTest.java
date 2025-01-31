package bio.terra.tanagra.query.bigquery.sqlbuilding;

import static bio.terra.tanagra.UnderlayTestConfigs.CMSSYNPUF;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bio.terra.tanagra.api.filter.AttributeFilter;
import bio.terra.tanagra.api.filter.BooleanAndOrFilter.LogicalOperator;
import bio.terra.tanagra.api.shared.BinaryOperator;
import bio.terra.tanagra.api.shared.Literal;
import bio.terra.tanagra.query.bigquery.translator.BQApiTranslator;
import bio.terra.tanagra.query.bigquery.translator.filter.BQAttributeFilterTranslator;
import bio.terra.tanagra.underlay.ConfigReader;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.entitymodel.Attribute;
import bio.terra.tanagra.underlay.entitymodel.Entity;
import bio.terra.tanagra.underlay.serialization.SZService;
import bio.terra.tanagra.underlay.serialization.SZUnderlay;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class BQApiTranslatorTest {
  private static Underlay underlay;
  private static AttributeFilter e1a1, e1a2, e1a3, e1a4;
  private final BQApiTranslator bqApiTranslator = new BQApiTranslator();

  @BeforeAll
  static void setup() {
    ConfigReader configReader = ConfigReader.fromJarResources();
    SZService szService = configReader.readService(CMSSYNPUF.fileName());
    SZUnderlay szUnderlay = configReader.readUnderlay(szService.underlay);
    underlay = Underlay.fromConfig(szService.bigQuery, szUnderlay, configReader);

    Entity entity = underlay.getPrimaryEntity();
    Entity testEntity =
        new Entity(
            entity.getName(),
            entity.getDisplayName(),
            entity.getDescription(),
            entity.isPrimary(),
            entity.getAttributes(),
            entity.getHierarchies(),
            entity.getOptimizeGroupByAttributes(),
            List.of(
                List.of(entity.getAttribute("ethnicity"), entity.getAttribute("gender")),
                List.of(entity.getAttribute("year_of_birth"))),
            entity.hasTextSearch(),
            entity.getOptimizeTextSearchAttributes(),
            entity.getSourceQueryTableName());

    e1a1 =
        new AttributeFilter(
            underlay,
            testEntity,
            testEntity.getAttribute("race"),
            BinaryOperator.EQUALS,
            Literal.forInt64(11L));
    e1a2 =
        new AttributeFilter(
            underlay,
            testEntity,
            testEntity.getAttribute("ethnicity"),
            BinaryOperator.EQUALS,
            Literal.forInt64(12L));
    e1a3 =
        new AttributeFilter(
            underlay,
            testEntity,
            testEntity.getAttribute("gender"),
            BinaryOperator.EQUALS,
            Literal.forInt64(13L));
    e1a4 =
        new AttributeFilter(
            underlay,
            testEntity,
            testEntity.getAttribute("year_of_birth"),
            BinaryOperator.EQUALS,
            Literal.forInt64(14L));
  }

  @Test
  void cannotMergeSingleFilter() {
    assertTrue(
        BQAttributeFilterTranslator.mergedTranslator(bqApiTranslator, List.of(e1a1), null, null)
            .isEmpty());
  }

  @Test
  void cannotMergeForDifferentEntity() {
    Entity entity2 = underlay.getEntity("condition");
    Attribute attribute3 = entity2.getAttribute("name");
    AttributeFilter e2a3 =
        new AttributeFilter(
            underlay, entity2, attribute3, BinaryOperator.EQUALS, Literal.forInt64(23L));
    assertFalse(
        BQAttributeFilterTranslator.mergedTranslator(
                bqApiTranslator, List.of(e1a1, e2a3), null, null)
            .isPresent());
  }

  @Test
  void canMergeForAllAttrOptTogether() {
    assertTrue(
        BQAttributeFilterTranslator.mergedTranslator(
                bqApiTranslator, List.of(e1a2, e1a3), LogicalOperator.OR, null)
            .isPresent());

    assertTrue(
        BQAttributeFilterTranslator.mergedTranslator(bqApiTranslator, List.of(e1a4), null, null)
            .isPresent());
  }

  @Test
  void cannotMergeForNotAllAttrOptTogether() {
    assertTrue(
        BQAttributeFilterTranslator.mergedTranslator(bqApiTranslator, List.of(e1a2), null, null)
            .isPresent());

    assertFalse(
        BQAttributeFilterTranslator.mergedTranslator(
                bqApiTranslator, List.of(e1a2, e1a4), null, null)
            .isPresent());

    assertFalse(
        BQAttributeFilterTranslator.mergedTranslator(
                bqApiTranslator, List.of(e1a2, e1a3, e1a4), null, null)
            .isPresent());
  }
}

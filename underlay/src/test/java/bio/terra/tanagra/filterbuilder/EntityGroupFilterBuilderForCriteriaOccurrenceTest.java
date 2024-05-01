package bio.terra.tanagra.filterbuilder;

import static bio.terra.tanagra.utils.ProtobufUtils.serializeToJson;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bio.terra.tanagra.api.filter.AttributeFilter;
import bio.terra.tanagra.api.filter.BooleanAndOrFilter;
import bio.terra.tanagra.api.filter.EntityFilter;
import bio.terra.tanagra.api.filter.HierarchyHasAncestorFilter;
import bio.terra.tanagra.api.filter.OccurrenceForPrimaryFilter;
import bio.terra.tanagra.api.filter.PrimaryWithCriteriaFilter;
import bio.terra.tanagra.api.shared.BinaryOperator;
import bio.terra.tanagra.api.shared.Literal;
import bio.terra.tanagra.api.shared.NaryOperator;
import bio.terra.tanagra.filterbuilder.impl.core.EntityGroupFilterBuilder;
import bio.terra.tanagra.proto.criteriaselector.DataRangeOuterClass.DataRange;
import bio.terra.tanagra.proto.criteriaselector.KeyOuterClass.Key;
import bio.terra.tanagra.proto.criteriaselector.ValueDataOuterClass.ValueData;
import bio.terra.tanagra.proto.criteriaselector.ValueOuterClass.Value;
import bio.terra.tanagra.proto.criteriaselector.configschema.CFAttribute;
import bio.terra.tanagra.proto.criteriaselector.configschema.CFEntityGroup;
import bio.terra.tanagra.proto.criteriaselector.configschema.CFUnhintedValue;
import bio.terra.tanagra.proto.criteriaselector.dataschema.DTAttribute;
import bio.terra.tanagra.proto.criteriaselector.dataschema.DTEntityGroup;
import bio.terra.tanagra.proto.criteriaselector.dataschema.DTUnhintedValue;
import bio.terra.tanagra.underlay.ConfigReader;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.entitymodel.Entity;
import bio.terra.tanagra.underlay.entitymodel.Hierarchy;
import bio.terra.tanagra.underlay.entitymodel.entitygroup.CriteriaOccurrence;
import bio.terra.tanagra.underlay.serialization.SZCorePlugin;
import bio.terra.tanagra.underlay.serialization.SZService;
import bio.terra.tanagra.underlay.serialization.SZUnderlay;
import bio.terra.tanagra.underlay.uiplugin.CriteriaSelector;
import bio.terra.tanagra.underlay.uiplugin.SelectionData;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class EntityGroupFilterBuilderForCriteriaOccurrenceTest {
  private Underlay underlay;

  @BeforeEach
  void setup() {
    ConfigReader configReader = ConfigReader.fromJarResources();
    SZService szService = configReader.readService("aouSR2019q4r4_broad");
    SZUnderlay szUnderlay = configReader.readUnderlay(szService.underlay);
    underlay = Underlay.fromConfig(szService.bigQuery, szUnderlay, configReader);
  }

  @Test
  void criteriaOnlyCohortFilter() {
    CFEntityGroup.EntityGroup config = CFEntityGroup.EntityGroup.newBuilder().build();
    CriteriaSelector criteriaSelector =
        new CriteriaSelector(
            "condition",
            true,
            true,
            "core.EntityGroupFilterBuilder",
            SZCorePlugin.ENTITY_GROUP.getIdInConfig(),
            serializeToJson(config),
            List.of());
    EntityGroupFilterBuilder filterBuilder = new EntityGroupFilterBuilder(criteriaSelector);

    // Single id.
    DTEntityGroup.EntityGroup data =
        DTEntityGroup.EntityGroup.newBuilder()
            .addSelected(
                DTEntityGroup.EntityGroup.Selection.newBuilder()
                    .setKey(Key.newBuilder().setInt64Key(201_826L).build())
                    .setName("Type 2 diabetes mellitus")
                    .setEntityGroup("conditionPerson")
                    .build())
            .build();
    SelectionData selectionData = new SelectionData("condition", serializeToJson(data));
    EntityFilter cohortFilter = filterBuilder.buildForCohort(underlay, List.of(selectionData));
    assertNotNull(cohortFilter);
    EntityFilter expectedCriteriaSubFilter =
        new HierarchyHasAncestorFilter(
            underlay,
            underlay.getEntity("condition"),
            underlay.getEntity("condition").getHierarchy(Hierarchy.DEFAULT_NAME),
            Literal.forInt64(201_826L));
    EntityFilter expectedCohortFilter =
        new PrimaryWithCriteriaFilter(
            underlay,
            (CriteriaOccurrence) underlay.getEntityGroup("conditionPerson"),
            expectedCriteriaSubFilter,
            null,
            null,
            null,
            null);
    assertEquals(expectedCohortFilter, cohortFilter);

    // Multiple ids, same entity group.
    data =
        DTEntityGroup.EntityGroup.newBuilder()
            .addSelected(
                DTEntityGroup.EntityGroup.Selection.newBuilder()
                    .setKey(Key.newBuilder().setInt64Key(201_826L).build())
                    .setName("Type 2 diabetes mellitus")
                    .setEntityGroup("conditionPerson")
                    .build())
            .addSelected(
                DTEntityGroup.EntityGroup.Selection.newBuilder()
                    .setKey(Key.newBuilder().setInt64Key(201_254L).build())
                    .setName("Type 1 diabetes mellitus")
                    .setEntityGroup("conditionPerson")
                    .build())
            .build();
    selectionData = new SelectionData("condition", serializeToJson(data));
    cohortFilter = filterBuilder.buildForCohort(underlay, List.of(selectionData));
    assertNotNull(cohortFilter);
    expectedCriteriaSubFilter =
        new HierarchyHasAncestorFilter(
            underlay,
            underlay.getEntity("condition"),
            underlay.getEntity("condition").getHierarchy(Hierarchy.DEFAULT_NAME),
            List.of(Literal.forInt64(201_826L), Literal.forInt64(201_254L)));
    expectedCohortFilter =
        new PrimaryWithCriteriaFilter(
            underlay,
            (CriteriaOccurrence) underlay.getEntityGroup("conditionPerson"),
            expectedCriteriaSubFilter,
            null,
            null,
            null,
            null);
    assertEquals(expectedCohortFilter, cohortFilter);

    // Multiple ids, different entity groups.
    data =
        DTEntityGroup.EntityGroup.newBuilder()
            .addSelected(
                DTEntityGroup.EntityGroup.Selection.newBuilder()
                    .setKey(Key.newBuilder().setInt64Key(201_826L).build())
                    .setName("Type 2 diabetes mellitus")
                    .setEntityGroup("conditionPerson")
                    .build())
            .addSelected(
                DTEntityGroup.EntityGroup.Selection.newBuilder()
                    .setKey(Key.newBuilder().setInt64Key(4_198_190L).build())
                    .setName("Appendectomy")
                    .setEntityGroup("procedurePerson")
                    .build())
            .build();
    selectionData = new SelectionData("condition", serializeToJson(data));
    cohortFilter = filterBuilder.buildForCohort(underlay, List.of(selectionData));
    assertNotNull(cohortFilter);
    EntityFilter expectedCriteriaSubFilter1 =
        new HierarchyHasAncestorFilter(
            underlay,
            underlay.getEntity("condition"),
            underlay.getEntity("condition").getHierarchy(Hierarchy.DEFAULT_NAME),
            Literal.forInt64(201_826L));
    EntityFilter expectedCohortFilter1 =
        new PrimaryWithCriteriaFilter(
            underlay,
            (CriteriaOccurrence) underlay.getEntityGroup("conditionPerson"),
            expectedCriteriaSubFilter1,
            null,
            null,
            null,
            null);
    EntityFilter expectedCriteriaSubFilter2 =
        new HierarchyHasAncestorFilter(
            underlay,
            underlay.getEntity("procedure"),
            underlay.getEntity("procedure").getHierarchy(Hierarchy.DEFAULT_NAME),
            Literal.forInt64(4_198_190L));
    EntityFilter expectedCohortFilter2 =
        new PrimaryWithCriteriaFilter(
            underlay,
            (CriteriaOccurrence) underlay.getEntityGroup("procedurePerson"),
            expectedCriteriaSubFilter2,
            null,
            null,
            null,
            null);
    expectedCohortFilter =
        new BooleanAndOrFilter(
            BooleanAndOrFilter.LogicalOperator.OR,
            List.of(expectedCohortFilter1, expectedCohortFilter2));
    assertEquals(expectedCohortFilter, cohortFilter);
  }

  @Test
  void criteriaWithAttrModifiersCohortFilter() {
    CFAttribute.Attribute ageAtOccurrenceConfig =
        CFAttribute.Attribute.newBuilder().setAttribute("age_at_occurrence").build();
    CriteriaSelector.Modifier ageAtOccurrenceModifier =
        new CriteriaSelector.Modifier(
            "age_at_occurrence",
            SZCorePlugin.ATTRIBUTE.getIdInConfig(),
            serializeToJson(ageAtOccurrenceConfig));
    CFAttribute.Attribute visitTypeConfig =
        CFAttribute.Attribute.newBuilder().setAttribute("visit_type").build();
    CriteriaSelector.Modifier visitTypeModifier =
        new CriteriaSelector.Modifier(
            "visit_type", SZCorePlugin.ATTRIBUTE.getIdInConfig(), serializeToJson(visitTypeConfig));
    CFEntityGroup.EntityGroup conditionConfig = CFEntityGroup.EntityGroup.newBuilder().build();
    CriteriaSelector criteriaSelector =
        new CriteriaSelector(
            "condition",
            true,
            true,
            "core.EntityGroupFilterBuilder",
            SZCorePlugin.ENTITY_GROUP.getIdInConfig(),
            serializeToJson(conditionConfig),
            List.of(ageAtOccurrenceModifier, visitTypeModifier));
    EntityGroupFilterBuilder filterBuilder = new EntityGroupFilterBuilder(criteriaSelector);

    // Single attribute modifier.
    DTAttribute.Attribute ageAtOccurrenceData =
        DTAttribute.Attribute.newBuilder()
            .addDataRanges(DataRange.newBuilder().setMin(45).setMax(65).build())
            .build();
    SelectionData ageAtOccurrenceSelectionData =
        new SelectionData("age_at_occurrence", serializeToJson(ageAtOccurrenceData));
    DTEntityGroup.EntityGroup entityGroupData =
        DTEntityGroup.EntityGroup.newBuilder()
            .addSelected(
                DTEntityGroup.EntityGroup.Selection.newBuilder()
                    .setKey(Key.newBuilder().setInt64Key(201_826L).build())
                    .setName("Type 2 diabetes mellitus")
                    .setEntityGroup("conditionPerson")
                    .build())
            .build();
    SelectionData entityGroupSelectionData =
        new SelectionData("condition", serializeToJson(entityGroupData));
    EntityFilter cohortFilter =
        filterBuilder.buildForCohort(
            underlay, List.of(entityGroupSelectionData, ageAtOccurrenceSelectionData));
    assertNotNull(cohortFilter);
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
            NaryOperator.BETWEEN,
            List.of(Literal.forDouble(45.0), Literal.forDouble(65.0)));
    EntityFilter expectedCohortFilter =
        new PrimaryWithCriteriaFilter(
            underlay,
            (CriteriaOccurrence) underlay.getEntityGroup("conditionPerson"),
            expectedCriteriaSubFilter,
            Map.of(
                underlay.getEntity("conditionOccurrence"),
                List.of(expectedAgeAtOccurrenceSubFilter)),
            null,
            null,
            null);
    assertEquals(expectedCohortFilter, cohortFilter);

    // Two attribute modifiers.
    DTAttribute.Attribute visitTypeData =
        DTAttribute.Attribute.newBuilder()
            .addSelected(
                DTAttribute.Attribute.Selection.newBuilder()
                    .setValue(Value.newBuilder().setInt64Value(9_202L).build())
                    .setName("Outpatient Visit")
                    .build())
            .build();
    SelectionData visitTypeSelectionData =
        new SelectionData("visit_type", serializeToJson(visitTypeData));
    cohortFilter =
        filterBuilder.buildForCohort(
            underlay,
            List.of(
                entityGroupSelectionData, ageAtOccurrenceSelectionData, visitTypeSelectionData));
    assertNotNull(cohortFilter);
    EntityFilter expectedVisitTypeSubFilter =
        new AttributeFilter(
            underlay,
            underlay.getEntity("conditionOccurrence"),
            underlay.getEntity("conditionOccurrence").getAttribute("visit_type"),
            BinaryOperator.EQUALS,
            Literal.forInt64(9_202L));
    expectedCohortFilter =
        new PrimaryWithCriteriaFilter(
            underlay,
            (CriteriaOccurrence) underlay.getEntityGroup("conditionPerson"),
            expectedCriteriaSubFilter,
            Map.of(
                underlay.getEntity("conditionOccurrence"),
                List.of(expectedAgeAtOccurrenceSubFilter, expectedVisitTypeSubFilter)),
            null,
            null,
            null);
    assertEquals(expectedCohortFilter, cohortFilter);
  }

  @Test
  void criteriaWithGroupByModifierCohortFilter() {
    CFUnhintedValue.UnhintedValue groupByConfig =
        CFUnhintedValue.UnhintedValue.newBuilder()
            .putAttributes(
                "conditionOccurrence",
                CFUnhintedValue.UnhintedValue.AttributeList.newBuilder()
                    .addValues("start_date")
                    .build())
            .build();
    CriteriaSelector.Modifier groupByModifier =
        new CriteriaSelector.Modifier(
            "group_by_count",
            SZCorePlugin.UNHINTED_VALUE.getIdInConfig(),
            serializeToJson(groupByConfig));
    CFEntityGroup.EntityGroup conditionConfig = CFEntityGroup.EntityGroup.newBuilder().build();
    CriteriaSelector criteriaSelector =
        new CriteriaSelector(
            "condition",
            true,
            true,
            "core.EntityGroupFilterBuilder",
            SZCorePlugin.ENTITY_GROUP.getIdInConfig(),
            serializeToJson(conditionConfig),
            List.of(groupByModifier));
    EntityGroupFilterBuilder filterBuilder = new EntityGroupFilterBuilder(criteriaSelector);

    DTUnhintedValue.UnhintedValue groupByData =
        DTUnhintedValue.UnhintedValue.newBuilder()
            .setOperator(
                DTUnhintedValue.UnhintedValue.ComparisonOperator
                    .COMPARISON_OPERATOR_GREATER_THAN_EQUAL)
            .setMin(2.0)
            .build();
    SelectionData groupBySelectionData =
        new SelectionData("group_by_count", serializeToJson(groupByData));
    DTEntityGroup.EntityGroup entityGroupData =
        DTEntityGroup.EntityGroup.newBuilder()
            .addSelected(
                DTEntityGroup.EntityGroup.Selection.newBuilder()
                    .setKey(Key.newBuilder().setInt64Key(201_826L).build())
                    .setName("Type 2 diabetes mellitus")
                    .setEntityGroup("conditionPerson")
                    .build())
            .build();
    SelectionData entityGroupSelectionData =
        new SelectionData("condition", serializeToJson(entityGroupData));
    EntityFilter cohortFilter =
        filterBuilder.buildForCohort(
            underlay, List.of(entityGroupSelectionData, groupBySelectionData));
    assertNotNull(cohortFilter);
    EntityFilter expectedCriteriaSubFilter =
        new HierarchyHasAncestorFilter(
            underlay,
            underlay.getEntity("condition"),
            underlay.getEntity("condition").getHierarchy(Hierarchy.DEFAULT_NAME),
            Literal.forInt64(201_826L));
    EntityFilter expectedCohortFilter =
        new PrimaryWithCriteriaFilter(
            underlay,
            (CriteriaOccurrence) underlay.getEntityGroup("conditionPerson"),
            expectedCriteriaSubFilter,
            null,
            Map.of(
                underlay.getEntity("conditionOccurrence"),
                List.of(underlay.getEntity("conditionOccurrence").getAttribute("start_date"))),
            BinaryOperator.GREATER_THAN_OR_EQUAL,
            2);
    assertEquals(expectedCohortFilter, cohortFilter);
  }

  @Test
  void criteriaWithInstanceLevelModifierCohortFilter() {
    CFEntityGroup.EntityGroup measurementConfig = CFEntityGroup.EntityGroup.newBuilder().build();
    CriteriaSelector criteriaSelector =
        new CriteriaSelector(
            "measurement",
            true,
            true,
            "core.EntityGroupFilterBuilder",
            SZCorePlugin.ENTITY_GROUP.getIdInConfig(),
            serializeToJson(measurementConfig),
            List.of());
    EntityGroupFilterBuilder filterBuilder = new EntityGroupFilterBuilder(criteriaSelector);

    // Enum attribute.
    DTEntityGroup.EntityGroup entityGroupData =
        DTEntityGroup.EntityGroup.newBuilder()
            .addSelected(
                DTEntityGroup.EntityGroup.Selection.newBuilder()
                    .setKey(Key.newBuilder().setInt64Key(3_004_501L).build())
                    .setName("Glucose [Mass/volume] in Serum or Plasma")
                    .setEntityGroup("measurementLoincPerson")
                    .build())
            .setValueData(
                ValueData.newBuilder()
                    .setAttribute("value_enum")
                    .addSelected(
                        ValueData.Selection.newBuilder()
                            .setValue(Value.newBuilder().setInt64Value(45_884_084L).build())
                            .setName("Positive")
                            .build()))
            .build();
    SelectionData entityGroupSelectionData =
        new SelectionData("measurement", serializeToJson(entityGroupData));
    EntityFilter cohortFilter =
        filterBuilder.buildForCohort(underlay, List.of(entityGroupSelectionData));
    assertNotNull(cohortFilter);
    EntityFilter expectedCriteriaSubFilter =
        new HierarchyHasAncestorFilter(
            underlay,
            underlay.getEntity("measurementLoinc"),
            underlay.getEntity("measurementLoinc").getHierarchy(Hierarchy.DEFAULT_NAME),
            Literal.forInt64(3_004_501L));
    EntityFilter expectedInstanceLevelSubFilter =
        new AttributeFilter(
            underlay,
            underlay.getEntity("measurementOccurrence"),
            underlay.getEntity("measurementOccurrence").getAttribute("value_enum"),
            BinaryOperator.EQUALS,
            Literal.forInt64(45_884_084L));
    EntityFilter expectedCohortFilter =
        new PrimaryWithCriteriaFilter(
            underlay,
            (CriteriaOccurrence) underlay.getEntityGroup("measurementLoincPerson"),
            expectedCriteriaSubFilter,
            Map.of(
                underlay.getEntity("measurementOccurrence"),
                List.of(expectedInstanceLevelSubFilter)),
            null,
            null,
            null);
    assertEquals(expectedCohortFilter, cohortFilter);

    // Numeric attribute.
    entityGroupData =
        DTEntityGroup.EntityGroup.newBuilder()
            .addSelected(
                DTEntityGroup.EntityGroup.Selection.newBuilder()
                    .setKey(Key.newBuilder().setInt64Key(3_004_501L).build())
                    .setName("Glucose [Mass/volume] in Serum or Plasma")
                    .setEntityGroup("measurementLoincPerson")
                    .build())
            .setValueData(
                ValueData.newBuilder()
                    .setAttribute("value_numeric")
                    .setRange(DataRange.newBuilder().setMin(0.0).setMax(250.0).build()))
            .build();
    entityGroupSelectionData = new SelectionData("measurement", serializeToJson(entityGroupData));
    cohortFilter = filterBuilder.buildForCohort(underlay, List.of(entityGroupSelectionData));
    assertNotNull(cohortFilter);
    expectedCriteriaSubFilter =
        new HierarchyHasAncestorFilter(
            underlay,
            underlay.getEntity("measurementLoinc"),
            underlay.getEntity("measurementLoinc").getHierarchy(Hierarchy.DEFAULT_NAME),
            Literal.forInt64(3_004_501L));
    expectedInstanceLevelSubFilter =
        new AttributeFilter(
            underlay,
            underlay.getEntity("measurementOccurrence"),
            underlay.getEntity("measurementOccurrence").getAttribute("value_numeric"),
            NaryOperator.BETWEEN,
            List.of(Literal.forDouble(0.0), Literal.forDouble(250.0)));
    expectedCohortFilter =
        new PrimaryWithCriteriaFilter(
            underlay,
            (CriteriaOccurrence) underlay.getEntityGroup("measurementLoincPerson"),
            expectedCriteriaSubFilter,
            Map.of(
                underlay.getEntity("measurementOccurrence"),
                List.of(expectedInstanceLevelSubFilter)),
            null,
            null,
            null);
    assertEquals(expectedCohortFilter, cohortFilter);
  }

  @Test
  void criteriaWithAttrAndInstanceLevelAndGroupByModifiersCohortFilter() {
    CFAttribute.Attribute ageAtOccurrenceConfig =
        CFAttribute.Attribute.newBuilder().setAttribute("age_at_occurrence").build();
    CriteriaSelector.Modifier ageAtOccurrenceModifier =
        new CriteriaSelector.Modifier(
            "age_at_occurrence",
            SZCorePlugin.ATTRIBUTE.getIdInConfig(),
            serializeToJson(ageAtOccurrenceConfig));
    CFAttribute.Attribute visitTypeConfig =
        CFAttribute.Attribute.newBuilder().setAttribute("visit_type").build();
    CriteriaSelector.Modifier visitTypeModifier =
        new CriteriaSelector.Modifier(
            "visit_type", SZCorePlugin.ATTRIBUTE.getIdInConfig(), serializeToJson(visitTypeConfig));
    CFUnhintedValue.UnhintedValue groupByConfig =
        CFUnhintedValue.UnhintedValue.newBuilder()
            .putAttributes(
                "measurementOccurrence",
                CFUnhintedValue.UnhintedValue.AttributeList.newBuilder().addValues("date").build())
            .build();
    CriteriaSelector.Modifier groupByModifier =
        new CriteriaSelector.Modifier(
            "group_by_count",
            SZCorePlugin.UNHINTED_VALUE.getIdInConfig(),
            serializeToJson(groupByConfig));
    CFEntityGroup.EntityGroup conditionConfig = CFEntityGroup.EntityGroup.newBuilder().build();
    CriteriaSelector criteriaSelector =
        new CriteriaSelector(
            "measurement",
            true,
            true,
            "core.EntityGroupFilterBuilder",
            SZCorePlugin.ENTITY_GROUP.getIdInConfig(),
            serializeToJson(conditionConfig),
            List.of(ageAtOccurrenceModifier, visitTypeModifier, groupByModifier));
    EntityGroupFilterBuilder filterBuilder = new EntityGroupFilterBuilder(criteriaSelector);

    DTAttribute.Attribute ageAtOccurrenceData =
        DTAttribute.Attribute.newBuilder()
            .addDataRanges(DataRange.newBuilder().setMin(45).setMax(65).build())
            .build();
    SelectionData ageAtOccurrenceSelectionData =
        new SelectionData("age_at_occurrence", serializeToJson(ageAtOccurrenceData));
    DTAttribute.Attribute visitTypeData =
        DTAttribute.Attribute.newBuilder()
            .addSelected(
                DTAttribute.Attribute.Selection.newBuilder()
                    .setValue(Value.newBuilder().setInt64Value(9_202L).build())
                    .setName("Outpatient Visit")
                    .build())
            .build();
    SelectionData visitTypeSelectionData =
        new SelectionData("visit_type", serializeToJson(visitTypeData));
    DTUnhintedValue.UnhintedValue groupByData =
        DTUnhintedValue.UnhintedValue.newBuilder()
            .setOperator(
                DTUnhintedValue.UnhintedValue.ComparisonOperator
                    .COMPARISON_OPERATOR_GREATER_THAN_EQUAL)
            .setMin(2.0)
            .build();
    SelectionData groupBySelectionData =
        new SelectionData("group_by_count", serializeToJson(groupByData));
    DTEntityGroup.EntityGroup entityGroupData =
        DTEntityGroup.EntityGroup.newBuilder()
            .addSelected(
                DTEntityGroup.EntityGroup.Selection.newBuilder()
                    .setKey(Key.newBuilder().setInt64Key(3_004_501L).build())
                    .setName("Glucose [Mass/volume] in Serum or Plasma")
                    .setEntityGroup("measurementLoincPerson")
                    .build())
            .setValueData(
                ValueData.newBuilder()
                    .setAttribute("value_numeric")
                    .setRange(DataRange.newBuilder().setMin(0.0).setMax(250.0).build()))
            .build();
    SelectionData entityGroupSelectionData =
        new SelectionData("measurement", serializeToJson(entityGroupData));
    EntityFilter cohortFilter =
        filterBuilder.buildForCohort(
            underlay,
            List.of(
                entityGroupSelectionData,
                ageAtOccurrenceSelectionData,
                visitTypeSelectionData,
                groupBySelectionData));
    assertNotNull(cohortFilter);
    EntityFilter expectedCriteriaSubFilter =
        new HierarchyHasAncestorFilter(
            underlay,
            underlay.getEntity("measurementLoinc"),
            underlay.getEntity("measurementLoinc").getHierarchy(Hierarchy.DEFAULT_NAME),
            Literal.forInt64(3_004_501L));
    EntityFilter expectedAgeAtOccurrenceSubFilter =
        new AttributeFilter(
            underlay,
            underlay.getEntity("measurementOccurrence"),
            underlay.getEntity("measurementOccurrence").getAttribute("age_at_occurrence"),
            NaryOperator.BETWEEN,
            List.of(Literal.forDouble(45.0), Literal.forDouble(65.0)));
    EntityFilter expectedVisitTypeSubFilter =
        new AttributeFilter(
            underlay,
            underlay.getEntity("measurementOccurrence"),
            underlay.getEntity("measurementOccurrence").getAttribute("visit_type"),
            BinaryOperator.EQUALS,
            Literal.forInt64(9_202L));
    EntityFilter expectedInstanceLevelSubFilter =
        new AttributeFilter(
            underlay,
            underlay.getEntity("measurementOccurrence"),
            underlay.getEntity("measurementOccurrence").getAttribute("value_numeric"),
            NaryOperator.BETWEEN,
            List.of(Literal.forDouble(0.0), Literal.forDouble(250.0)));
    EntityFilter expectedCohortFilter =
        new PrimaryWithCriteriaFilter(
            underlay,
            (CriteriaOccurrence) underlay.getEntityGroup("measurementLoincPerson"),
            expectedCriteriaSubFilter,
            Map.of(
                underlay.getEntity("measurementOccurrence"),
                List.of(
                    expectedAgeAtOccurrenceSubFilter,
                    expectedVisitTypeSubFilter,
                    expectedInstanceLevelSubFilter)),
            Map.of(
                underlay.getEntity("measurementOccurrence"),
                List.of(underlay.getEntity("measurementOccurrence").getAttribute("date"))),
            BinaryOperator.GREATER_THAN_OR_EQUAL,
            2);
    assertEquals(expectedCohortFilter, cohortFilter);
  }

  @Test
  void emptyCriteriaCohortFilter() {
    CFEntityGroup.EntityGroup config = CFEntityGroup.EntityGroup.newBuilder().build();
    CriteriaSelector criteriaSelector =
        new CriteriaSelector(
            "condition",
            true,
            true,
            "core.EntityGroupFilterBuilder",
            SZCorePlugin.ENTITY_GROUP.getIdInConfig(),
            serializeToJson(config),
            List.of());
    EntityGroupFilterBuilder filterBuilder = new EntityGroupFilterBuilder(criteriaSelector);

    // Null selection data.
    SelectionData selectionData = new SelectionData("condition", null);
    EntityFilter cohortFilter = filterBuilder.buildForCohort(underlay, List.of(selectionData));
    assertNull(cohortFilter);

    // Empty string selection data.
    selectionData = new SelectionData("condition", "");
    cohortFilter = filterBuilder.buildForCohort(underlay, List.of(selectionData));
    assertNull(cohortFilter);
  }

  @Test
  void emptyAttrModifierCohortFilter() {
    CFAttribute.Attribute ageAtOccurrenceConfig =
        CFAttribute.Attribute.newBuilder().setAttribute("age_at_occurrence").build();
    CriteriaSelector.Modifier ageAtOccurrenceModifier =
        new CriteriaSelector.Modifier(
            "age_at_occurrence",
            SZCorePlugin.ATTRIBUTE.getIdInConfig(),
            serializeToJson(ageAtOccurrenceConfig));
    CFEntityGroup.EntityGroup config = CFEntityGroup.EntityGroup.newBuilder().build();
    CriteriaSelector criteriaSelector =
        new CriteriaSelector(
            "condition",
            true,
            true,
            "core.EntityGroupFilterBuilder",
            SZCorePlugin.ENTITY_GROUP.getIdInConfig(),
            serializeToJson(config),
            List.of(ageAtOccurrenceModifier));
    EntityGroupFilterBuilder filterBuilder = new EntityGroupFilterBuilder(criteriaSelector);

    DTEntityGroup.EntityGroup entityGroupData =
        DTEntityGroup.EntityGroup.newBuilder()
            .addSelected(
                DTEntityGroup.EntityGroup.Selection.newBuilder()
                    .setKey(Key.newBuilder().setInt64Key(201_826L).build())
                    .setName("Type 2 diabetes mellitus")
                    .setEntityGroup("conditionPerson")
                    .build())
            .build();
    SelectionData entityGroupSelectionData =
        new SelectionData("condition", serializeToJson(entityGroupData));

    EntityFilter expectedCriteriaSubFilter =
        new HierarchyHasAncestorFilter(
            underlay,
            underlay.getEntity("condition"),
            underlay.getEntity("condition").getHierarchy(Hierarchy.DEFAULT_NAME),
            Literal.forInt64(201_826L));
    EntityFilter expectedCohortFilter =
        new PrimaryWithCriteriaFilter(
            underlay,
            (CriteriaOccurrence) underlay.getEntityGroup("conditionPerson"),
            expectedCriteriaSubFilter,
            Map.of(),
            null,
            null,
            null);

    // Null selection data.
    SelectionData ageAtOccurrenceSelectionData = new SelectionData("age_at_occurrence", null);
    EntityFilter cohortFilter =
        filterBuilder.buildForCohort(
            underlay, List.of(entityGroupSelectionData, ageAtOccurrenceSelectionData));
    assertNotNull(cohortFilter);
    assertEquals(expectedCohortFilter, cohortFilter);

    // Empty string selection data.
    ageAtOccurrenceSelectionData = new SelectionData("age_at_occurrence", "");
    cohortFilter =
        filterBuilder.buildForCohort(
            underlay, List.of(entityGroupSelectionData, ageAtOccurrenceSelectionData));
    assertNotNull(cohortFilter);
    assertEquals(expectedCohortFilter, cohortFilter);
  }

  @Test
  void emptyGroupByModifierCohortFilter() {
    CFUnhintedValue.UnhintedValue groupByConfig =
        CFUnhintedValue.UnhintedValue.newBuilder()
            .putAttributes(
                "conditionOccurrence",
                CFUnhintedValue.UnhintedValue.AttributeList.newBuilder()
                    .addValues("start_date")
                    .build())
            .build();
    CriteriaSelector.Modifier groupByModifier =
        new CriteriaSelector.Modifier(
            "group_by_count",
            SZCorePlugin.UNHINTED_VALUE.getIdInConfig(),
            serializeToJson(groupByConfig));
    CFEntityGroup.EntityGroup config = CFEntityGroup.EntityGroup.newBuilder().build();
    CriteriaSelector criteriaSelector =
        new CriteriaSelector(
            "condition",
            true,
            true,
            "core.EntityGroupFilterBuilder",
            SZCorePlugin.ENTITY_GROUP.getIdInConfig(),
            serializeToJson(config),
            List.of(groupByModifier));
    EntityGroupFilterBuilder filterBuilder = new EntityGroupFilterBuilder(criteriaSelector);

    DTEntityGroup.EntityGroup entityGroupData =
        DTEntityGroup.EntityGroup.newBuilder()
            .addSelected(
                DTEntityGroup.EntityGroup.Selection.newBuilder()
                    .setKey(Key.newBuilder().setInt64Key(201_826L).build())
                    .setName("Type 2 diabetes mellitus")
                    .setEntityGroup("conditionPerson")
                    .build())
            .build();
    SelectionData entityGroupSelectionData =
        new SelectionData("condition", serializeToJson(entityGroupData));

    EntityFilter expectedCriteriaSubFilter =
        new HierarchyHasAncestorFilter(
            underlay,
            underlay.getEntity("condition"),
            underlay.getEntity("condition").getHierarchy(Hierarchy.DEFAULT_NAME),
            Literal.forInt64(201_826L));
    EntityFilter expectedCohortFilter =
        new PrimaryWithCriteriaFilter(
            underlay,
            (CriteriaOccurrence) underlay.getEntityGroup("conditionPerson"),
            expectedCriteriaSubFilter,
            Map.of(),
            null,
            null,
            null);

    // Null selection data.
    SelectionData groupBySelectionData = new SelectionData("group_by_count", null);
    EntityFilter cohortFilter =
        filterBuilder.buildForCohort(
            underlay, List.of(entityGroupSelectionData, groupBySelectionData));
    assertNotNull(cohortFilter);
    assertEquals(expectedCohortFilter, cohortFilter);

    // Empty string selection data.
    groupBySelectionData = new SelectionData("group_by_count", "");
    cohortFilter =
        filterBuilder.buildForCohort(
            underlay, List.of(entityGroupSelectionData, groupBySelectionData));
    assertNotNull(cohortFilter);
    assertEquals(expectedCohortFilter, cohortFilter);
  }

  @Test
  void criteriaOnlySingleOccurrenceDataFeatureFilter() {
    CFEntityGroup.EntityGroup config = CFEntityGroup.EntityGroup.newBuilder().build();
    CriteriaSelector criteriaSelector =
        new CriteriaSelector(
            "condition",
            true,
            true,
            "core.EntityGroupFilterBuilder",
            SZCorePlugin.ENTITY_GROUP.getIdInConfig(),
            serializeToJson(config),
            List.of());
    EntityGroupFilterBuilder filterBuilder = new EntityGroupFilterBuilder(criteriaSelector);

    // Single id.
    DTEntityGroup.EntityGroup data =
        DTEntityGroup.EntityGroup.newBuilder()
            .addSelected(
                DTEntityGroup.EntityGroup.Selection.newBuilder()
                    .setKey(Key.newBuilder().setInt64Key(201_826L).build())
                    .setName("Type 2 diabetes mellitus")
                    .setEntityGroup("conditionPerson")
                    .build())
            .build();
    SelectionData selectionData = new SelectionData("condition", serializeToJson(data));
    List<EntityOutput> dataFeatureOutputs =
        filterBuilder.buildForDataFeature(underlay, List.of(selectionData));
    assertEquals(1, dataFeatureOutputs.size());
    EntityFilter expectedCriteriaSubFilter =
        new HierarchyHasAncestorFilter(
            underlay,
            underlay.getEntity("condition"),
            underlay.getEntity("condition").getHierarchy(Hierarchy.DEFAULT_NAME),
            Literal.forInt64(201_826L));
    EntityFilter expectedDataFeatureFilter =
        new OccurrenceForPrimaryFilter(
            underlay,
            (CriteriaOccurrence) underlay.getEntityGroup("conditionPerson"),
            underlay.getEntity("conditionOccurrence"),
            null,
            expectedCriteriaSubFilter);
    EntityOutput expectedDataFeatureOutput =
        EntityOutput.filtered(underlay.getEntity("conditionOccurrence"), expectedDataFeatureFilter);
    assertEquals(expectedDataFeatureOutput, dataFeatureOutputs.get(0));

    // Multiple ids, same entity group.
    data =
        DTEntityGroup.EntityGroup.newBuilder()
            .addSelected(
                DTEntityGroup.EntityGroup.Selection.newBuilder()
                    .setKey(Key.newBuilder().setInt64Key(201_826L).build())
                    .setName("Type 2 diabetes mellitus")
                    .setEntityGroup("conditionPerson")
                    .build())
            .addSelected(
                DTEntityGroup.EntityGroup.Selection.newBuilder()
                    .setKey(Key.newBuilder().setInt64Key(201_254L).build())
                    .setName("Type 1 diabetes mellitus")
                    .setEntityGroup("conditionPerson")
                    .build())
            .build();
    selectionData = new SelectionData("condition", serializeToJson(data));
    dataFeatureOutputs = filterBuilder.buildForDataFeature(underlay, List.of(selectionData));
    assertEquals(1, dataFeatureOutputs.size());
    expectedCriteriaSubFilter =
        new HierarchyHasAncestorFilter(
            underlay,
            underlay.getEntity("condition"),
            underlay.getEntity("condition").getHierarchy(Hierarchy.DEFAULT_NAME),
            List.of(Literal.forInt64(201_826L), Literal.forInt64(201_254L)));
    expectedDataFeatureFilter =
        new OccurrenceForPrimaryFilter(
            underlay,
            (CriteriaOccurrence) underlay.getEntityGroup("conditionPerson"),
            underlay.getEntity("conditionOccurrence"),
            null,
            expectedCriteriaSubFilter);
    expectedDataFeatureOutput =
        EntityOutput.filtered(underlay.getEntity("conditionOccurrence"), expectedDataFeatureFilter);
    assertEquals(expectedDataFeatureOutput, dataFeatureOutputs.get(0));

    // Multiple ids, different entity groups.
    data =
        DTEntityGroup.EntityGroup.newBuilder()
            .addSelected(
                DTEntityGroup.EntityGroup.Selection.newBuilder()
                    .setKey(Key.newBuilder().setInt64Key(201_826L).build())
                    .setName("Type 2 diabetes mellitus")
                    .setEntityGroup("conditionPerson")
                    .build())
            .addSelected(
                DTEntityGroup.EntityGroup.Selection.newBuilder()
                    .setKey(Key.newBuilder().setInt64Key(4_198_190L).build())
                    .setName("Appendectomy")
                    .setEntityGroup("procedurePerson")
                    .build())
            .build();
    selectionData = new SelectionData("condition", serializeToJson(data));
    dataFeatureOutputs = filterBuilder.buildForDataFeature(underlay, List.of(selectionData));
    assertEquals(2, dataFeatureOutputs.size());
    EntityFilter expectedCriteriaSubFilter1 =
        new HierarchyHasAncestorFilter(
            underlay,
            underlay.getEntity("condition"),
            underlay.getEntity("condition").getHierarchy(Hierarchy.DEFAULT_NAME),
            Literal.forInt64(201_826L));
    EntityFilter expectedDataFeatureFilter1 =
        new OccurrenceForPrimaryFilter(
            underlay,
            (CriteriaOccurrence) underlay.getEntityGroup("conditionPerson"),
            underlay.getEntity("conditionOccurrence"),
            null,
            expectedCriteriaSubFilter1);
    EntityOutput expectedDataFeatureOutput1 =
        EntityOutput.filtered(
            underlay.getEntity("conditionOccurrence"), expectedDataFeatureFilter1);
    EntityFilter expectedCriteriaSubFilter2 =
        new HierarchyHasAncestorFilter(
            underlay,
            underlay.getEntity("procedure"),
            underlay.getEntity("procedure").getHierarchy(Hierarchy.DEFAULT_NAME),
            Literal.forInt64(4_198_190L));
    EntityFilter expectedDataFeatureFilter2 =
        new OccurrenceForPrimaryFilter(
            underlay,
            (CriteriaOccurrence) underlay.getEntityGroup("procedurePerson"),
            underlay.getEntity("procedureOccurrence"),
            null,
            expectedCriteriaSubFilter2);
    EntityOutput expectedDataFeatureOutput2 =
        EntityOutput.filtered(
            underlay.getEntity("procedureOccurrence"), expectedDataFeatureFilter2);
    assertEquals(
        List.of(expectedDataFeatureOutput1, expectedDataFeatureOutput2), dataFeatureOutputs);
  }

  @Test
  void criteriaOnlyMultipleOccurrencesDataFeatureFilter() {
    CFEntityGroup.EntityGroup config = CFEntityGroup.EntityGroup.newBuilder().build();
    CriteriaSelector criteriaSelector =
        new CriteriaSelector(
            "icd9cm",
            true,
            true,
            "core.EntityGroupFilterBuilder",
            SZCorePlugin.ENTITY_GROUP.getIdInConfig(),
            serializeToJson(config),
            List.of());
    EntityGroupFilterBuilder filterBuilder = new EntityGroupFilterBuilder(criteriaSelector);

    // Single id.
    DTEntityGroup.EntityGroup data =
        DTEntityGroup.EntityGroup.newBuilder()
            .addSelected(
                DTEntityGroup.EntityGroup.Selection.newBuilder()
                    .setKey(Key.newBuilder().setInt64Key(44_833_365L).build())
                    .setName("Diabetes mellitus")
                    .setEntityGroup("icd9cmPerson")
                    .build())
            .build();
    SelectionData selectionData = new SelectionData("icd9cm", serializeToJson(data));
    List<EntityOutput> dataFeatureOutputs =
        filterBuilder.buildForDataFeature(underlay, List.of(selectionData));
    assertEquals(4, dataFeatureOutputs.size());
    EntityFilter expectedCriteriaSubFilterSingleId =
        new HierarchyHasAncestorFilter(
            underlay,
            underlay.getEntity("icd9cm"),
            underlay.getEntity("icd9cm").getHierarchy(Hierarchy.DEFAULT_NAME),
            Literal.forInt64(44_833_365L));
    CriteriaOccurrence criteriaOccurrence =
        (CriteriaOccurrence) underlay.getEntityGroup("icd9cmPerson");
    List<EntityOutput> expectedDataFeatureOutputs =
        criteriaOccurrence.getOccurrenceEntities().stream()
            .sorted(Comparator.comparing(Entity::getName))
            .map(
                occurrenceEntity ->
                    EntityOutput.filtered(
                        occurrenceEntity,
                        new OccurrenceForPrimaryFilter(
                            underlay,
                            criteriaOccurrence,
                            occurrenceEntity,
                            null,
                            expectedCriteriaSubFilterSingleId)))
            .collect(Collectors.toList());
    assertEquals(expectedDataFeatureOutputs, dataFeatureOutputs);

    // Multiple ids, same entity group.
    data =
        DTEntityGroup.EntityGroup.newBuilder()
            .addSelected(
                DTEntityGroup.EntityGroup.Selection.newBuilder()
                    .setKey(Key.newBuilder().setInt64Key(44_833_365L).build())
                    .setName("Diabetes mellitus")
                    .setEntityGroup("icd9cmPerson")
                    .build())
            .addSelected(
                DTEntityGroup.EntityGroup.Selection.newBuilder()
                    .setKey(Key.newBuilder().setInt64Key(44_833_556L).build())
                    .setName("Essential hypertension")
                    .setEntityGroup("icd9cmPerson")
                    .build())
            .build();
    selectionData = new SelectionData("icd9cm", serializeToJson(data));
    dataFeatureOutputs = filterBuilder.buildForDataFeature(underlay, List.of(selectionData));
    assertEquals(4, dataFeatureOutputs.size());
    EntityFilter expectedCriteriaSubFilterMultipleIdsSameGroup =
        new HierarchyHasAncestorFilter(
            underlay,
            underlay.getEntity("icd9cm"),
            underlay.getEntity("icd9cm").getHierarchy(Hierarchy.DEFAULT_NAME),
            List.of(Literal.forInt64(44_833_365L), Literal.forInt64(44_833_556L)));
    expectedDataFeatureOutputs =
        criteriaOccurrence.getOccurrenceEntities().stream()
            .sorted(Comparator.comparing(Entity::getName))
            .map(
                occurrenceEntity ->
                    EntityOutput.filtered(
                        occurrenceEntity,
                        new OccurrenceForPrimaryFilter(
                            underlay,
                            criteriaOccurrence,
                            occurrenceEntity,
                            null,
                            expectedCriteriaSubFilterMultipleIdsSameGroup)))
            .collect(Collectors.toList());
    assertEquals(expectedDataFeatureOutputs, dataFeatureOutputs);

    // Multiple ids, different entity groups.
    data =
        DTEntityGroup.EntityGroup.newBuilder()
            .addSelected(
                DTEntityGroup.EntityGroup.Selection.newBuilder()
                    .setKey(Key.newBuilder().setInt64Key(44_833_365L).build())
                    .setName("Diabetes mellitus")
                    .setEntityGroup("icd9cmPerson")
                    .build())
            .addSelected(
                DTEntityGroup.EntityGroup.Selection.newBuilder()
                    .setKey(Key.newBuilder().setInt64Key(2_002_907L).build())
                    .setName("Appendectomy")
                    .setEntityGroup("icd9procPerson")
                    .build())
            .build();
    selectionData = new SelectionData("icd9cm_icd9proc", serializeToJson(data));
    List<EntityOutput> dataFeatureOutputsMultipleEntityGroups =
        filterBuilder.buildForDataFeature(underlay, List.of(selectionData));
    assertEquals(5, dataFeatureOutputsMultipleEntityGroups.size());

    CriteriaOccurrence criteriaOccurrenceGroup1 =
        (CriteriaOccurrence) underlay.getEntityGroup("icd9cmPerson");
    CriteriaOccurrence criteriaOccurrenceGroup2 =
        (CriteriaOccurrence) underlay.getEntityGroup("icd9procPerson");

    criteriaOccurrenceGroup1
        .getOccurrenceEntities()
        .forEach(
            occurrenceEntity ->
                assertTrue(
                    dataFeatureOutputsMultipleEntityGroups.stream()
                        .anyMatch(
                            entityOutput -> entityOutput.getEntity().equals(occurrenceEntity))));
    criteriaOccurrenceGroup2
        .getOccurrenceEntities()
        .forEach(
            occurrenceEntity ->
                assertTrue(
                    dataFeatureOutputsMultipleEntityGroups.stream()
                        .anyMatch(
                            entityOutput -> entityOutput.getEntity().equals(occurrenceEntity))));
  }

  @Test
  void emptyCriteriaDataFeatureFilter() {
    CFEntityGroup.EntityGroup config =
        CFEntityGroup.EntityGroup.newBuilder()
            .addClassificationEntityGroups(
                CFEntityGroup.EntityGroup.EntityGroupConfig.newBuilder()
                    .setId("icd9procPerson")
                    .build())
            .build();
    CriteriaSelector criteriaSelector =
        new CriteriaSelector(
            "icd9proc",
            true,
            true,
            "core.EntityGroupFilterBuilder",
            SZCorePlugin.ENTITY_GROUP.getIdInConfig(),
            serializeToJson(config),
            List.of());
    EntityGroupFilterBuilder filterBuilder = new EntityGroupFilterBuilder(criteriaSelector);
    EntityOutput expectedEntityOutput1 =
        EntityOutput.unfiltered(underlay.getEntity("ingredientOccurrence"));
    EntityOutput expectedEntityOutput2 =
        EntityOutput.unfiltered(underlay.getEntity("procedureOccurrence"));

    // Null selection data.
    SelectionData selectionData = new SelectionData("icd9proc", null);
    List<EntityOutput> dataFeatureOutputs =
        filterBuilder.buildForDataFeature(underlay, List.of(selectionData));
    assertEquals(2, dataFeatureOutputs.size());
    assertTrue(dataFeatureOutputs.contains(expectedEntityOutput1));
    assertTrue(dataFeatureOutputs.contains(expectedEntityOutput2));

    // Empty string selection data.
    selectionData = new SelectionData("icd9proc", "");
    dataFeatureOutputs = filterBuilder.buildForDataFeature(underlay, List.of(selectionData));
    assertEquals(2, dataFeatureOutputs.size());
    assertTrue(dataFeatureOutputs.contains(expectedEntityOutput1));
    assertTrue(dataFeatureOutputs.contains(expectedEntityOutput2));
  }
}

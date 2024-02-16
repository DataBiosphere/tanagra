package bio.terra.tanagra.filterbuilder;

import static bio.terra.tanagra.utils.ProtobufUtils.serializeToJson;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import bio.terra.tanagra.api.filter.AttributeFilter;
import bio.terra.tanagra.api.filter.EntityFilter;
import bio.terra.tanagra.api.filter.HierarchyHasAncestorFilter;
import bio.terra.tanagra.api.filter.PrimaryWithCriteriaFilter;
import bio.terra.tanagra.api.shared.BinaryOperator;
import bio.terra.tanagra.api.shared.Literal;
import bio.terra.tanagra.api.shared.NaryOperator;
import bio.terra.tanagra.filterbuilder.impl.core.EntityGroupFilterBuilder;
import bio.terra.tanagra.proto.criteriaselector.DataRangeOuterClass.DataRange;
import bio.terra.tanagra.proto.criteriaselector.KeyOuterClass.Key;
import bio.terra.tanagra.proto.criteriaselector.ValueOuterClass.Value;
import bio.terra.tanagra.proto.criteriaselector.configschema.CFPlaceholder;
import bio.terra.tanagra.proto.criteriaselector.dataschema.DTAttribute;
import bio.terra.tanagra.proto.criteriaselector.dataschema.DTEntityGroup;
import bio.terra.tanagra.proto.criteriaselector.dataschema.DTUnhintedValue;
import bio.terra.tanagra.underlay.ConfigReader;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.entitymodel.Hierarchy;
import bio.terra.tanagra.underlay.entitymodel.entitygroup.CriteriaOccurrence;
import bio.terra.tanagra.underlay.serialization.SZService;
import bio.terra.tanagra.underlay.serialization.SZUnderlay;
import bio.terra.tanagra.underlay.uiplugin.CriteriaSelector;
import bio.terra.tanagra.underlay.uiplugin.SelectionData;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class EntityGroupFilterBuilderTest {
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
    CFPlaceholder.Placeholder config = CFPlaceholder.Placeholder.newBuilder().build();
    CriteriaSelector criteriaSelector =
        new CriteriaSelector(
            "condition",
            true,
            true,
            "core.EntityGroupFilterBuilder",
            "core/entityGroup",
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
  }

  @Test
  void criteriaWithAttrModifiersCohortFilter() {
    CFPlaceholder.Placeholder ageAtOccurrenceConfig =
        CFPlaceholder.Placeholder.newBuilder().setAttribute("age_at_occurrence").build();
    CriteriaSelector.Modifier ageAtOccurrenceModifier =
        new CriteriaSelector.Modifier(
            "age_at_occurrence", "core/attribute", serializeToJson(ageAtOccurrenceConfig));
    CFPlaceholder.Placeholder visitTypeConfig =
        CFPlaceholder.Placeholder.newBuilder().setAttribute("visit_type").build();
    CriteriaSelector.Modifier visitTypeModifier =
        new CriteriaSelector.Modifier(
            "visit_type", "core/attribute", serializeToJson(visitTypeConfig));
    CFPlaceholder.Placeholder conditionConfig = CFPlaceholder.Placeholder.newBuilder().build();
    CriteriaSelector criteriaSelector =
        new CriteriaSelector(
            "condition",
            true,
            true,
            "core.EntityGroupFilterBuilder",
            "core/entityGroup",
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
    CFPlaceholder.Placeholder groupByConfig =
        CFPlaceholder.Placeholder.newBuilder()
            .putGroupByAttributesPerOccurrenceEntity(
                "conditionOccurrence",
                CFPlaceholder.Placeholder.GroupByAttributes.newBuilder()
                    .addAttribute("start_date")
                    .build())
            .build();
    CriteriaSelector.Modifier groupByModifier =
        new CriteriaSelector.Modifier(
            "group_by_count", "core/unhinted-value", serializeToJson(groupByConfig));
    CFPlaceholder.Placeholder conditionConfig = CFPlaceholder.Placeholder.newBuilder().build();
    CriteriaSelector criteriaSelector =
        new CriteriaSelector(
            "condition",
            true,
            true,
            "core.EntityGroupFilterBuilder",
            "core/entityGroup",
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
  void criteriaWithAttrAndGroupByModifiersCohortFilter() {
    CFPlaceholder.Placeholder ageAtOccurrenceConfig =
        CFPlaceholder.Placeholder.newBuilder().setAttribute("age_at_occurrence").build();
    CriteriaSelector.Modifier ageAtOccurrenceModifier =
        new CriteriaSelector.Modifier(
            "age_at_occurrence", "core/attribute", serializeToJson(ageAtOccurrenceConfig));
    CFPlaceholder.Placeholder visitTypeConfig =
        CFPlaceholder.Placeholder.newBuilder().setAttribute("visit_type").build();
    CriteriaSelector.Modifier visitTypeModifier =
        new CriteriaSelector.Modifier(
            "visit_type", "core/attribute", serializeToJson(visitTypeConfig));
    CFPlaceholder.Placeholder groupByConfig =
        CFPlaceholder.Placeholder.newBuilder()
            .putGroupByAttributesPerOccurrenceEntity(
                "conditionOccurrence",
                CFPlaceholder.Placeholder.GroupByAttributes.newBuilder()
                    .addAttribute("start_date")
                    .build())
            .build();
    CriteriaSelector.Modifier groupByModifier =
        new CriteriaSelector.Modifier(
            "group_by_count", "core/unhinted-value", serializeToJson(groupByConfig));
    CFPlaceholder.Placeholder conditionConfig = CFPlaceholder.Placeholder.newBuilder().build();
    CriteriaSelector criteriaSelector =
        new CriteriaSelector(
            "condition",
            true,
            true,
            "core.EntityGroupFilterBuilder",
            "core/entityGroup",
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
                    .setKey(Key.newBuilder().setInt64Key(201_826L).build())
                    .setName("Type 2 diabetes mellitus")
                    .setEntityGroup("conditionPerson")
                    .build())
            .build();
    SelectionData entityGroupSelectionData =
        new SelectionData("condition", serializeToJson(entityGroupData));
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
    EntityFilter expectedVisitTypeSubFilter =
        new AttributeFilter(
            underlay,
            underlay.getEntity("conditionOccurrence"),
            underlay.getEntity("conditionOccurrence").getAttribute("visit_type"),
            BinaryOperator.EQUALS,
            Literal.forInt64(9_202L));
    EntityFilter expectedCohortFilter =
        new PrimaryWithCriteriaFilter(
            underlay,
            (CriteriaOccurrence) underlay.getEntityGroup("conditionPerson"),
            expectedCriteriaSubFilter,
            Map.of(
                underlay.getEntity("conditionOccurrence"),
                List.of(expectedAgeAtOccurrenceSubFilter, expectedVisitTypeSubFilter)),
            Map.of(
                underlay.getEntity("conditionOccurrence"),
                List.of(underlay.getEntity("conditionOccurrence").getAttribute("start_date"))),
            BinaryOperator.GREATER_THAN_OR_EQUAL,
            2);
    assertEquals(expectedCohortFilter, cohortFilter);
  }
}

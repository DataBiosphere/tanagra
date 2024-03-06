package bio.terra.tanagra.filterbuilder;

import static bio.terra.tanagra.utils.ProtobufUtils.serializeToJson;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import bio.terra.tanagra.api.filter.AttributeFilter;
import bio.terra.tanagra.api.filter.BooleanAndOrFilter;
import bio.terra.tanagra.api.filter.EntityFilter;
import bio.terra.tanagra.api.filter.GroupHasItemsFilter;
import bio.terra.tanagra.api.shared.BinaryOperator;
import bio.terra.tanagra.api.shared.Literal;
import bio.terra.tanagra.api.shared.NaryOperator;
import bio.terra.tanagra.filterbuilder.impl.core.MultiAttributeFilterBuilder;
import bio.terra.tanagra.proto.criteriaselector.DataRangeOuterClass.DataRange;
import bio.terra.tanagra.proto.criteriaselector.ValueDataOuterClass.ValueData;
import bio.terra.tanagra.proto.criteriaselector.ValueOuterClass.Value;
import bio.terra.tanagra.proto.criteriaselector.configschema.CFPlaceholder;
import bio.terra.tanagra.proto.criteriaselector.dataschema.DTAttribute;
import bio.terra.tanagra.proto.criteriaselector.dataschema.DTMultiAttribute;
import bio.terra.tanagra.proto.criteriaselector.dataschema.DTUnhintedValue;
import bio.terra.tanagra.underlay.ConfigReader;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.entitymodel.entitygroup.GroupItems;
import bio.terra.tanagra.underlay.serialization.SZCorePlugin;
import bio.terra.tanagra.underlay.serialization.SZService;
import bio.terra.tanagra.underlay.serialization.SZUnderlay;
import bio.terra.tanagra.underlay.uiplugin.CriteriaSelector;
import bio.terra.tanagra.underlay.uiplugin.SelectionData;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class MultiAttributeFilterBuilderTest {
  private Underlay underlay;

  @BeforeEach
  void setup() {
    ConfigReader configReader = ConfigReader.fromJarResources();
    SZService szService = configReader.readService("sd20230831_verily");
    SZUnderlay szUnderlay = configReader.readUnderlay(szService.underlay);
    underlay = Underlay.fromConfig(szService.bigQuery, szUnderlay, configReader);
  }

  @Test
  void noModifiersCohortFilter() {
    CFPlaceholder.Placeholder config =
        CFPlaceholder.Placeholder.newBuilder()
            .setEntityGroupMultiAttr("bloodPressurePerson")
            .build();
    CriteriaSelector criteriaSelector =
        new CriteriaSelector(
            "bloodPressure",
            true,
            true,
            "core.MultiAttributeFilterBuilder",
            SZCorePlugin.MULTI_ATTRIBUTE.getIdInConfig(),
            serializeToJson(config),
            List.of());
    MultiAttributeFilterBuilder filterBuilder = new MultiAttributeFilterBuilder(criteriaSelector);

    // Single attribute.
    DTMultiAttribute.MultiAttribute data =
        DTMultiAttribute.MultiAttribute.newBuilder()
            .addValueData(
                ValueData.newBuilder()
                    .setAttribute("systolic")
                    .setRange(DataRange.newBuilder().setMin(100).setMax(120).build())
                    .build())
            .build();
    SelectionData selectionData = new SelectionData("bloodPressure", serializeToJson(data));
    EntityFilter cohortFilter = filterBuilder.buildForCohort(underlay, List.of(selectionData));
    assertNotNull(cohortFilter);
    EntityFilter itemsSubFilter =
        new AttributeFilter(
            underlay,
            underlay.getEntity("bloodPressure"),
            underlay.getEntity("bloodPressure").getAttribute("systolic"),
            NaryOperator.BETWEEN,
            List.of(Literal.forDouble(100.0), Literal.forDouble(120.0)));
    EntityFilter expectedCohortFilter =
        new GroupHasItemsFilter(
            underlay,
            (GroupItems) underlay.getEntityGroup("bloodPressurePerson"),
            itemsSubFilter,
            null,
            null,
            null);
    assertEquals(expectedCohortFilter, cohortFilter);

    // Multiple attributes.
    data =
        DTMultiAttribute.MultiAttribute.newBuilder()
            .addValueData(
                ValueData.newBuilder()
                    .setAttribute("systolic")
                    .setRange(DataRange.newBuilder().setMin(100).setMax(120).build())
                    .build())
            .addValueData(
                ValueData.newBuilder()
                    .setAttribute("status_code")
                    .addSelected(
                        ValueData.Selection.newBuilder()
                            .setValue(Value.newBuilder().setInt64Value(3L).build())
                            .build())
                    .build())
            .build();
    selectionData = new SelectionData("bloodPressure", serializeToJson(data));
    cohortFilter = filterBuilder.buildForCohort(underlay, List.of(selectionData));
    assertNotNull(cohortFilter);
    itemsSubFilter =
        new BooleanAndOrFilter(
            BooleanAndOrFilter.LogicalOperator.AND,
            List.of(
                new AttributeFilter(
                    underlay,
                    underlay.getEntity("bloodPressure"),
                    underlay.getEntity("bloodPressure").getAttribute("systolic"),
                    NaryOperator.BETWEEN,
                    List.of(Literal.forDouble(100.0), Literal.forDouble(120.0))),
                new AttributeFilter(
                    underlay,
                    underlay.getEntity("bloodPressure"),
                    underlay.getEntity("bloodPressure").getAttribute("status_code"),
                    BinaryOperator.EQUALS,
                    Literal.forInt64(3L))));
    expectedCohortFilter =
        new GroupHasItemsFilter(
            underlay,
            (GroupItems) underlay.getEntityGroup("bloodPressurePerson"),
            itemsSubFilter,
            null,
            null,
            null);
    assertEquals(expectedCohortFilter, cohortFilter);
  }

  @Test
  void withAttrModifiersCohortFilter() {
    CFPlaceholder.Placeholder mainConfig =
        CFPlaceholder.Placeholder.newBuilder()
            .setEntityGroupMultiAttr("bloodPressurePerson")
            .build();
    CFPlaceholder.Placeholder ageAtOccurrenceConfig =
        CFPlaceholder.Placeholder.newBuilder().setAttribute("age_at_occurrence").build();
    CriteriaSelector.Modifier ageAtOccurrenceModifier =
        new CriteriaSelector.Modifier(
            "age_at_occurrence",
            SZCorePlugin.ATTRIBUTE.getIdInConfig(),
            serializeToJson(ageAtOccurrenceConfig));
    CFPlaceholder.Placeholder visitTypeConfig =
        CFPlaceholder.Placeholder.newBuilder().setAttribute("visit_type").build();
    CriteriaSelector.Modifier visitTypeModifier =
        new CriteriaSelector.Modifier(
            "visit_type", SZCorePlugin.ATTRIBUTE.getIdInConfig(), serializeToJson(visitTypeConfig));
    CriteriaSelector criteriaSelector =
        new CriteriaSelector(
            "bloodPressure",
            true,
            true,
            "core.MultiAttributeFilterBuilder",
            SZCorePlugin.MULTI_ATTRIBUTE.getIdInConfig(),
            serializeToJson(mainConfig),
            List.of(ageAtOccurrenceModifier, visitTypeModifier));
    MultiAttributeFilterBuilder filterBuilder = new MultiAttributeFilterBuilder(criteriaSelector);

    // Single attribute.
    DTMultiAttribute.MultiAttribute data =
        DTMultiAttribute.MultiAttribute.newBuilder()
            .addValueData(
                ValueData.newBuilder()
                    .setAttribute("systolic")
                    .setRange(DataRange.newBuilder().setMin(100).setMax(120).build())
                    .build())
            .build();
    SelectionData selectionData = new SelectionData("bloodPressure", serializeToJson(data));
    DTAttribute.Attribute ageAtOccurrenceData =
        DTAttribute.Attribute.newBuilder()
            .addDataRanges(DataRange.newBuilder().setMin(25.0).setMax(45.0).build())
            .build();
    SelectionData ageAtOccurrenceSelectionData =
        new SelectionData("age_at_occurrence", serializeToJson(ageAtOccurrenceData));
    EntityFilter cohortFilter =
        filterBuilder.buildForCohort(
            underlay, List.of(selectionData, ageAtOccurrenceSelectionData));
    assertNotNull(cohortFilter);
    EntityFilter itemsSubFilter =
        new BooleanAndOrFilter(
            BooleanAndOrFilter.LogicalOperator.AND,
            List.of(
                new AttributeFilter(
                    underlay,
                    underlay.getEntity("bloodPressure"),
                    underlay.getEntity("bloodPressure").getAttribute("systolic"),
                    NaryOperator.BETWEEN,
                    List.of(Literal.forDouble(100.0), Literal.forDouble(120.0))),
                new AttributeFilter(
                    underlay,
                    underlay.getEntity("bloodPressure"),
                    underlay.getEntity("bloodPressure").getAttribute("age_at_occurrence"),
                    NaryOperator.BETWEEN,
                    List.of(Literal.forDouble(25.0), Literal.forDouble(45.0)))));
    EntityFilter expectedCohortFilter =
        new GroupHasItemsFilter(
            underlay,
            (GroupItems) underlay.getEntityGroup("bloodPressurePerson"),
            itemsSubFilter,
            null,
            null,
            null);
    assertEquals(expectedCohortFilter, cohortFilter);

    // Multiple attributes.
    data =
        DTMultiAttribute.MultiAttribute.newBuilder()
            .addValueData(
                ValueData.newBuilder()
                    .setAttribute("systolic")
                    .setRange(DataRange.newBuilder().setMin(100).setMax(120).build())
                    .build())
            .addValueData(
                ValueData.newBuilder()
                    .setAttribute("status_code")
                    .addSelected(
                        ValueData.Selection.newBuilder()
                            .setValue(Value.newBuilder().setInt64Value(3L).build())
                            .build())
                    .build())
            .build();
    selectionData = new SelectionData("bloodPressure", serializeToJson(data));
    DTAttribute.Attribute visitTypeData =
        DTAttribute.Attribute.newBuilder()
            .addSelected(
                DTAttribute.Attribute.Selection.newBuilder()
                    .setValue(Value.newBuilder().setInt64Value(8_870L).build())
                    .build())
            .build();
    SelectionData visitTypeSelectionData =
        new SelectionData("visit_type", serializeToJson(visitTypeData));
    cohortFilter =
        filterBuilder.buildForCohort(
            underlay, List.of(selectionData, ageAtOccurrenceSelectionData, visitTypeSelectionData));
    assertNotNull(cohortFilter);
    itemsSubFilter =
        new BooleanAndOrFilter(
            BooleanAndOrFilter.LogicalOperator.AND,
            List.of(
                new AttributeFilter(
                    underlay,
                    underlay.getEntity("bloodPressure"),
                    underlay.getEntity("bloodPressure").getAttribute("systolic"),
                    NaryOperator.BETWEEN,
                    List.of(Literal.forDouble(100.0), Literal.forDouble(120.0))),
                new AttributeFilter(
                    underlay,
                    underlay.getEntity("bloodPressure"),
                    underlay.getEntity("bloodPressure").getAttribute("status_code"),
                    BinaryOperator.EQUALS,
                    Literal.forInt64(3L)),
                new AttributeFilter(
                    underlay,
                    underlay.getEntity("bloodPressure"),
                    underlay.getEntity("bloodPressure").getAttribute("age_at_occurrence"),
                    NaryOperator.BETWEEN,
                    List.of(Literal.forDouble(25.0), Literal.forDouble(45.0))),
                new AttributeFilter(
                    underlay,
                    underlay.getEntity("bloodPressure"),
                    underlay.getEntity("bloodPressure").getAttribute("visit_type"),
                    BinaryOperator.EQUALS,
                    Literal.forInt64(8_870L))));
    expectedCohortFilter =
        new GroupHasItemsFilter(
            underlay,
            (GroupItems) underlay.getEntityGroup("bloodPressurePerson"),
            itemsSubFilter,
            null,
            null,
            null);
    assertEquals(expectedCohortFilter, cohortFilter);
  }

  @Test
  void withGroupByModifierCohortFilter() {
    CFPlaceholder.Placeholder mainConfig =
        CFPlaceholder.Placeholder.newBuilder()
            .setEntityGroupMultiAttr("bloodPressurePerson")
            .build();
    CFPlaceholder.Placeholder groupByConfig =
        CFPlaceholder.Placeholder.newBuilder()
            .putGroupByAttributesPerOccurrenceEntity(
                "bloodPressure",
                CFPlaceholder.Placeholder.GroupByAttributes.newBuilder()
                    .addAttribute("date")
                    .build())
            .build();
    CriteriaSelector.Modifier groupByModifier =
        new CriteriaSelector.Modifier(
            "group_by_count",
            SZCorePlugin.UNHINTED_VALUE.getIdInConfig(),
            serializeToJson(groupByConfig));
    CriteriaSelector criteriaSelector =
        new CriteriaSelector(
            "bloodPressure",
            true,
            true,
            "core.MultiAttributeFilterBuilder",
            SZCorePlugin.MULTI_ATTRIBUTE.getIdInConfig(),
            serializeToJson(mainConfig),
            List.of(groupByModifier));
    MultiAttributeFilterBuilder filterBuilder = new MultiAttributeFilterBuilder(criteriaSelector);

    // Single attribute.
    DTMultiAttribute.MultiAttribute data =
        DTMultiAttribute.MultiAttribute.newBuilder()
            .addValueData(
                ValueData.newBuilder()
                    .setAttribute("systolic")
                    .setRange(DataRange.newBuilder().setMin(100).setMax(120).build())
                    .build())
            .build();
    SelectionData selectionData = new SelectionData("bloodPressure", serializeToJson(data));
    DTUnhintedValue.UnhintedValue groupByData =
        DTUnhintedValue.UnhintedValue.newBuilder()
            .setOperator(
                DTUnhintedValue.UnhintedValue.ComparisonOperator
                    .COMPARISON_OPERATOR_GREATER_THAN_EQUAL)
            .setMin(2.0)
            .build();
    SelectionData groupBySelectionData =
        new SelectionData("group_by_count", serializeToJson(groupByData));
    EntityFilter cohortFilter =
        filterBuilder.buildForCohort(underlay, List.of(selectionData, groupBySelectionData));
    assertNotNull(cohortFilter);
    EntityFilter itemsSubFilter =
        new AttributeFilter(
            underlay,
            underlay.getEntity("bloodPressure"),
            underlay.getEntity("bloodPressure").getAttribute("systolic"),
            NaryOperator.BETWEEN,
            List.of(Literal.forDouble(100.0), Literal.forDouble(120.0)));
    EntityFilter expectedCohortFilter =
        new GroupHasItemsFilter(
            underlay,
            (GroupItems) underlay.getEntityGroup("bloodPressurePerson"),
            itemsSubFilter,
            underlay.getEntity("bloodPressure").getAttribute("date"),
            BinaryOperator.GREATER_THAN_OR_EQUAL,
            2);
    assertEquals(expectedCohortFilter, cohortFilter);

    // Multiple attributes.
    data =
        DTMultiAttribute.MultiAttribute.newBuilder()
            .addValueData(
                ValueData.newBuilder()
                    .setAttribute("systolic")
                    .setRange(DataRange.newBuilder().setMin(100).setMax(120).build())
                    .build())
            .addValueData(
                ValueData.newBuilder()
                    .setAttribute("status_code")
                    .addSelected(
                        ValueData.Selection.newBuilder()
                            .setValue(Value.newBuilder().setInt64Value(3L).build())
                            .build())
                    .build())
            .build();
    selectionData = new SelectionData("bloodPressure", serializeToJson(data));
    cohortFilter =
        filterBuilder.buildForCohort(underlay, List.of(selectionData, groupBySelectionData));
    assertNotNull(cohortFilter);
    itemsSubFilter =
        new BooleanAndOrFilter(
            BooleanAndOrFilter.LogicalOperator.AND,
            List.of(
                new AttributeFilter(
                    underlay,
                    underlay.getEntity("bloodPressure"),
                    underlay.getEntity("bloodPressure").getAttribute("systolic"),
                    NaryOperator.BETWEEN,
                    List.of(Literal.forDouble(100.0), Literal.forDouble(120.0))),
                new AttributeFilter(
                    underlay,
                    underlay.getEntity("bloodPressure"),
                    underlay.getEntity("bloodPressure").getAttribute("status_code"),
                    BinaryOperator.EQUALS,
                    Literal.forInt64(3L))));
    expectedCohortFilter =
        new GroupHasItemsFilter(
            underlay,
            (GroupItems) underlay.getEntityGroup("bloodPressurePerson"),
            itemsSubFilter,
            underlay.getEntity("bloodPressure").getAttribute("date"),
            BinaryOperator.GREATER_THAN_OR_EQUAL,
            2);
    assertEquals(expectedCohortFilter, cohortFilter);
  }

  @Test
  void withAttrAndGroupByModifiersCohortFilter() {
    CFPlaceholder.Placeholder mainConfig =
        CFPlaceholder.Placeholder.newBuilder()
            .setEntityGroupMultiAttr("bloodPressurePerson")
            .build();
    CFPlaceholder.Placeholder ageAtOccurrenceConfig =
        CFPlaceholder.Placeholder.newBuilder().setAttribute("age_at_occurrence").build();
    CriteriaSelector.Modifier ageAtOccurrenceModifier =
        new CriteriaSelector.Modifier(
            "age_at_occurrence",
            SZCorePlugin.ATTRIBUTE.getIdInConfig(),
            serializeToJson(ageAtOccurrenceConfig));
    CFPlaceholder.Placeholder visitTypeConfig =
        CFPlaceholder.Placeholder.newBuilder().setAttribute("visit_type").build();
    CriteriaSelector.Modifier visitTypeModifier =
        new CriteriaSelector.Modifier(
            "visit_type", SZCorePlugin.ATTRIBUTE.getIdInConfig(), serializeToJson(visitTypeConfig));
    CFPlaceholder.Placeholder groupByConfig =
        CFPlaceholder.Placeholder.newBuilder()
            .putGroupByAttributesPerOccurrenceEntity(
                "bloodPressure",
                CFPlaceholder.Placeholder.GroupByAttributes.newBuilder()
                    .addAttribute("date")
                    .build())
            .build();
    CriteriaSelector.Modifier groupByModifier =
        new CriteriaSelector.Modifier(
            "group_by_count",
            SZCorePlugin.UNHINTED_VALUE.getIdInConfig(),
            serializeToJson(groupByConfig));
    CriteriaSelector criteriaSelector =
        new CriteriaSelector(
            "bloodPressure",
            true,
            true,
            "core.MultiAttributeFilterBuilder",
            SZCorePlugin.MULTI_ATTRIBUTE.getIdInConfig(),
            serializeToJson(mainConfig),
            List.of(ageAtOccurrenceModifier, visitTypeModifier, groupByModifier));
    MultiAttributeFilterBuilder filterBuilder = new MultiAttributeFilterBuilder(criteriaSelector);

    // Single attribute.
    DTMultiAttribute.MultiAttribute data =
        DTMultiAttribute.MultiAttribute.newBuilder()
            .addValueData(
                ValueData.newBuilder()
                    .setAttribute("systolic")
                    .setRange(DataRange.newBuilder().setMin(100).setMax(120).build())
                    .build())
            .build();
    SelectionData selectionData = new SelectionData("bloodPressure", serializeToJson(data));
    DTAttribute.Attribute ageAtOccurrenceData =
        DTAttribute.Attribute.newBuilder()
            .addDataRanges(DataRange.newBuilder().setMin(25.0).setMax(45.0).build())
            .build();
    SelectionData ageAtOccurrenceSelectionData =
        new SelectionData("age_at_occurrence", serializeToJson(ageAtOccurrenceData));
    DTUnhintedValue.UnhintedValue groupByData =
        DTUnhintedValue.UnhintedValue.newBuilder()
            .setOperator(
                DTUnhintedValue.UnhintedValue.ComparisonOperator
                    .COMPARISON_OPERATOR_GREATER_THAN_EQUAL)
            .setMin(2.0)
            .build();
    SelectionData groupBySelectionData =
        new SelectionData("group_by_count", serializeToJson(groupByData));
    EntityFilter cohortFilter =
        filterBuilder.buildForCohort(
            underlay, List.of(selectionData, ageAtOccurrenceSelectionData, groupBySelectionData));
    assertNotNull(cohortFilter);
    EntityFilter itemsSubFilter =
        new BooleanAndOrFilter(
            BooleanAndOrFilter.LogicalOperator.AND,
            List.of(
                new AttributeFilter(
                    underlay,
                    underlay.getEntity("bloodPressure"),
                    underlay.getEntity("bloodPressure").getAttribute("systolic"),
                    NaryOperator.BETWEEN,
                    List.of(Literal.forDouble(100.0), Literal.forDouble(120.0))),
                new AttributeFilter(
                    underlay,
                    underlay.getEntity("bloodPressure"),
                    underlay.getEntity("bloodPressure").getAttribute("age_at_occurrence"),
                    NaryOperator.BETWEEN,
                    List.of(Literal.forDouble(25.0), Literal.forDouble(45.0)))));
    EntityFilter expectedCohortFilter =
        new GroupHasItemsFilter(
            underlay,
            (GroupItems) underlay.getEntityGroup("bloodPressurePerson"),
            itemsSubFilter,
            underlay.getEntity("bloodPressure").getAttribute("date"),
            BinaryOperator.GREATER_THAN_OR_EQUAL,
            2);
    assertEquals(expectedCohortFilter, cohortFilter);

    // Multiple attributes.
    data =
        DTMultiAttribute.MultiAttribute.newBuilder()
            .addValueData(
                ValueData.newBuilder()
                    .setAttribute("systolic")
                    .setRange(DataRange.newBuilder().setMin(100).setMax(120).build())
                    .build())
            .addValueData(
                ValueData.newBuilder()
                    .setAttribute("status_code")
                    .addSelected(
                        ValueData.Selection.newBuilder()
                            .setValue(Value.newBuilder().setInt64Value(3L).build())
                            .build())
                    .build())
            .build();
    selectionData = new SelectionData("bloodPressure", serializeToJson(data));
    DTAttribute.Attribute visitTypeData =
        DTAttribute.Attribute.newBuilder()
            .addSelected(
                DTAttribute.Attribute.Selection.newBuilder()
                    .setValue(Value.newBuilder().setInt64Value(8_870L).build())
                    .build())
            .build();
    SelectionData visitTypeSelectionData =
        new SelectionData("visit_type", serializeToJson(visitTypeData));
    cohortFilter =
        filterBuilder.buildForCohort(
            underlay,
            List.of(
                selectionData,
                ageAtOccurrenceSelectionData,
                visitTypeSelectionData,
                groupBySelectionData));
    assertNotNull(cohortFilter);
    itemsSubFilter =
        new BooleanAndOrFilter(
            BooleanAndOrFilter.LogicalOperator.AND,
            List.of(
                new AttributeFilter(
                    underlay,
                    underlay.getEntity("bloodPressure"),
                    underlay.getEntity("bloodPressure").getAttribute("systolic"),
                    NaryOperator.BETWEEN,
                    List.of(Literal.forDouble(100.0), Literal.forDouble(120.0))),
                new AttributeFilter(
                    underlay,
                    underlay.getEntity("bloodPressure"),
                    underlay.getEntity("bloodPressure").getAttribute("status_code"),
                    BinaryOperator.EQUALS,
                    Literal.forInt64(3L)),
                new AttributeFilter(
                    underlay,
                    underlay.getEntity("bloodPressure"),
                    underlay.getEntity("bloodPressure").getAttribute("age_at_occurrence"),
                    NaryOperator.BETWEEN,
                    List.of(Literal.forDouble(25.0), Literal.forDouble(45.0))),
                new AttributeFilter(
                    underlay,
                    underlay.getEntity("bloodPressure"),
                    underlay.getEntity("bloodPressure").getAttribute("visit_type"),
                    BinaryOperator.EQUALS,
                    Literal.forInt64(8_870L))));
    expectedCohortFilter =
        new GroupHasItemsFilter(
            underlay,
            (GroupItems) underlay.getEntityGroup("bloodPressurePerson"),
            itemsSubFilter,
            underlay.getEntity("bloodPressure").getAttribute("date"),
            BinaryOperator.GREATER_THAN_OR_EQUAL,
            2);
    assertEquals(expectedCohortFilter, cohortFilter);
  }

  @Test
  void noModifiersDataFeatureFilter() {
    CFPlaceholder.Placeholder config =
        CFPlaceholder.Placeholder.newBuilder()
            .setEntityGroupMultiAttr("bloodPressurePerson")
            .build();
    CriteriaSelector criteriaSelector =
        new CriteriaSelector(
            "bloodPressure",
            true,
            true,
            "core.MultiAttributeFilterBuilder",
            SZCorePlugin.MULTI_ATTRIBUTE.getIdInConfig(),
            serializeToJson(config),
            List.of());
    MultiAttributeFilterBuilder filterBuilder = new MultiAttributeFilterBuilder(criteriaSelector);

    // No attributes.
    DTMultiAttribute.MultiAttribute data = DTMultiAttribute.MultiAttribute.newBuilder().build();
    SelectionData selectionData = new SelectionData("bloodPressure", serializeToJson(data));
    List<EntityOutput> dataFeatureOutputs =
        filterBuilder.buildForDataFeature(underlay, List.of(selectionData));
    assertEquals(1, dataFeatureOutputs.size());
    assertEquals(
        dataFeatureOutputs.get(0), EntityOutput.unfiltered(underlay.getEntity("bloodPressure")));

    // Single attribute.
    data =
        DTMultiAttribute.MultiAttribute.newBuilder()
            .addValueData(
                ValueData.newBuilder()
                    .setAttribute("systolic")
                    .setRange(DataRange.newBuilder().setMin(100).setMax(120).build())
                    .build())
            .build();
    selectionData = new SelectionData("bloodPressure", serializeToJson(data));
    dataFeatureOutputs = filterBuilder.buildForDataFeature(underlay, List.of(selectionData));
    assertEquals(1, dataFeatureOutputs.size());
    EntityFilter expectedDataFeatureFilter =
        new AttributeFilter(
            underlay,
            underlay.getEntity("bloodPressure"),
            underlay.getEntity("bloodPressure").getAttribute("systolic"),
            NaryOperator.BETWEEN,
            List.of(Literal.forDouble(100.0), Literal.forDouble(120.0)));
    assertEquals(
        dataFeatureOutputs.get(0),
        EntityOutput.filtered(underlay.getEntity("bloodPressure"), expectedDataFeatureFilter));

    // Multiple attributes.
    data =
        DTMultiAttribute.MultiAttribute.newBuilder()
            .addValueData(
                ValueData.newBuilder()
                    .setAttribute("systolic")
                    .setRange(DataRange.newBuilder().setMin(100).setMax(120).build())
                    .build())
            .addValueData(
                ValueData.newBuilder()
                    .setAttribute("status_code")
                    .addSelected(
                        ValueData.Selection.newBuilder()
                            .setValue(Value.newBuilder().setInt64Value(3L).build())
                            .build())
                    .build())
            .build();
    selectionData = new SelectionData("bloodPressure", serializeToJson(data));
    dataFeatureOutputs = filterBuilder.buildForDataFeature(underlay, List.of(selectionData));
    assertEquals(1, dataFeatureOutputs.size());
    expectedDataFeatureFilter =
        new BooleanAndOrFilter(
            BooleanAndOrFilter.LogicalOperator.AND,
            List.of(
                new AttributeFilter(
                    underlay,
                    underlay.getEntity("bloodPressure"),
                    underlay.getEntity("bloodPressure").getAttribute("systolic"),
                    NaryOperator.BETWEEN,
                    List.of(Literal.forDouble(100.0), Literal.forDouble(120.0))),
                new AttributeFilter(
                    underlay,
                    underlay.getEntity("bloodPressure"),
                    underlay.getEntity("bloodPressure").getAttribute("status_code"),
                    BinaryOperator.EQUALS,
                    Literal.forInt64(3L))));
    assertEquals(
        dataFeatureOutputs.get(0),
        EntityOutput.filtered(underlay.getEntity("bloodPressure"), expectedDataFeatureFilter));
  }
}

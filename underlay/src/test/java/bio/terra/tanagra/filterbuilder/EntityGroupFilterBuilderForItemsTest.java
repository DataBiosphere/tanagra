package bio.terra.tanagra.filterbuilder;

import static bio.terra.tanagra.utils.ProtobufUtils.serializeToJson;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import bio.terra.tanagra.api.filter.AttributeFilter;
import bio.terra.tanagra.api.filter.EntityFilter;
import bio.terra.tanagra.api.filter.GroupHasItemsFilter;
import bio.terra.tanagra.api.shared.BinaryOperator;
import bio.terra.tanagra.api.shared.Literal;
import bio.terra.tanagra.api.shared.NaryOperator;
import bio.terra.tanagra.filterbuilder.impl.core.EntityGroupFilterBuilder;
import bio.terra.tanagra.proto.criteriaselector.DataRangeOuterClass.DataRange;
import bio.terra.tanagra.proto.criteriaselector.configschema.CFPlaceholder;
import bio.terra.tanagra.proto.criteriaselector.dataschema.DTAttribute;
import bio.terra.tanagra.proto.criteriaselector.dataschema.DTEntityGroup;
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

public class EntityGroupFilterBuilderForItemsTest {
  private Underlay underlay;

  @BeforeEach
  void setup() {
    ConfigReader configReader = ConfigReader.fromJarResources();
    SZService szService = configReader.readService("sd20230831_verily");
    SZUnderlay szUnderlay = configReader.readUnderlay(szService.underlay);
    underlay = Underlay.fromConfig(szService.bigQuery, szUnderlay, configReader);
  }

  @Test
  void criteriaWithAttrModifiersCohortFilter() {
    CFPlaceholder.Placeholder systolicConfig =
        CFPlaceholder.Placeholder.newBuilder().setAttribute("systolic").build();
    CriteriaSelector.Modifier systolicModifier =
        new CriteriaSelector.Modifier(
            "systolic", SZCorePlugin.ATTRIBUTE.getIdInConfig(), serializeToJson(systolicConfig));
    CFPlaceholder.Placeholder bloodPressureConfig = CFPlaceholder.Placeholder.newBuilder().build();
    CriteriaSelector criteriaSelector =
        new CriteriaSelector(
            "bloodPressure",
            true,
            true,
            "core.EntityGroupFilterBuilder",
            SZCorePlugin.ENTITY_GROUP.getIdInConfig(),
            serializeToJson(bloodPressureConfig),
            List.of(systolicModifier));
    EntityGroupFilterBuilder filterBuilder = new EntityGroupFilterBuilder(criteriaSelector);

    // Single attribute modifier.
    DTAttribute.Attribute systolicData =
        DTAttribute.Attribute.newBuilder()
            .addDataRanges(DataRange.newBuilder().setMin(100).setMax(120).build())
            .build();
    SelectionData systolicSelectionData =
        new SelectionData("systolic", serializeToJson(systolicData));
    DTEntityGroup.EntityGroup entityGroupData =
        DTEntityGroup.EntityGroup.newBuilder()
            .addSelected(
                DTEntityGroup.EntityGroup.Selection.newBuilder()
                    .setEntityGroup("bloodPressurePerson")
                    .build())
            .build();
    SelectionData entityGroupSelectionData =
        new SelectionData("bloodPressure", serializeToJson(entityGroupData));
    EntityFilter cohortFilter =
        filterBuilder.buildForCohort(
            underlay, List.of(entityGroupSelectionData, systolicSelectionData));
    assertNotNull(cohortFilter);
    EntityFilter expectedSystolicSubFilter =
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
            expectedSystolicSubFilter,
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
    CFPlaceholder.Placeholder bloodPressureConfig = CFPlaceholder.Placeholder.newBuilder().build();
    CriteriaSelector criteriaSelector =
        new CriteriaSelector(
            "bloodPressure",
            true,
            true,
            "core.EntityGroupFilterBuilder",
            SZCorePlugin.ENTITY_GROUP.getIdInConfig(),
            serializeToJson(bloodPressureConfig),
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
                    .setEntityGroup("bloodPressurePerson")
                    .build())
            .build();
    SelectionData entityGroupSelectionData =
        new SelectionData("bloodPressure", serializeToJson(entityGroupData));
    EntityFilter cohortFilter =
        filterBuilder.buildForCohort(
            underlay, List.of(entityGroupSelectionData, groupBySelectionData));
    assertNotNull(cohortFilter);
    EntityFilter expectedCohortFilter =
        new GroupHasItemsFilter(
            underlay,
            (GroupItems) underlay.getEntityGroup("bloodPressurePerson"),
            null,
            underlay.getEntity("bloodPressure").getAttribute("date"),
            BinaryOperator.GREATER_THAN_OR_EQUAL,
            2);
    assertEquals(expectedCohortFilter, cohortFilter);
  }

  @Test
  void criteriaWithAttrAndGroupByModifiersCohortFilter() {
    CFPlaceholder.Placeholder systolicConfig =
        CFPlaceholder.Placeholder.newBuilder().setAttribute("systolic").build();
    CriteriaSelector.Modifier systolicModifier =
        new CriteriaSelector.Modifier(
            "systolic", SZCorePlugin.ATTRIBUTE.getIdInConfig(), serializeToJson(systolicConfig));
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
    CFPlaceholder.Placeholder bloodPressureConfig = CFPlaceholder.Placeholder.newBuilder().build();
    CriteriaSelector criteriaSelector =
        new CriteriaSelector(
            "bloodPressure",
            true,
            true,
            "core.EntityGroupFilterBuilder",
            SZCorePlugin.ENTITY_GROUP.getIdInConfig(),
            serializeToJson(bloodPressureConfig),
            List.of(systolicModifier, groupByModifier));
    EntityGroupFilterBuilder filterBuilder = new EntityGroupFilterBuilder(criteriaSelector);

    DTAttribute.Attribute systolicData =
        DTAttribute.Attribute.newBuilder()
            .addDataRanges(DataRange.newBuilder().setMin(100).setMax(120).build())
            .build();
    SelectionData systolicSelectionData =
        new SelectionData("systolic", serializeToJson(systolicData));
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
                    .setEntityGroup("bloodPressurePerson")
                    .build())
            .build();
    SelectionData entityGroupSelectionData =
        new SelectionData("bloodPressure", serializeToJson(entityGroupData));
    EntityFilter cohortFilter =
        filterBuilder.buildForCohort(
            underlay,
            List.of(entityGroupSelectionData, systolicSelectionData, groupBySelectionData));
    assertNotNull(cohortFilter);
    EntityFilter expectedSystolicSubFilter =
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
            expectedSystolicSubFilter,
            underlay.getEntity("bloodPressure").getAttribute("date"),
            BinaryOperator.GREATER_THAN_OR_EQUAL,
            2);
    assertEquals(expectedCohortFilter, cohortFilter);
  }

  @Test
  void criteriaOnlyDataFeatureFilter() {
    CFPlaceholder.Placeholder config = CFPlaceholder.Placeholder.newBuilder().build();
    CriteriaSelector criteriaSelector =
        new CriteriaSelector(
            "bloodPressure",
            true,
            true,
            "core.EntityGroupFilterBuilder",
            SZCorePlugin.ENTITY_GROUP.getIdInConfig(),
            serializeToJson(config),
            List.of());
    EntityGroupFilterBuilder filterBuilder = new EntityGroupFilterBuilder(criteriaSelector);

    // No ids.
    DTEntityGroup.EntityGroup data =
        DTEntityGroup.EntityGroup.newBuilder()
            .addSelected(
                DTEntityGroup.EntityGroup.Selection.newBuilder()
                    .setEntityGroup("bloodPressurePerson")
                    .build())
            .build();
    SelectionData selectionData = new SelectionData("bloodPressure", serializeToJson(data));
    List<EntityOutput> dataFeatureOutputs =
        filterBuilder.buildForDataFeature(underlay, List.of(selectionData));
    assertEquals(1, dataFeatureOutputs.size());
    EntityOutput expectedDataFeatureOutput =
        EntityOutput.unfiltered(underlay.getEntity("bloodPressure"));
    assertEquals(expectedDataFeatureOutput, dataFeatureOutputs.get(0));
  }
}

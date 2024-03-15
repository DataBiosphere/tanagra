package bio.terra.tanagra.filterbuilder;

import static bio.terra.tanagra.utils.ProtobufUtils.serializeToJson;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import bio.terra.tanagra.api.filter.AttributeFilter;
import bio.terra.tanagra.api.filter.BooleanAndOrFilter;
import bio.terra.tanagra.api.filter.EntityFilter;
import bio.terra.tanagra.api.filter.HierarchyHasAncestorFilter;
import bio.terra.tanagra.api.filter.ItemInGroupFilter;
import bio.terra.tanagra.api.filter.OccurrenceForPrimaryFilter;
import bio.terra.tanagra.api.filter.PrimaryWithCriteriaFilter;
import bio.terra.tanagra.api.shared.BinaryOperator;
import bio.terra.tanagra.api.shared.Literal;
import bio.terra.tanagra.filterbuilder.impl.core.EntityGroupFilterBuilder;
import bio.terra.tanagra.proto.criteriaselector.KeyOuterClass.Key;
import bio.terra.tanagra.proto.criteriaselector.ValueOuterClass.Value;
import bio.terra.tanagra.proto.criteriaselector.configschema.CFAttribute;
import bio.terra.tanagra.proto.criteriaselector.configschema.CFEntityGroup;
import bio.terra.tanagra.proto.criteriaselector.configschema.CFUnhintedValue;
import bio.terra.tanagra.proto.criteriaselector.dataschema.DTAttribute;
import bio.terra.tanagra.proto.criteriaselector.dataschema.DTEntityGroup;
import bio.terra.tanagra.proto.criteriaselector.dataschema.DTUnhintedValue;
import bio.terra.tanagra.underlay.ConfigReader;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.entitymodel.Hierarchy;
import bio.terra.tanagra.underlay.entitymodel.entitygroup.CriteriaOccurrence;
import bio.terra.tanagra.underlay.entitymodel.entitygroup.GroupItems;
import bio.terra.tanagra.underlay.serialization.SZCorePlugin;
import bio.terra.tanagra.underlay.serialization.SZService;
import bio.terra.tanagra.underlay.serialization.SZUnderlay;
import bio.terra.tanagra.underlay.uiplugin.CriteriaSelector;
import bio.terra.tanagra.underlay.uiplugin.SelectionData;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class EntityGroupFilterBuilderForGroupTest {
  private Underlay underlay;

  @BeforeEach
  void setup() {
    ConfigReader configReader = ConfigReader.fromJarResources();
    SZService szService = configReader.readService("sd20230831_verily");
    SZUnderlay szUnderlay = configReader.readUnderlay(szService.underlay);
    underlay = Underlay.fromConfig(szService.bigQuery, szUnderlay, configReader);
  }

  @Test
  void criteriaOnlyCohortFilter() {
    CFEntityGroup.EntityGroup config = CFEntityGroup.EntityGroup.newBuilder().build();
    CriteriaSelector criteriaSelector =
        new CriteriaSelector(
            "genotyping",
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
                    .setKey(Key.newBuilder().setInt64Key(30L).build())
                    .setName("Illumina MEGA-ex Array")
                    .setEntityGroup("genotypingPerson")
                    .build())
            .build();
    SelectionData selectionData = new SelectionData("genotyping", serializeToJson(data));
    EntityFilter cohortFilter = filterBuilder.buildForCohort(underlay, List.of(selectionData));
    assertNotNull(cohortFilter);
    EntityFilter expectedCriteriaSubFilter =
        new HierarchyHasAncestorFilter(
            underlay,
            underlay.getEntity("genotyping"),
            underlay.getEntity("genotyping").getHierarchy(Hierarchy.DEFAULT_NAME),
            Literal.forInt64(30L));
    EntityFilter expectedCohortFilter =
        new ItemInGroupFilter(
            underlay,
            (GroupItems) underlay.getEntityGroup("genotypingPerson"),
            expectedCriteriaSubFilter,
            null,
            null,
            null);
    assertEquals(expectedCohortFilter, cohortFilter);

    // Multiple ids, same entity group.
    data =
        DTEntityGroup.EntityGroup.newBuilder()
            .addSelected(
                DTEntityGroup.EntityGroup.Selection.newBuilder()
                    .setKey(Key.newBuilder().setInt64Key(30L).build())
                    .setName("Illumina MEGA-ex Array")
                    .setEntityGroup("genotypingPerson")
                    .build())
            .addSelected(
                DTEntityGroup.EntityGroup.Selection.newBuilder()
                    .setKey(Key.newBuilder().setInt64Key(3L).build())
                    .setName("Illumina OMNI-Quad")
                    .setEntityGroup("genotypingPerson")
                    .build())
            .build();
    selectionData = new SelectionData("genotyping", serializeToJson(data));
    cohortFilter = filterBuilder.buildForCohort(underlay, List.of(selectionData));
    assertNotNull(cohortFilter);
    expectedCriteriaSubFilter =
        new HierarchyHasAncestorFilter(
            underlay,
            underlay.getEntity("genotyping"),
            underlay.getEntity("genotyping").getHierarchy(Hierarchy.DEFAULT_NAME),
            List.of(Literal.forInt64(30L), Literal.forInt64(3L)));
    expectedCohortFilter =
        new ItemInGroupFilter(
            underlay,
            (GroupItems) underlay.getEntityGroup("genotypingPerson"),
            expectedCriteriaSubFilter,
            null,
            null,
            null);
    assertEquals(expectedCohortFilter, cohortFilter);

    // Multiple ids, different entity groups.
    data =
        DTEntityGroup.EntityGroup.newBuilder()
            .addSelected(
                DTEntityGroup.EntityGroup.Selection.newBuilder()
                    .setKey(Key.newBuilder().setInt64Key(30L).build())
                    .setName("Illumina MEGA-ex Array")
                    .setEntityGroup("genotypingPerson")
                    .build())
            .addSelected(
                DTEntityGroup.EntityGroup.Selection.newBuilder()
                    .setKey(Key.newBuilder().setInt64Key(4_198_190L).build())
                    .setName("Appendectomy")
                    .setEntityGroup("procedurePerson")
                    .build())
            .build();
    selectionData = new SelectionData("genotyping", serializeToJson(data));
    cohortFilter = filterBuilder.buildForCohort(underlay, List.of(selectionData));
    assertNotNull(cohortFilter);
    EntityFilter expectedCriteriaSubFilter1 =
        new HierarchyHasAncestorFilter(
            underlay,
            underlay.getEntity("genotyping"),
            underlay.getEntity("genotyping").getHierarchy(Hierarchy.DEFAULT_NAME),
            Literal.forInt64(30L));
    EntityFilter expectedCohortFilter1 =
        new ItemInGroupFilter(
            underlay,
            (GroupItems) underlay.getEntityGroup("genotypingPerson"),
            expectedCriteriaSubFilter1,
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
    CFAttribute.Attribute nameConfig =
        CFAttribute.Attribute.newBuilder().setAttribute("name").build();
    CriteriaSelector.Modifier nameModifier =
        new CriteriaSelector.Modifier(
            "name", SZCorePlugin.ATTRIBUTE.getIdInConfig(), serializeToJson(nameConfig));
    CFEntityGroup.EntityGroup genotypingConfig = CFEntityGroup.EntityGroup.newBuilder().build();
    CriteriaSelector criteriaSelector =
        new CriteriaSelector(
            "genotyping",
            true,
            true,
            "core.EntityGroupFilterBuilder",
            SZCorePlugin.ENTITY_GROUP.getIdInConfig(),
            serializeToJson(genotypingConfig),
            List.of(nameModifier));
    EntityGroupFilterBuilder filterBuilder = new EntityGroupFilterBuilder(criteriaSelector);

    // Single attribute modifier.
    DTAttribute.Attribute nameData =
        DTAttribute.Attribute.newBuilder()
            .addSelected(
                DTAttribute.Attribute.Selection.newBuilder()
                    .setValue(Value.newBuilder().setStringValue("Illumina OMNI-Quad").build())
                    .build())
            .build();
    SelectionData nameSelectionData = new SelectionData("name", serializeToJson(nameData));
    DTEntityGroup.EntityGroup entityGroupData =
        DTEntityGroup.EntityGroup.newBuilder()
            .addSelected(
                DTEntityGroup.EntityGroup.Selection.newBuilder()
                    .setKey(Key.newBuilder().setInt64Key(3L).build())
                    .setName("Illumina OMNI-Quad")
                    .setEntityGroup("genotypingPerson")
                    .build())
            .build();
    SelectionData entityGroupSelectionData =
        new SelectionData("genotyping", serializeToJson(entityGroupData));
    EntityFilter cohortFilter =
        filterBuilder.buildForCohort(
            underlay, List.of(entityGroupSelectionData, nameSelectionData));
    assertNotNull(cohortFilter);
    EntityFilter expectedCriteriaSubFilter =
        new HierarchyHasAncestorFilter(
            underlay,
            underlay.getEntity("genotyping"),
            underlay.getEntity("genotyping").getHierarchy(Hierarchy.DEFAULT_NAME),
            Literal.forInt64(3L));
    EntityFilter expectedNameSubFilter =
        new AttributeFilter(
            underlay,
            underlay.getEntity("genotyping"),
            underlay.getEntity("genotyping").getAttribute("name"),
            BinaryOperator.EQUALS,
            Literal.forString("Illumina OMNI-Quad"));
    EntityFilter expectedCohortFilter =
        new ItemInGroupFilter(
            underlay,
            (GroupItems) underlay.getEntityGroup("genotypingPerson"),
            new BooleanAndOrFilter(
                BooleanAndOrFilter.LogicalOperator.AND,
                List.of(expectedCriteriaSubFilter, expectedNameSubFilter)),
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
                "genotyping",
                CFUnhintedValue.UnhintedValue.AttributeList.newBuilder().addValues("name").build())
            .build();
    CriteriaSelector.Modifier groupByModifier =
        new CriteriaSelector.Modifier(
            "group_by_count",
            SZCorePlugin.UNHINTED_VALUE.getIdInConfig(),
            serializeToJson(groupByConfig));
    CFEntityGroup.EntityGroup genotypingConfig = CFEntityGroup.EntityGroup.newBuilder().build();
    CriteriaSelector criteriaSelector =
        new CriteriaSelector(
            "genotyping",
            true,
            true,
            "core.EntityGroupFilterBuilder",
            SZCorePlugin.ENTITY_GROUP.getIdInConfig(),
            serializeToJson(genotypingConfig),
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
                    .setKey(Key.newBuilder().setInt64Key(3L).build())
                    .setName("Illumina OMNI-Quad")
                    .setEntityGroup("genotypingPerson")
                    .build())
            .build();
    SelectionData entityGroupSelectionData =
        new SelectionData("genotyping", serializeToJson(entityGroupData));
    EntityFilter cohortFilter =
        filterBuilder.buildForCohort(
            underlay, List.of(entityGroupSelectionData, groupBySelectionData));
    assertNotNull(cohortFilter);
    EntityFilter expectedCriteriaSubFilter =
        new HierarchyHasAncestorFilter(
            underlay,
            underlay.getEntity("genotyping"),
            underlay.getEntity("genotyping").getHierarchy(Hierarchy.DEFAULT_NAME),
            Literal.forInt64(3L));
    EntityFilter expectedCohortFilter =
        new ItemInGroupFilter(
            underlay,
            (GroupItems) underlay.getEntityGroup("genotypingPerson"),
            expectedCriteriaSubFilter,
            List.of(underlay.getEntity("genotyping").getAttribute("name")),
            BinaryOperator.GREATER_THAN_OR_EQUAL,
            2);
    assertEquals(expectedCohortFilter, cohortFilter);
  }

  @Test
  void criteriaWithAttrAndGroupByModifiersCohortFilter() {
    CFAttribute.Attribute nameConfig =
        CFAttribute.Attribute.newBuilder().setAttribute("name").build();
    CriteriaSelector.Modifier nameModifier =
        new CriteriaSelector.Modifier(
            "name", SZCorePlugin.ATTRIBUTE.getIdInConfig(), serializeToJson(nameConfig));
    CFUnhintedValue.UnhintedValue groupByConfig =
        CFUnhintedValue.UnhintedValue.newBuilder()
            .putAttributes(
                "genotyping",
                CFUnhintedValue.UnhintedValue.AttributeList.newBuilder().addValues("name").build())
            .build();
    CriteriaSelector.Modifier groupByModifier =
        new CriteriaSelector.Modifier(
            "group_by_count",
            SZCorePlugin.UNHINTED_VALUE.getIdInConfig(),
            serializeToJson(groupByConfig));
    CFEntityGroup.EntityGroup genotypingConfig = CFEntityGroup.EntityGroup.newBuilder().build();
    CriteriaSelector criteriaSelector =
        new CriteriaSelector(
            "genotyping",
            true,
            true,
            "core.EntityGroupFilterBuilder",
            SZCorePlugin.ENTITY_GROUP.getIdInConfig(),
            serializeToJson(genotypingConfig),
            List.of(nameModifier, groupByModifier));
    EntityGroupFilterBuilder filterBuilder = new EntityGroupFilterBuilder(criteriaSelector);

    DTAttribute.Attribute nameData =
        DTAttribute.Attribute.newBuilder()
            .addSelected(
                DTAttribute.Attribute.Selection.newBuilder()
                    .setValue(Value.newBuilder().setStringValue("Illumina OMNI-Quad").build())
                    .build())
            .build();
    SelectionData nameSelectionData = new SelectionData("name", serializeToJson(nameData));
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
                    .setKey(Key.newBuilder().setInt64Key(3L).build())
                    .setName("Illumina OMNI-Quad")
                    .setEntityGroup("genotypingPerson")
                    .build())
            .build();
    SelectionData entityGroupSelectionData =
        new SelectionData("genotyping", serializeToJson(entityGroupData));
    EntityFilter cohortFilter =
        filterBuilder.buildForCohort(
            underlay, List.of(entityGroupSelectionData, nameSelectionData, groupBySelectionData));
    assertNotNull(cohortFilter);
    EntityFilter expectedCriteriaSubFilter =
        new HierarchyHasAncestorFilter(
            underlay,
            underlay.getEntity("genotyping"),
            underlay.getEntity("genotyping").getHierarchy(Hierarchy.DEFAULT_NAME),
            Literal.forInt64(3L));
    EntityFilter expectedNameSubFilter =
        new AttributeFilter(
            underlay,
            underlay.getEntity("genotyping"),
            underlay.getEntity("genotyping").getAttribute("name"),
            BinaryOperator.EQUALS,
            Literal.forString("Illumina OMNI-Quad"));
    EntityFilter expectedCohortFilter =
        new ItemInGroupFilter(
            underlay,
            (GroupItems) underlay.getEntityGroup("genotypingPerson"),
            new BooleanAndOrFilter(
                BooleanAndOrFilter.LogicalOperator.AND,
                List.of(expectedCriteriaSubFilter, expectedNameSubFilter)),
            List.of(underlay.getEntity("genotyping").getAttribute("name")),
            BinaryOperator.GREATER_THAN_OR_EQUAL,
            2);
    assertEquals(expectedCohortFilter, cohortFilter);
  }

  @Test
  void criteriaOnlyDataFeatureFilter() {
    CFEntityGroup.EntityGroup config = CFEntityGroup.EntityGroup.newBuilder().build();
    CriteriaSelector criteriaSelector =
        new CriteriaSelector(
            "genotyping",
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
                    .setKey(Key.newBuilder().setInt64Key(30L).build())
                    .setName("Illumina MEGA-ex Array")
                    .setEntityGroup("genotypingPerson")
                    .build())
            .build();
    SelectionData selectionData = new SelectionData("genotyping", serializeToJson(data));
    List<EntityOutput> dataFeatureOutputs =
        filterBuilder.buildForDataFeature(underlay, List.of(selectionData));
    assertEquals(1, dataFeatureOutputs.size());
    EntityFilter expectedDataFeatureFilter =
        new HierarchyHasAncestorFilter(
            underlay,
            underlay.getEntity("genotyping"),
            underlay.getEntity("genotyping").getHierarchy(Hierarchy.DEFAULT_NAME),
            Literal.forInt64(30L));
    EntityOutput expectedDataFeatureOutput =
        EntityOutput.filtered(underlay.getEntity("genotyping"), expectedDataFeatureFilter);
    assertEquals(expectedDataFeatureOutput, dataFeatureOutputs.get(0));

    // Multiple ids, same entity group.
    data =
        DTEntityGroup.EntityGroup.newBuilder()
            .addSelected(
                DTEntityGroup.EntityGroup.Selection.newBuilder()
                    .setKey(Key.newBuilder().setInt64Key(30L).build())
                    .setName("Illumina MEGA-ex Array")
                    .setEntityGroup("genotypingPerson")
                    .build())
            .addSelected(
                DTEntityGroup.EntityGroup.Selection.newBuilder()
                    .setKey(Key.newBuilder().setInt64Key(3L).build())
                    .setName("Illumina OMNI-Quad")
                    .setEntityGroup("genotypingPerson")
                    .build())
            .build();
    selectionData = new SelectionData("genotyping", serializeToJson(data));
    dataFeatureOutputs = filterBuilder.buildForDataFeature(underlay, List.of(selectionData));
    assertEquals(1, dataFeatureOutputs.size());
    expectedDataFeatureFilter =
        new HierarchyHasAncestorFilter(
            underlay,
            underlay.getEntity("genotyping"),
            underlay.getEntity("genotyping").getHierarchy(Hierarchy.DEFAULT_NAME),
            List.of(Literal.forInt64(30L), Literal.forInt64(3L)));
    expectedDataFeatureOutput =
        EntityOutput.filtered(underlay.getEntity("genotyping"), expectedDataFeatureFilter);
    assertEquals(expectedDataFeatureOutput, dataFeatureOutputs.get(0));

    // Multiple ids, different entity groups.
    data =
        DTEntityGroup.EntityGroup.newBuilder()
            .addSelected(
                DTEntityGroup.EntityGroup.Selection.newBuilder()
                    .setKey(Key.newBuilder().setInt64Key(30L).build())
                    .setName("Illumina MEGA-ex Array")
                    .setEntityGroup("genotypingPerson")
                    .build())
            .addSelected(
                DTEntityGroup.EntityGroup.Selection.newBuilder()
                    .setKey(Key.newBuilder().setInt64Key(4_198_190L).build())
                    .setName("Appendectomy")
                    .setEntityGroup("procedurePerson")
                    .build())
            .build();
    selectionData = new SelectionData("genotyping", serializeToJson(data));
    dataFeatureOutputs = filterBuilder.buildForDataFeature(underlay, List.of(selectionData));
    assertEquals(2, dataFeatureOutputs.size());
    EntityFilter expectedDataFeatureFilter1 =
        new HierarchyHasAncestorFilter(
            underlay,
            underlay.getEntity("genotyping"),
            underlay.getEntity("genotyping").getHierarchy(Hierarchy.DEFAULT_NAME),
            Literal.forInt64(30L));
    EntityOutput expectedDataFeatureOutput1 =
        EntityOutput.filtered(underlay.getEntity("genotyping"), expectedDataFeatureFilter1);
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
}

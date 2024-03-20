package bio.terra.tanagra.filterbuilder;

import static bio.terra.tanagra.utils.ProtobufUtils.serializeToJson;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import bio.terra.tanagra.api.filter.AttributeFilter;
import bio.terra.tanagra.api.filter.BooleanAndOrFilter;
import bio.terra.tanagra.api.filter.EntityFilter;
import bio.terra.tanagra.api.filter.OccurrenceForPrimaryFilter;
import bio.terra.tanagra.api.filter.PrimaryWithCriteriaFilter;
import bio.terra.tanagra.api.filter.TextSearchFilter;
import bio.terra.tanagra.api.shared.BinaryOperator;
import bio.terra.tanagra.api.shared.Literal;
import bio.terra.tanagra.api.shared.NaryOperator;
import bio.terra.tanagra.filterbuilder.impl.core.TextSearchFilterBuilder;
import bio.terra.tanagra.proto.criteriaselector.DataRangeOuterClass;
import bio.terra.tanagra.proto.criteriaselector.ValueOuterClass;
import bio.terra.tanagra.proto.criteriaselector.configschema.CFAttribute;
import bio.terra.tanagra.proto.criteriaselector.configschema.CFTextSearch;
import bio.terra.tanagra.proto.criteriaselector.configschema.CFUnhintedValue;
import bio.terra.tanagra.proto.criteriaselector.dataschema.DTAttribute;
import bio.terra.tanagra.proto.criteriaselector.dataschema.DTTextSearch;
import bio.terra.tanagra.proto.criteriaselector.dataschema.DTUnhintedValue;
import bio.terra.tanagra.underlay.ConfigReader;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.entitymodel.Entity;
import bio.terra.tanagra.underlay.entitymodel.entitygroup.CriteriaOccurrence;
import bio.terra.tanagra.underlay.serialization.SZCorePlugin;
import bio.terra.tanagra.underlay.serialization.SZService;
import bio.terra.tanagra.underlay.serialization.SZUnderlay;
import bio.terra.tanagra.underlay.uiplugin.CriteriaSelector;
import bio.terra.tanagra.underlay.uiplugin.SelectionData;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TextSearchFilterBuilderTest {
  private Underlay underlay;

  @BeforeEach
  void setup() {
    ConfigReader configReader = ConfigReader.fromJarResources();
    SZService szService = configReader.readService("sd20230831_verily");
    SZUnderlay szUnderlay = configReader.readUnderlay(szService.underlay);
    underlay = Underlay.fromConfig(szService.bigQuery, szUnderlay, configReader);
  }

  @Test
  void textOnlyCohortFilter() {
    // Text query, no attribute.
    CFTextSearch.TextSearch config = CFTextSearch.TextSearch.newBuilder().setEntity("note").build();
    CriteriaSelector criteriaSelector =
        new CriteriaSelector(
            "note_noAttribute",
            true,
            true,
            "core.TextSearchFilterBuilder",
            SZCorePlugin.TEXT_SEARCH.getIdInConfig(),
            serializeToJson(config),
            List.of());
    TextSearchFilterBuilder filterBuilder = new TextSearchFilterBuilder(criteriaSelector);

    DTTextSearch.TextSearch data =
        DTTextSearch.TextSearch.newBuilder().setQuery("ambulance").build();
    SelectionData selectionData = new SelectionData("note_noAttribute", serializeToJson(data));
    EntityFilter cohortFilter = filterBuilder.buildForCohort(underlay, List.of(selectionData));
    assertNotNull(cohortFilter);

    Entity occurrenceEntity = underlay.getEntity("noteOccurrence");
    EntityFilter expectedCohortFilter =
        new PrimaryWithCriteriaFilter(
            underlay,
            (CriteriaOccurrence) underlay.getEntityGroup("notePerson"),
            null,
            Map.of(
                occurrenceEntity,
                List.of(
                    new TextSearchFilter(
                        underlay,
                        occurrenceEntity,
                        TextSearchFilter.TextSearchOperator.EXACT_MATCH,
                        "ambulance",
                        null))),
            null,
            null,
            null);
    assertEquals(expectedCohortFilter, cohortFilter);

    // Text query, with attribute.
    config =
        CFTextSearch.TextSearch.newBuilder().setEntity("note").setSearchAttribute("title").build();
    criteriaSelector =
        new CriteriaSelector(
            "note_withAttribute",
            true,
            true,
            "core.TextSearchFilterBuilder",
            SZCorePlugin.TEXT_SEARCH.getIdInConfig(),
            serializeToJson(config),
            List.of());
    filterBuilder = new TextSearchFilterBuilder(criteriaSelector);

    selectionData = new SelectionData("note_withAttribute", serializeToJson(data));
    cohortFilter = filterBuilder.buildForCohort(underlay, List.of(selectionData));
    assertNotNull(cohortFilter);
    expectedCohortFilter =
        new PrimaryWithCriteriaFilter(
            underlay,
            (CriteriaOccurrence) underlay.getEntityGroup("notePerson"),
            null,
            Map.of(
                occurrenceEntity,
                List.of(
                    new TextSearchFilter(
                        underlay,
                        occurrenceEntity,
                        TextSearchFilter.TextSearchOperator.EXACT_MATCH,
                        "ambulance",
                        occurrenceEntity.getAttribute("title")))),
            null,
            null,
            null);
    assertEquals(expectedCohortFilter, cohortFilter);
  }

  @Test
  void criteriaOnlyCohortFilter() {
    CFTextSearch.TextSearch configNoAttr =
        CFTextSearch.TextSearch.newBuilder().setEntity("note").build();
    CriteriaSelector criteriaSelectorNoAttr =
        new CriteriaSelector(
            "note_noAttribute",
            true,
            true,
            "core.TextSearchFilterBuilder",
            SZCorePlugin.TEXT_SEARCH.getIdInConfig(),
            serializeToJson(configNoAttr),
            List.of());
    TextSearchFilterBuilder filterBuilderNoAttr =
        new TextSearchFilterBuilder(criteriaSelectorNoAttr);

    // Single id, no text search filter.
    DTTextSearch.TextSearch data =
        DTTextSearch.TextSearch.newBuilder()
            .addCategories(
                DTTextSearch.TextSearch.Selection.newBuilder()
                    .setValue(ValueOuterClass.Value.newBuilder().setInt64Value(44_814_644L).build())
                    .setName("Nursing report")
                    .build())
            .build();
    SelectionData selectionData = new SelectionData("note_noAttribute", serializeToJson(data));
    EntityFilter cohortFilter =
        filterBuilderNoAttr.buildForCohort(underlay, List.of(selectionData));
    assertNotNull(cohortFilter);

    CriteriaOccurrence criteriaOccurrence =
        (CriteriaOccurrence) underlay.getEntityGroup("notePerson");
    Entity occurrenceEntity = underlay.getEntity("noteOccurrence");
    EntityFilter expectedCohortFilter =
        new PrimaryWithCriteriaFilter(
            underlay,
            criteriaOccurrence,
            new AttributeFilter(
                underlay,
                criteriaOccurrence.getCriteriaEntity(),
                occurrenceEntity.getIdAttribute(),
                BinaryOperator.EQUALS,
                Literal.forInt64(44_814_644L)),
            Map.of(),
            null,
            null,
            null);
    assertEquals(expectedCohortFilter, cohortFilter);

    // Single id, with text search filter.
    data =
        DTTextSearch.TextSearch.newBuilder()
            .setQuery("ambulance")
            .addCategories(
                DTTextSearch.TextSearch.Selection.newBuilder()
                    .setValue(ValueOuterClass.Value.newBuilder().setInt64Value(44_814_644L).build())
                    .setName("Nursing report")
                    .build())
            .build();
    selectionData = new SelectionData("note_noAttribute", serializeToJson(data));
    cohortFilter = filterBuilderNoAttr.buildForCohort(underlay, List.of(selectionData));
    assertNotNull(cohortFilter);

    expectedCohortFilter =
        new PrimaryWithCriteriaFilter(
            underlay,
            criteriaOccurrence,
            new AttributeFilter(
                underlay,
                criteriaOccurrence.getCriteriaEntity(),
                occurrenceEntity.getIdAttribute(),
                BinaryOperator.EQUALS,
                Literal.forInt64(44_814_644L)),
            Map.of(
                occurrenceEntity,
                List.of(
                    new TextSearchFilter(
                        underlay,
                        occurrenceEntity,
                        TextSearchFilter.TextSearchOperator.EXACT_MATCH,
                        "ambulance",
                        null))),
            null,
            null,
            null);
    assertEquals(expectedCohortFilter, cohortFilter);

    // Multiple ids, no text search filter.
    data =
        DTTextSearch.TextSearch.newBuilder()
            .addCategories(
                DTTextSearch.TextSearch.Selection.newBuilder()
                    .setValue(ValueOuterClass.Value.newBuilder().setInt64Value(44_814_644L).build())
                    .setName("Nursing report")
                    .build())
            .addCategories(
                DTTextSearch.TextSearch.Selection.newBuilder()
                    .setValue(ValueOuterClass.Value.newBuilder().setInt64Value(44_814_638L).build())
                    .setName("Admission note")
                    .build())
            .build();
    selectionData = new SelectionData("note_noAttribute", serializeToJson(data));
    cohortFilter = filterBuilderNoAttr.buildForCohort(underlay, List.of(selectionData));
    assertNotNull(cohortFilter);

    expectedCohortFilter =
        new PrimaryWithCriteriaFilter(
            underlay,
            criteriaOccurrence,
            new AttributeFilter(
                underlay,
                criteriaOccurrence.getCriteriaEntity(),
                occurrenceEntity.getIdAttribute(),
                NaryOperator.IN,
                List.of(Literal.forInt64(44_814_644L), Literal.forInt64(44_814_638L))),
            Map.of(),
            null,
            null,
            null);
    assertEquals(expectedCohortFilter, cohortFilter);

    // Multiple ids, with text search filter.
    data =
        DTTextSearch.TextSearch.newBuilder()
            .setQuery("ambulance")
            .addCategories(
                DTTextSearch.TextSearch.Selection.newBuilder()
                    .setValue(ValueOuterClass.Value.newBuilder().setInt64Value(44_814_644L).build())
                    .setName("Nursing report")
                    .build())
            .addCategories(
                DTTextSearch.TextSearch.Selection.newBuilder()
                    .setValue(ValueOuterClass.Value.newBuilder().setInt64Value(44_814_638L).build())
                    .setName("Admission note")
                    .build())
            .build();
    selectionData = new SelectionData("note_noAttribute", serializeToJson(data));
    cohortFilter = filterBuilderNoAttr.buildForCohort(underlay, List.of(selectionData));
    assertNotNull(cohortFilter);

    expectedCohortFilter =
        new PrimaryWithCriteriaFilter(
            underlay,
            criteriaOccurrence,
            new AttributeFilter(
                underlay,
                criteriaOccurrence.getCriteriaEntity(),
                occurrenceEntity.getIdAttribute(),
                NaryOperator.IN,
                List.of(Literal.forInt64(44_814_644L), Literal.forInt64(44_814_638L))),
            Map.of(
                occurrenceEntity,
                List.of(
                    new TextSearchFilter(
                        underlay,
                        occurrenceEntity,
                        TextSearchFilter.TextSearchOperator.EXACT_MATCH,
                        "ambulance",
                        null))),
            null,
            null,
            null);
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
    CFTextSearch.TextSearch textSearchConfig =
        CFTextSearch.TextSearch.newBuilder().setEntity("note").build();
    CriteriaSelector criteriaSelector =
        new CriteriaSelector(
            "note_noAttribute",
            true,
            true,
            "core.TextSearchFilterBuilder",
            SZCorePlugin.ENTITY_GROUP.getIdInConfig(),
            serializeToJson(textSearchConfig),
            List.of(ageAtOccurrenceModifier, visitTypeModifier));
    TextSearchFilterBuilder filterBuilder = new TextSearchFilterBuilder(criteriaSelector);

    // Single attribute modifier, no text search filter.
    DTTextSearch.TextSearch textSearchData =
        DTTextSearch.TextSearch.newBuilder()
            .addCategories(
                DTTextSearch.TextSearch.Selection.newBuilder()
                    .setValue(ValueOuterClass.Value.newBuilder().setInt64Value(44_814_644L).build())
                    .setName("Nursing report")
                    .build())
            .build();
    SelectionData textSearchSelectionData =
        new SelectionData("note_noAttribute", serializeToJson(textSearchData));
    DTAttribute.Attribute ageAtOccurrenceData =
        DTAttribute.Attribute.newBuilder()
            .addDataRanges(DataRangeOuterClass.DataRange.newBuilder().setMin(45).setMax(65).build())
            .build();
    SelectionData ageAtOccurrenceSelectionData =
        new SelectionData("age_at_occurrence", serializeToJson(ageAtOccurrenceData));
    EntityFilter cohortFilter =
        filterBuilder.buildForCohort(
            underlay, List.of(textSearchSelectionData, ageAtOccurrenceSelectionData));
    assertNotNull(cohortFilter);

    CriteriaOccurrence criteriaOccurrence =
        (CriteriaOccurrence) underlay.getEntityGroup("notePerson");
    Entity occurrenceEntity = underlay.getEntity("noteOccurrence");
    EntityFilter expectedAgeAtOccurrenceSubFilter =
        new AttributeFilter(
            underlay,
            occurrenceEntity,
            occurrenceEntity.getAttribute("age_at_occurrence"),
            NaryOperator.BETWEEN,
            List.of(Literal.forDouble(45.0), Literal.forDouble(65.0)));
    EntityFilter expectedCohortFilter =
        new PrimaryWithCriteriaFilter(
            underlay,
            criteriaOccurrence,
            new AttributeFilter(
                underlay,
                criteriaOccurrence.getCriteriaEntity(),
                occurrenceEntity.getIdAttribute(),
                BinaryOperator.EQUALS,
                Literal.forInt64(44_814_644L)),
            Map.of(occurrenceEntity, List.of(expectedAgeAtOccurrenceSubFilter)),
            null,
            null,
            null);
    assertEquals(expectedCohortFilter, cohortFilter);

    // Two attribute modifiers, with text search filter.
    textSearchData = DTTextSearch.TextSearch.newBuilder().setQuery("ambulance").build();
    textSearchSelectionData =
        new SelectionData("note_noAttribute", serializeToJson(textSearchData));
    DTAttribute.Attribute visitTypeData =
        DTAttribute.Attribute.newBuilder()
            .addSelected(
                DTAttribute.Attribute.Selection.newBuilder()
                    .setValue(ValueOuterClass.Value.newBuilder().setInt64Value(9_202L).build())
                    .setName("Outpatient Visit")
                    .build())
            .build();
    SelectionData visitTypeSelectionData =
        new SelectionData("visit_type", serializeToJson(visitTypeData));
    cohortFilter =
        filterBuilder.buildForCohort(
            underlay,
            List.of(textSearchSelectionData, ageAtOccurrenceSelectionData, visitTypeSelectionData));
    assertNotNull(cohortFilter);
    EntityFilter expectedTextSearchSubFilter =
        new TextSearchFilter(
            underlay,
            occurrenceEntity,
            TextSearchFilter.TextSearchOperator.EXACT_MATCH,
            "ambulance",
            null);
    EntityFilter expectedVisitTypeSubFilter =
        new AttributeFilter(
            underlay,
            occurrenceEntity,
            occurrenceEntity.getAttribute("visit_type"),
            BinaryOperator.EQUALS,
            Literal.forInt64(9_202L));
    expectedCohortFilter =
        new PrimaryWithCriteriaFilter(
            underlay,
            criteriaOccurrence,
            null,
            Map.of(
                occurrenceEntity,
                List.of(
                    expectedAgeAtOccurrenceSubFilter,
                    expectedVisitTypeSubFilter,
                    expectedTextSearchSubFilter)),
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
                "noteOccurrence",
                CFUnhintedValue.UnhintedValue.AttributeList.newBuilder()
                    .addValues("start_date")
                    .build())
            .build();
    CriteriaSelector.Modifier groupByModifier =
        new CriteriaSelector.Modifier(
            "group_by_count",
            SZCorePlugin.UNHINTED_VALUE.getIdInConfig(),
            serializeToJson(groupByConfig));
    CFTextSearch.TextSearch textSearchConfig =
        CFTextSearch.TextSearch.newBuilder().setEntity("note").build();
    CriteriaSelector criteriaSelector =
        new CriteriaSelector(
            "note_noAttribute",
            true,
            true,
            "core.TextSearchFilterBuilder",
            SZCorePlugin.ENTITY_GROUP.getIdInConfig(),
            serializeToJson(textSearchConfig),
            List.of(groupByModifier));
    TextSearchFilterBuilder filterBuilder = new TextSearchFilterBuilder(criteriaSelector);

    DTTextSearch.TextSearch textSearchData =
        DTTextSearch.TextSearch.newBuilder().setQuery("ambulance").build();
    SelectionData textSearchSelectionData =
        new SelectionData("note_noAttribute", serializeToJson(textSearchData));
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
            underlay, List.of(textSearchSelectionData, groupBySelectionData));
    assertNotNull(cohortFilter);

    CriteriaOccurrence criteriaOccurrence =
        (CriteriaOccurrence) underlay.getEntityGroup("notePerson");
    Entity occurrenceEntity = underlay.getEntity("noteOccurrence");
    EntityFilter expectedTextSearchSubFilter =
        new TextSearchFilter(
            underlay,
            occurrenceEntity,
            TextSearchFilter.TextSearchOperator.EXACT_MATCH,
            "ambulance",
            null);
    EntityFilter expectedCohortFilter =
        new PrimaryWithCriteriaFilter(
            underlay,
            criteriaOccurrence,
            null,
            Map.of(occurrenceEntity, List.of(expectedTextSearchSubFilter)),
            Map.of(occurrenceEntity, List.of(occurrenceEntity.getAttribute("start_date"))),
            BinaryOperator.GREATER_THAN_OR_EQUAL,
            2);
    assertEquals(expectedCohortFilter, cohortFilter);
  }

  @Test
  void criteriaWithAttrAndGroupByModifiersCohortFilter() {
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
                "noteOccurrence",
                CFUnhintedValue.UnhintedValue.AttributeList.newBuilder()
                    .addValues("start_date")
                    .build())
            .build();
    CriteriaSelector.Modifier groupByModifier =
        new CriteriaSelector.Modifier(
            "group_by_count",
            SZCorePlugin.UNHINTED_VALUE.getIdInConfig(),
            serializeToJson(groupByConfig));
    CFTextSearch.TextSearch textSearchConfig =
        CFTextSearch.TextSearch.newBuilder().setEntity("note").build();
    CriteriaSelector criteriaSelector =
        new CriteriaSelector(
            "note_noAttribute",
            true,
            true,
            "core.TextSearchFilterBuilder",
            SZCorePlugin.ENTITY_GROUP.getIdInConfig(),
            serializeToJson(textSearchConfig),
            List.of(ageAtOccurrenceModifier, visitTypeModifier, groupByModifier));
    TextSearchFilterBuilder filterBuilder = new TextSearchFilterBuilder(criteriaSelector);

    // Single attribute modifier, no text search filter.
    DTTextSearch.TextSearch textSearchData =
        DTTextSearch.TextSearch.newBuilder()
            .setQuery("ambulance")
            .addCategories(
                DTTextSearch.TextSearch.Selection.newBuilder()
                    .setValue(ValueOuterClass.Value.newBuilder().setInt64Value(44_814_644L).build())
                    .setName("Nursing report")
                    .build())
            .build();
    SelectionData textSearchSelectionData =
        new SelectionData("note_noAttribute", serializeToJson(textSearchData));
    DTAttribute.Attribute ageAtOccurrenceData =
        DTAttribute.Attribute.newBuilder()
            .addDataRanges(DataRangeOuterClass.DataRange.newBuilder().setMin(45).setMax(65).build())
            .build();
    SelectionData ageAtOccurrenceSelectionData =
        new SelectionData("age_at_occurrence", serializeToJson(ageAtOccurrenceData));
    DTAttribute.Attribute visitTypeData =
        DTAttribute.Attribute.newBuilder()
            .addSelected(
                DTAttribute.Attribute.Selection.newBuilder()
                    .setValue(ValueOuterClass.Value.newBuilder().setInt64Value(9_202L).build())
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

    EntityFilter cohortFilter =
        filterBuilder.buildForCohort(
            underlay,
            List.of(
                textSearchSelectionData,
                ageAtOccurrenceSelectionData,
                visitTypeSelectionData,
                groupBySelectionData));
    assertNotNull(cohortFilter);

    CriteriaOccurrence criteriaOccurrence =
        (CriteriaOccurrence) underlay.getEntityGroup("notePerson");
    Entity occurrenceEntity = underlay.getEntity("noteOccurrence");
    EntityFilter expectedAgeAtOccurrenceSubFilter =
        new AttributeFilter(
            underlay,
            occurrenceEntity,
            occurrenceEntity.getAttribute("age_at_occurrence"),
            NaryOperator.BETWEEN,
            List.of(Literal.forDouble(45.0), Literal.forDouble(65.0)));
    EntityFilter expectedVisitTypeSubFilter =
        new AttributeFilter(
            underlay,
            occurrenceEntity,
            occurrenceEntity.getAttribute("visit_type"),
            BinaryOperator.EQUALS,
            Literal.forInt64(9_202L));
    EntityFilter expectedTextSearchSubFilter =
        new TextSearchFilter(
            underlay,
            occurrenceEntity,
            TextSearchFilter.TextSearchOperator.EXACT_MATCH,
            "ambulance",
            null);
    EntityFilter expectedCohortFilter =
        new PrimaryWithCriteriaFilter(
            underlay,
            criteriaOccurrence,
            new AttributeFilter(
                underlay,
                criteriaOccurrence.getCriteriaEntity(),
                occurrenceEntity.getIdAttribute(),
                BinaryOperator.EQUALS,
                Literal.forInt64(44_814_644L)),
            Map.of(
                occurrenceEntity,
                List.of(
                    expectedAgeAtOccurrenceSubFilter,
                    expectedVisitTypeSubFilter,
                    expectedTextSearchSubFilter)),
            Map.of(occurrenceEntity, List.of(occurrenceEntity.getAttribute("start_date"))),
            BinaryOperator.GREATER_THAN_OR_EQUAL,
            2);
    assertEquals(expectedCohortFilter, cohortFilter);
  }

  @Test
  void emptyCriteriaCohortFilter() {
    CFTextSearch.TextSearch configNoAttr =
        CFTextSearch.TextSearch.newBuilder().setEntity("note").build();
    CriteriaSelector criteriaSelectorNoAttr =
        new CriteriaSelector(
            "note_noAttribute",
            true,
            true,
            "core.TextSearchFilterBuilder",
            SZCorePlugin.TEXT_SEARCH.getIdInConfig(),
            serializeToJson(configNoAttr),
            List.of());
    TextSearchFilterBuilder filterBuilder = new TextSearchFilterBuilder(criteriaSelectorNoAttr);

    // Null selection data.
    SelectionData selectionData = new SelectionData("note_noAttribute", null);
    EntityFilter cohortFilter = filterBuilder.buildForCohort(underlay, List.of(selectionData));
    assertNull(cohortFilter);

    // Empty string selection data.
    selectionData = new SelectionData("note_noAttribute", "");
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
    CFTextSearch.TextSearch textSearchConfig =
        CFTextSearch.TextSearch.newBuilder().setEntity("note").build();
    CriteriaSelector criteriaSelector =
        new CriteriaSelector(
            "note_noAttribute",
            true,
            true,
            "core.TextSearchFilterBuilder",
            SZCorePlugin.ENTITY_GROUP.getIdInConfig(),
            serializeToJson(textSearchConfig),
            List.of(ageAtOccurrenceModifier));
    TextSearchFilterBuilder filterBuilder = new TextSearchFilterBuilder(criteriaSelector);

    DTTextSearch.TextSearch textSearchData =
        DTTextSearch.TextSearch.newBuilder()
            .addCategories(
                DTTextSearch.TextSearch.Selection.newBuilder()
                    .setValue(ValueOuterClass.Value.newBuilder().setInt64Value(44_814_644L).build())
                    .setName("Nursing report")
                    .build())
            .build();
    SelectionData textSearchSelectionData =
        new SelectionData("note_noAttribute", serializeToJson(textSearchData));

    CriteriaOccurrence criteriaOccurrence =
        (CriteriaOccurrence) underlay.getEntityGroup("notePerson");
    Entity occurrenceEntity = underlay.getEntity("noteOccurrence");
    EntityFilter expectedCohortFilter =
        new PrimaryWithCriteriaFilter(
            underlay,
            criteriaOccurrence,
            new AttributeFilter(
                underlay,
                criteriaOccurrence.getCriteriaEntity(),
                occurrenceEntity.getIdAttribute(),
                BinaryOperator.EQUALS,
                Literal.forInt64(44_814_644L)),
            null,
            null,
            null,
            null);

    // Null selection data.
    SelectionData ageAtOccurrenceSelectionData = new SelectionData("age_at_occurrence", null);
    EntityFilter cohortFilter =
        filterBuilder.buildForCohort(
            underlay, List.of(textSearchSelectionData, ageAtOccurrenceSelectionData));
    assertNotNull(cohortFilter);
    assertEquals(expectedCohortFilter, cohortFilter);

    // Empty string selection data.
    ageAtOccurrenceSelectionData = new SelectionData("age_at_occurrence", "");
    cohortFilter =
        filterBuilder.buildForCohort(
            underlay, List.of(textSearchSelectionData, ageAtOccurrenceSelectionData));
    assertNotNull(cohortFilter);
    assertEquals(expectedCohortFilter, cohortFilter);
  }

  @Test
  void emptyGroupByModifierCohortFilter() {
    CFUnhintedValue.UnhintedValue groupByConfig =
        CFUnhintedValue.UnhintedValue.newBuilder()
            .putAttributes(
                "noteOccurrence",
                CFUnhintedValue.UnhintedValue.AttributeList.newBuilder()
                    .addValues("start_date")
                    .build())
            .build();
    CriteriaSelector.Modifier groupByModifier =
        new CriteriaSelector.Modifier(
            "group_by_count",
            SZCorePlugin.UNHINTED_VALUE.getIdInConfig(),
            serializeToJson(groupByConfig));
    CFTextSearch.TextSearch textSearchConfig =
        CFTextSearch.TextSearch.newBuilder().setEntity("note").build();
    CriteriaSelector criteriaSelector =
        new CriteriaSelector(
            "note_noAttribute",
            true,
            true,
            "core.TextSearchFilterBuilder",
            SZCorePlugin.ENTITY_GROUP.getIdInConfig(),
            serializeToJson(textSearchConfig),
            List.of(groupByModifier));
    TextSearchFilterBuilder filterBuilder = new TextSearchFilterBuilder(criteriaSelector);

    DTTextSearch.TextSearch textSearchData =
        DTTextSearch.TextSearch.newBuilder().setQuery("ambulance").build();
    SelectionData textSearchSelectionData =
        new SelectionData("note_noAttribute", serializeToJson(textSearchData));

    CriteriaOccurrence criteriaOccurrence =
        (CriteriaOccurrence) underlay.getEntityGroup("notePerson");
    Entity occurrenceEntity = underlay.getEntity("noteOccurrence");
    EntityFilter expectedTextSearchSubFilter =
        new TextSearchFilter(
            underlay,
            occurrenceEntity,
            TextSearchFilter.TextSearchOperator.EXACT_MATCH,
            "ambulance",
            null);
    EntityFilter expectedCohortFilter =
        new PrimaryWithCriteriaFilter(
            underlay,
            criteriaOccurrence,
            null,
            Map.of(occurrenceEntity, List.of(expectedTextSearchSubFilter)),
            null,
            null,
            null);

    // Null selection data.
    SelectionData groupBySelectionData = new SelectionData("group_by_count", null);
    EntityFilter cohortFilter =
        filterBuilder.buildForCohort(
            underlay, List.of(textSearchSelectionData, groupBySelectionData));
    assertNotNull(cohortFilter);
    assertEquals(expectedCohortFilter, cohortFilter);

    // Empty string selection data.
    groupBySelectionData = new SelectionData("group_by_count", "");
    cohortFilter =
        filterBuilder.buildForCohort(
            underlay, List.of(textSearchSelectionData, groupBySelectionData));
    assertNotNull(cohortFilter);
    assertEquals(expectedCohortFilter, cohortFilter);
  }

  @Test
  void singleOccurrenceDataFeatureFilter() {
    CFTextSearch.TextSearch configNoAttr =
        CFTextSearch.TextSearch.newBuilder().setEntity("note").build();
    CriteriaSelector criteriaSelectorNoAttr =
        new CriteriaSelector(
            "note_noAttribute",
            true,
            true,
            "core.TextSearchFilterBuilder",
            SZCorePlugin.TEXT_SEARCH.getIdInConfig(),
            serializeToJson(configNoAttr),
            List.of());
    TextSearchFilterBuilder filterBuilderNoAttr =
        new TextSearchFilterBuilder(criteriaSelectorNoAttr);
    CFTextSearch.TextSearch configWithAttr =
        CFTextSearch.TextSearch.newBuilder().setEntity("note").setSearchAttribute("title").build();
    CriteriaSelector criteriaSelectorWithAttr =
        new CriteriaSelector(
            "note_withAttribute",
            true,
            true,
            "core.TextSearchFilterBuilder",
            SZCorePlugin.TEXT_SEARCH.getIdInConfig(),
            serializeToJson(configWithAttr),
            List.of());
    TextSearchFilterBuilder filterBuilderWithAttr =
        new TextSearchFilterBuilder(criteriaSelectorWithAttr);

    // No id, no text search filter.
    DTTextSearch.TextSearch data = DTTextSearch.TextSearch.newBuilder().build();
    SelectionData selectionData = new SelectionData("note_noAttribute", serializeToJson(data));
    List<EntityOutput> dataFeatureOutputs =
        filterBuilderNoAttr.buildForDataFeature(underlay, List.of(selectionData));
    assertEquals(1, dataFeatureOutputs.size());

    CriteriaOccurrence criteriaOccurrence =
        (CriteriaOccurrence) underlay.getEntityGroup("notePerson");
    Entity occurrenceEntity = underlay.getEntity("noteOccurrence");
    assertEquals(List.of(EntityOutput.unfiltered(occurrenceEntity)), dataFeatureOutputs);

    // No id, with text search filter.
    data = DTTextSearch.TextSearch.newBuilder().setQuery("ambulance").build();
    selectionData = new SelectionData("note_withAttribute", serializeToJson(data));
    dataFeatureOutputs =
        filterBuilderWithAttr.buildForDataFeature(underlay, List.of(selectionData));
    assertEquals(1, dataFeatureOutputs.size());
    EntityFilter expectedDataFeatureFilter =
        new TextSearchFilter(
            underlay,
            occurrenceEntity,
            TextSearchFilter.TextSearchOperator.EXACT_MATCH,
            "ambulance",
            occurrenceEntity.getAttribute("title"));
    assertEquals(
        List.of(EntityOutput.filtered(occurrenceEntity, expectedDataFeatureFilter)),
        dataFeatureOutputs);

    // Single id, no text search filter.
    data =
        DTTextSearch.TextSearch.newBuilder()
            .addCategories(
                DTTextSearch.TextSearch.Selection.newBuilder()
                    .setValue(ValueOuterClass.Value.newBuilder().setInt64Value(44_814_644L).build())
                    .setName("Nursing report")
                    .build())
            .build();
    selectionData = new SelectionData("note_noAttribute", serializeToJson(data));
    dataFeatureOutputs = filterBuilderNoAttr.buildForDataFeature(underlay, List.of(selectionData));
    assertEquals(1, dataFeatureOutputs.size());
    expectedDataFeatureFilter =
        new OccurrenceForPrimaryFilter(
            underlay,
            criteriaOccurrence,
            occurrenceEntity,
            null,
            new AttributeFilter(
                underlay,
                criteriaOccurrence.getCriteriaEntity(),
                occurrenceEntity.getIdAttribute(),
                BinaryOperator.EQUALS,
                Literal.forInt64(44_814_644L)));
    assertEquals(
        List.of(EntityOutput.filtered(occurrenceEntity, expectedDataFeatureFilter)),
        dataFeatureOutputs);

    // Multiple ids, with text search filter.
    data =
        DTTextSearch.TextSearch.newBuilder()
            .setQuery("ambulance")
            .addCategories(
                DTTextSearch.TextSearch.Selection.newBuilder()
                    .setValue(ValueOuterClass.Value.newBuilder().setInt64Value(44_814_644L).build())
                    .setName("Nursing report")
                    .build())
            .addCategories(
                DTTextSearch.TextSearch.Selection.newBuilder()
                    .setValue(ValueOuterClass.Value.newBuilder().setInt64Value(44_814_638L).build())
                    .setName("Admission note")
                    .build())
            .build();
    selectionData = new SelectionData("note_noAttribute", serializeToJson(data));
    dataFeatureOutputs = filterBuilderNoAttr.buildForDataFeature(underlay, List.of(selectionData));
    assertEquals(1, dataFeatureOutputs.size());
    EntityFilter expectedCriteriaFilter =
        new OccurrenceForPrimaryFilter(
            underlay,
            criteriaOccurrence,
            occurrenceEntity,
            null,
            new AttributeFilter(
                underlay,
                criteriaOccurrence.getCriteriaEntity(),
                occurrenceEntity.getIdAttribute(),
                NaryOperator.IN,
                List.of(Literal.forInt64(44_814_644L), Literal.forInt64(44_814_638L))));
    EntityFilter expectedTextSearchFilter =
        new TextSearchFilter(
            underlay,
            occurrenceEntity,
            TextSearchFilter.TextSearchOperator.EXACT_MATCH,
            "ambulance",
            null);
    expectedDataFeatureFilter =
        new BooleanAndOrFilter(
            BooleanAndOrFilter.LogicalOperator.AND,
            List.of(expectedCriteriaFilter, expectedTextSearchFilter));
    assertEquals(
        List.of(EntityOutput.filtered(occurrenceEntity, expectedDataFeatureFilter)),
        dataFeatureOutputs);
  }

  @Test
  void emptySelectionDataFeatureFilter() {
    CFTextSearch.TextSearch configWithAttr =
        CFTextSearch.TextSearch.newBuilder().setEntity("note").setSearchAttribute("title").build();
    CriteriaSelector criteriaSelectorWithAttr =
        new CriteriaSelector(
            "note_withAttribute",
            true,
            true,
            "core.TextSearchFilterBuilder",
            SZCorePlugin.TEXT_SEARCH.getIdInConfig(),
            serializeToJson(configWithAttr),
            List.of());
    TextSearchFilterBuilder filterBuilder = new TextSearchFilterBuilder(criteriaSelectorWithAttr);
    EntityOutput expectedEntityOutput =
        EntityOutput.unfiltered(underlay.getEntity("noteOccurrence"));

    // Null selection data.
    SelectionData selectionData = new SelectionData("note_withAttribute", null);
    List<EntityOutput> dataFeatureOutputs =
        filterBuilder.buildForDataFeature(underlay, List.of(selectionData));
    assertEquals(1, dataFeatureOutputs.size());
    assertEquals(expectedEntityOutput, dataFeatureOutputs.get(0));

    // Empty string selection data.
    selectionData = new SelectionData("note_withAttribute", "");
    dataFeatureOutputs = filterBuilder.buildForDataFeature(underlay, List.of(selectionData));
    assertEquals(1, dataFeatureOutputs.size());
    assertEquals(expectedEntityOutput, dataFeatureOutputs.get(0));
  }
}

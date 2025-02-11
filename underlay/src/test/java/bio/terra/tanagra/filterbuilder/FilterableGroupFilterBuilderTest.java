package bio.terra.tanagra.filterbuilder;

import static bio.terra.tanagra.UnderlayTestConfigs.AOUSC2023Q3R2;
import static bio.terra.tanagra.api.filter.BooleanAndOrFilter.newBooleanAndOrFilter;
import static bio.terra.tanagra.utils.ProtobufUtils.serializeToJson;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import bio.terra.tanagra.api.filter.AttributeFilter;
import bio.terra.tanagra.api.filter.BooleanAndOrFilter.LogicalOperator;
import bio.terra.tanagra.api.filter.EntityFilter;
import bio.terra.tanagra.api.filter.GroupHasItemsFilter;
import bio.terra.tanagra.api.filter.ItemInGroupFilter;
import bio.terra.tanagra.api.shared.BinaryOperator;
import bio.terra.tanagra.api.shared.Literal;
import bio.terra.tanagra.api.shared.NaryOperator;
import bio.terra.tanagra.api.shared.UnaryOperator;
import bio.terra.tanagra.exception.InvalidQueryException;
import bio.terra.tanagra.filterbuilder.impl.core.FilterableGroupFilterBuilder;
import bio.terra.tanagra.proto.criteriaselector.KeyOuterClass;
import bio.terra.tanagra.proto.criteriaselector.ValueDataOuterClass.ValueData;
import bio.terra.tanagra.proto.criteriaselector.ValueOuterClass.Value;
import bio.terra.tanagra.proto.criteriaselector.configschema.CFFilterableGroup;
import bio.terra.tanagra.proto.criteriaselector.dataschema.DTFilterableGroup.FilterableGroup;
import bio.terra.tanagra.proto.criteriaselector.dataschema.DTFilterableGroup.FilterableGroup.SelectAll;
import bio.terra.tanagra.proto.criteriaselector.dataschema.DTFilterableGroup.FilterableGroup.Selection;
import bio.terra.tanagra.proto.criteriaselector.dataschema.DTFilterableGroup.FilterableGroup.SingleSelect;
import bio.terra.tanagra.underlay.ConfigReader;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.entitymodel.Attribute;
import bio.terra.tanagra.underlay.entitymodel.Entity;
import bio.terra.tanagra.underlay.entitymodel.entitygroup.GroupItems;
import bio.terra.tanagra.underlay.serialization.SZCorePlugin;
import bio.terra.tanagra.underlay.serialization.SZService;
import bio.terra.tanagra.underlay.serialization.SZUnderlay;
import bio.terra.tanagra.underlay.uiplugin.CriteriaSelector;
import bio.terra.tanagra.underlay.uiplugin.SelectionData;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class FilterableGroupFilterBuilderTest {
  // Double escaped for Java then for JSON.
  private static final String configJson =
      """
{
  "entityGroup": "variantPerson",
  "searchConfigs": [
    {
      "name": "RS number",
      "example": "rs558865434",
      "regex": "rs\\\\d+",
      "parameters": [
        {
          "attribute": "rs_number",
          "operator": "OPERATOR_EQUALS"
        }
      ]
    },
    {
      "name": "Variant id",
      "example": "20-38623282-G-A",
      "regex": "\\\\d+-\\\\d+-\\\\w+-\\\\w+",
      "parameters": [
        {
          "attribute": "id",
          "operator": "OPERATOR_EQUALS"
        }
      ]
    },
    {
      "name": "Genomic region",
      "example": "chr20:38623000-38623379",
      "regex": "(\\\\w+):(\\\\d+)-(\\\\d+)",
      "parameters": [
        {
          "attribute": "gene",
          "operator": "OPERATOR_EQUALS"
        },
        {
          "attribute": "allele_number",
          "operator": "OPERATOR_GREATER_THAN_OR_EQUAL"
        },
        {
          "attribute": "allele_number",
          "operator": "OPERATOR_LESS_THAN_OR_EQUAL"
        }
      ]
    },
    {
      "name": "Gene",
      "example": "WFDC2",
      "regex": "\\\\w+",
      "displayOrder": -1,
      "parameters": [
        {
          "attribute": "gene",
          "operator": "OPERATOR_EQUALS",
          "case": "CASE_UPPER"
        }
      ]
    }
  ]
}
      """;
  private static Underlay underlay;
  private static Entity entity_variant;
  private static GroupItems groupItems_variant;

  @BeforeAll
  static void setup() {
    ConfigReader configReader = ConfigReader.fromJarResources();
    SZService szService = configReader.readService(AOUSC2023Q3R2.fileName());
    SZUnderlay szUnderlay = configReader.readUnderlay(szService.underlay);
    underlay = Underlay.fromConfig(szService.bigQuery, szUnderlay, configReader);

    entity_variant = underlay.getEntity("variant");
    groupItems_variant = (GroupItems) underlay.getEntityGroup("variantPerson");
  }

  private static FilterableGroupFilterBuilder newFilterBuilder() {
    return new FilterableGroupFilterBuilder(
        new CriteriaSelector(
            "tanagra-variant",
            true,
            false,
            false,
            "core.FilterableGroupFilterBuilder",
            SZCorePlugin.FILTERABLE_GROUP.getIdInConfig(),
            configJson,
            List.of()));
  }

  @Test
  void singleSelectsFilter() {
    FilterableGroupFilterBuilder filterBuilder = newFilterBuilder();
    FilterableGroup.Builder dataBuilder = FilterableGroup.newBuilder();

    // empty selection (no ids)
    EntityFilter expectedSubFilter =
        new ItemInGroupFilter(underlay, groupItems_variant, null, List.of(), null, null);
    EntityFilter expectedCohortFilter =
        newBooleanAndOrFilter(LogicalOperator.OR, List.of(expectedSubFilter));

    SelectionData selectionData = new SelectionData(null, serializeToJson(dataBuilder.build()));
    EntityFilter cohortFilter = filterBuilder.buildForCohort(underlay, List.of(selectionData));
    assertEquals(expectedCohortFilter, cohortFilter);

    // multiple ids
    List<Long> selectedIds = List.of(123456L, 234567L, 345678L);

    EntityFilter expectedIdsSubFilter =
        new AttributeFilter(
            underlay,
            entity_variant,
            entity_variant.getIdAttribute(),
            NaryOperator.IN,
            selectedIds.stream().map(Literal::forInt64).toList());
    expectedSubFilter =
        new ItemInGroupFilter(
            underlay, groupItems_variant, expectedIdsSubFilter, List.of(), null, null);
    expectedCohortFilter = newBooleanAndOrFilter(LogicalOperator.OR, List.of(expectedSubFilter));

    selectedIds.forEach(
        id ->
            dataBuilder.addSelected(
                Selection.newBuilder()
                    .setSingle(
                        SingleSelect.newBuilder()
                            .setKey(KeyOuterClass.Key.newBuilder().setInt64Key(id).build()))
                    .build()));
    selectionData = new SelectionData(null, serializeToJson(dataBuilder.build()));
    cohortFilter = filterBuilder.buildForCohort(underlay, List.of(selectionData));
    assertEquals(expectedCohortFilter, cohortFilter);
  }

  @Test
  void selectAllQueryFilter() {
    FilterableGroupFilterBuilder filterBuilder = newFilterBuilder();

    // query format: rs_number ("rs[0-9]+")
    String query = "rs100343";
    FilterableGroup data =
        FilterableGroup.newBuilder()
            .addSelected(
                Selection.newBuilder().setAll(SelectAll.newBuilder().setQuery(query).build()))
            .build();

    EntityFilter expectedQuerySubFilter =
        new AttributeFilter(
            underlay,
            entity_variant,
            entity_variant.getAttribute("rs_number"),
            BinaryOperator.EQUALS,
            Literal.forString(query));
    EntityFilter expectedSubFilter =
        new ItemInGroupFilter(
            underlay, groupItems_variant, expectedQuerySubFilter, List.of(), null, null);
    EntityFilter expectedCohortFilter =
        newBooleanAndOrFilter(LogicalOperator.OR, List.of(expectedSubFilter));

    SelectionData selectionData = new SelectionData(null, serializeToJson(data));
    EntityFilter cohortFilter = filterBuilder.buildForCohort(underlay, List.of(selectionData));
    assertEquals(expectedCohortFilter, cohortFilter);

    // query format: id ("[0-9]+-[0-9]+-[A-Z]+-[A-Z]+")
    query = "12-34-AB-CD";
    data =
        FilterableGroup.newBuilder()
            .addSelected(
                Selection.newBuilder().setAll(SelectAll.newBuilder().setQuery(query).build()))
            .build();

    expectedQuerySubFilter =
        new AttributeFilter(
            underlay,
            entity_variant,
            entity_variant.getAttribute("id"),
            BinaryOperator.EQUALS,
            Literal.forString(query));
    expectedSubFilter =
        new ItemInGroupFilter(
            underlay, groupItems_variant, expectedQuerySubFilter, List.of(), null, null);
    expectedCohortFilter = newBooleanAndOrFilter(LogicalOperator.OR, List.of(expectedSubFilter));

    selectionData = new SelectionData(null, serializeToJson(data));
    cohortFilter = filterBuilder.buildForCohort(underlay, List.of(selectionData));
    assertEquals(expectedCohortFilter, cohortFilter);

    // query format: gene
    query = "xyz123";
    data =
        FilterableGroup.newBuilder()
            .addSelected(
                Selection.newBuilder().setAll(SelectAll.newBuilder().setQuery(query).build()))
            .build();

    expectedQuerySubFilter =
        new AttributeFilter(
            underlay,
            entity_variant,
            entity_variant.getAttribute("gene"),
            BinaryOperator.EQUALS,
            Literal.forString(query.toUpperCase()));
    expectedSubFilter =
        new ItemInGroupFilter(
            underlay, groupItems_variant, expectedQuerySubFilter, List.of(), null, null);
    expectedCohortFilter = newBooleanAndOrFilter(LogicalOperator.OR, List.of(expectedSubFilter));

    selectionData = new SelectionData(null, serializeToJson(data));
    cohortFilter = filterBuilder.buildForCohort(underlay, List.of(selectionData));
    assertEquals(expectedCohortFilter, cohortFilter);

    // query format: region
    query = "xyz:123-456";
    data =
        FilterableGroup.newBuilder()
            .addSelected(
                Selection.newBuilder().setAll(SelectAll.newBuilder().setQuery(query).build()))
            .build();

    expectedQuerySubFilter =
        newBooleanAndOrFilter(
            LogicalOperator.AND,
            List.of(
                new AttributeFilter(
                    underlay,
                    entity_variant,
                    entity_variant.getAttribute("gene"),
                    BinaryOperator.EQUALS,
                    Literal.forString("xyz")),
                new AttributeFilter(
                    underlay,
                    entity_variant,
                    entity_variant.getAttribute("allele_number"),
                    BinaryOperator.GREATER_THAN_OR_EQUAL,
                    Literal.forInt64(123L)),
                new AttributeFilter(
                    underlay,
                    entity_variant,
                    entity_variant.getAttribute("allele_number"),
                    BinaryOperator.LESS_THAN_OR_EQUAL,
                    Literal.forInt64(456L))));
    expectedSubFilter =
        new ItemInGroupFilter(
            underlay, groupItems_variant, expectedQuerySubFilter, List.of(), null, null);
    expectedCohortFilter = newBooleanAndOrFilter(LogicalOperator.OR, List.of(expectedSubFilter));

    selectionData = new SelectionData(null, serializeToJson(data));
    cohortFilter = filterBuilder.buildForCohort(underlay, List.of(selectionData));
    assertEquals(expectedCohortFilter, cohortFilter);

    // empty query
    query = "";
    data =
        FilterableGroup.newBuilder()
            .addSelected(
                Selection.newBuilder().setAll(SelectAll.newBuilder().setQuery(query).build()))
            .build();

    expectedSubFilter =
        new ItemInGroupFilter(underlay, groupItems_variant, null, List.of(), null, null);
    expectedCohortFilter = newBooleanAndOrFilter(LogicalOperator.OR, List.of(expectedSubFilter));

    selectionData = new SelectionData(null, serializeToJson(data));
    cohortFilter = filterBuilder.buildForCohort(underlay, List.of(selectionData));
    assertEquals(expectedCohortFilter, cohortFilter);

    // all other entityGroups
    CFFilterableGroup.FilterableGroup otherConfig =
        CFFilterableGroup.FilterableGroup.newBuilder()
            .setEntityGroup("heartRateSummaryPerson")
            .build();
    filterBuilder =
        new FilterableGroupFilterBuilder(
            new CriteriaSelector(
                "tanagra-variant",
                true,
                false,
                false,
                "core.FilterableGroupFilterBuilder",
                SZCorePlugin.FILTERABLE_GROUP.getIdInConfig(),
                serializeToJson(otherConfig),
                List.of()));

    expectedSubFilter =
        new GroupHasItemsFilter(
            underlay,
            (GroupItems) underlay.getEntityGroup(otherConfig.getEntityGroup()),
            null,
            List.of(),
            null,
            null);
    expectedCohortFilter = newBooleanAndOrFilter(LogicalOperator.OR, List.of(expectedSubFilter));

    selectionData = new SelectionData(null, serializeToJson(data));
    cohortFilter = filterBuilder.buildForCohort(underlay, List.of(selectionData));
    assertEquals(expectedCohortFilter, cohortFilter);
  }

  @Test
  void selectAllValueDataFilter() {
    FilterableGroupFilterBuilder filterBuilder = newFilterBuilder();
    Attribute attribute = entity_variant.getAttribute("consequence");

    // single attribute value in filter
    ValueData.Selection selection1 =
        ValueData.Selection.newBuilder()
            .setValue(Value.newBuilder().setStringValue("abcdef").build())
            .build();
    ValueData valueData =
        ValueData.newBuilder().setAttribute(attribute.getName()).addSelected(selection1).build();
    FilterableGroup data =
        FilterableGroup.newBuilder()
            .addSelected(
                Selection.newBuilder()
                    .setAll(SelectAll.newBuilder().addValueData(valueData).build()))
            .build();

    EntityFilter expectedValueDataSubFilter =
        new AttributeFilter(
            underlay,
            entity_variant,
            attribute,
            BinaryOperator.EQUALS,
            Literal.forString(selection1.getValue().getStringValue()));
    EntityFilter expectedSubFilter =
        new ItemInGroupFilter(
            underlay, groupItems_variant, expectedValueDataSubFilter, List.of(), null, null);
    EntityFilter expectedCohortFilter =
        newBooleanAndOrFilter(LogicalOperator.OR, List.of(expectedSubFilter));

    SelectionData selectionData = new SelectionData(null, serializeToJson(data));
    EntityFilter cohortFilter = filterBuilder.buildForCohort(underlay, List.of(selectionData));
    assertEquals(expectedCohortFilter, cohortFilter);

    // Multiple attribute values including n/a in filter
    ValueData.Selection selection2 =
        ValueData.Selection.newBuilder()
            .setValue(Value.newBuilder().setStringValue("ghijkl").build())
            .build();
    ValueData.Selection selectionNA =
        ValueData.Selection.newBuilder()
            .setValue(
                Value.newBuilder().setStringValue(Attribute.DEFAULT_EMPTY_VALUE_DISPLAY).build())
            .build();
    valueData =
        ValueData.newBuilder()
            .setAttribute(attribute.getName())
            .addSelected(selection1)
            .addSelected(selectionNA)
            .addSelected(selection2)
            .build();
    data =
        FilterableGroup.newBuilder()
            .addSelected(
                Selection.newBuilder()
                    .setAll(SelectAll.newBuilder().addValueData(valueData).build()))
            .build();

    expectedValueDataSubFilter =
        newBooleanAndOrFilter(
            LogicalOperator.OR,
            List.of(
                new AttributeFilter(underlay, entity_variant, attribute, UnaryOperator.IS_NULL),
                new AttributeFilter(
                    underlay,
                    entity_variant,
                    attribute,
                    NaryOperator.IN,
                    List.of(
                        Literal.forString(selection1.getValue().getStringValue()),
                        Literal.forString(selection2.getValue().getStringValue())))));
    expectedSubFilter =
        new ItemInGroupFilter(
            underlay, groupItems_variant, expectedValueDataSubFilter, List.of(), null, null);
    expectedCohortFilter = newBooleanAndOrFilter(LogicalOperator.OR, List.of(expectedSubFilter));

    selectionData = new SelectionData(null, serializeToJson(data));
    cohortFilter = filterBuilder.buildForCohort(underlay, List.of(selectionData));
    assertEquals(expectedCohortFilter, cohortFilter);
  }

  @Test
  void selectAllExclusionFilter() {
    FilterableGroupFilterBuilder filterBuilder = newFilterBuilder();

    // empty selection (no ids)
    SelectAll.Builder selectAllBuilder = SelectAll.newBuilder();

    EntityFilter expectedSubFilter =
        new ItemInGroupFilter(underlay, groupItems_variant, null, List.of(), null, null);
    EntityFilter expectedCohortFilter =
        newBooleanAndOrFilter(LogicalOperator.OR, List.of(expectedSubFilter));

    FilterableGroup data =
        FilterableGroup.newBuilder()
            .addSelected(Selection.newBuilder().setAll(selectAllBuilder.build()))
            .build();

    SelectionData selectionData = new SelectionData(null, serializeToJson(data));
    EntityFilter cohortFilter = filterBuilder.buildForCohort(underlay, List.of(selectionData));
    assertEquals(expectedCohortFilter, cohortFilter);

    // single id
    long excludedId = 456789L;

    EntityFilter expectedIdsSubFilter =
        new AttributeFilter(
            underlay,
            entity_variant,
            entity_variant.getIdAttribute(),
            BinaryOperator.NOT_EQUALS,
            Literal.forInt64(excludedId));
    expectedSubFilter =
        new ItemInGroupFilter(
            underlay, groupItems_variant, expectedIdsSubFilter, List.of(), null, null);
    expectedCohortFilter = newBooleanAndOrFilter(LogicalOperator.OR, List.of(expectedSubFilter));

    selectAllBuilder.addExclusions(
        SingleSelect.newBuilder()
            .setKey(KeyOuterClass.Key.newBuilder().setInt64Key(excludedId).build()));

    data =
        FilterableGroup.newBuilder()
            .addSelected(Selection.newBuilder().setAll(selectAllBuilder.build()))
            .build();

    selectionData = new SelectionData(null, serializeToJson(data));
    cohortFilter = filterBuilder.buildForCohort(underlay, List.of(selectionData));
    assertEquals(expectedCohortFilter, cohortFilter);

    // multiple ids
    List<Long> excludedIds = List.of(123456L, 234567L, 345678L);

    expectedIdsSubFilter =
        new AttributeFilter(
            underlay,
            entity_variant,
            entity_variant.getIdAttribute(),
            NaryOperator.NOT_IN,
            excludedIds.stream().map(Literal::forInt64).toList());
    expectedSubFilter =
        new ItemInGroupFilter(
            underlay, groupItems_variant, expectedIdsSubFilter, List.of(), null, null);
    expectedCohortFilter = newBooleanAndOrFilter(LogicalOperator.OR, List.of(expectedSubFilter));

    selectAllBuilder.clearExclusions();
    excludedIds.forEach(
        id ->
            selectAllBuilder
                .addExclusions(
                    SingleSelect.newBuilder()
                        .setKey(KeyOuterClass.Key.newBuilder().setInt64Key(id).build()))
                .build());
    data =
        FilterableGroup.newBuilder()
            .addSelected(Selection.newBuilder().setAll(selectAllBuilder.build()))
            .build();

    selectionData = new SelectionData(null, serializeToJson(data));
    cohortFilter = filterBuilder.buildForCohort(underlay, List.of(selectionData));
    assertEquals(expectedCohortFilter, cohortFilter);
  }

  @Test
  void allFilter() {
    FilterableGroupFilterBuilder filterBuilder = newFilterBuilder();

    List<EntityFilter> expectedSubFilters = new ArrayList<>();
    FilterableGroup.Builder dataBuilder = FilterableGroup.newBuilder();

    // query
    String query = "rs100343";
    EntityFilter expectedQuerySubFilter =
        new AttributeFilter(
            underlay,
            entity_variant,
            entity_variant.getAttribute("rs_number"),
            BinaryOperator.EQUALS,
            Literal.forString(query));

    // value-data
    String attr = "consequence";
    String attrVal = "abcdef";
    EntityFilter expectedValueDataSubFilter =
        new AttributeFilter(
            underlay,
            entity_variant,
            entity_variant.getAttribute(attr),
            BinaryOperator.EQUALS,
            Literal.forString(attrVal));

    // exclusion
    long excludedId = 456789L;
    EntityFilter expectedExclusionsSubFilter =
        new AttributeFilter(
            underlay,
            entity_variant,
            entity_variant.getIdAttribute(),
            BinaryOperator.NOT_EQUALS,
            Literal.forInt64(excludedId));

    // Add multiple select-all
    expectedSubFilters.add(
        new ItemInGroupFilter(
            underlay,
            groupItems_variant,
            newBooleanAndOrFilter(
                LogicalOperator.AND, List.of(expectedQuerySubFilter, expectedValueDataSubFilter)),
            List.of(),
            null,
            null));
    expectedSubFilters.add(
        new ItemInGroupFilter(
            underlay,
            groupItems_variant,
            newBooleanAndOrFilter(
                LogicalOperator.AND,
                List.of(expectedValueDataSubFilter, expectedExclusionsSubFilter)),
            List.of(),
            null,
            null));

    ValueData valueData =
        ValueData.newBuilder()
            .setAttribute(attr)
            .addSelected(
                ValueData.Selection.newBuilder()
                    .setValue(Value.newBuilder().setStringValue(attrVal).build())
                    .build())
            .build();
    SingleSelect exclusion =
        SingleSelect.newBuilder()
            .setKey(KeyOuterClass.Key.newBuilder().setInt64Key(excludedId).build())
            .build();

    SelectAll selectAll1 = SelectAll.newBuilder().setQuery(query).addValueData(valueData).build();
    SelectAll selectAll2 =
        SelectAll.newBuilder().addValueData(valueData).addExclusions(exclusion).build();

    dataBuilder
        .addSelected(Selection.newBuilder().setAll(selectAll1).build())
        .addSelected(Selection.newBuilder().setAll(selectAll2).build());

    // Add multiple single-select
    List<Long> selectedIds = List.of(123456L, 234567L, 345678L);

    EntityFilter expectedIdsSubFilter =
        new AttributeFilter(
            underlay,
            entity_variant,
            entity_variant.getIdAttribute(),
            NaryOperator.IN,
            selectedIds.stream().map(Literal::forInt64).toList());
    expectedSubFilters.add(
        new ItemInGroupFilter(
            underlay, groupItems_variant, expectedIdsSubFilter, List.of(), null, null));

    selectedIds.forEach(
        id ->
            dataBuilder.addSelected(
                Selection.newBuilder()
                    .setSingle(
                        SingleSelect.newBuilder()
                            .setKey(KeyOuterClass.Key.newBuilder().setInt64Key(id).build()))
                    .build()));

    EntityFilter expectedCohortFilter =
        newBooleanAndOrFilter(LogicalOperator.OR, expectedSubFilters);

    SelectionData selectionData = new SelectionData(null, serializeToJson(dataBuilder.build()));
    EntityFilter cohortFilter = filterBuilder.buildForCohort(underlay, List.of(selectionData));
    assertEquals(expectedCohortFilter, cohortFilter);
  }

  @Test
  void invalid() {
    List<FilterableGroupFilterBuilder> filterBuilderList = List.of(newFilterBuilder());
    // empty selection
    assertThrows(
        InvalidQueryException.class,
        () -> filterBuilderList.get(0).buildForCohort(underlay, List.of()));

    // selection item neither singleSelect, nor selectAll
    FilterableGroup data =
        FilterableGroup.newBuilder().addSelected(Selection.newBuilder().build()).build();
    assertThrows(
        InvalidQueryException.class,
        () ->
            filterBuilderList
                .get(0)
                .buildForCohort(underlay, List.of(new SelectionData(null, serializeToJson(data)))));
  }
}

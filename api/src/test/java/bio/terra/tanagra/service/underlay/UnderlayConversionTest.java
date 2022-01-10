package bio.terra.tanagra.service.underlay;

import static bio.terra.tanagra.service.underlay.NauticalUnderlayUtils.BOAT;
import static bio.terra.tanagra.service.underlay.NauticalUnderlayUtils.BOAT_BT_ID_COL;
import static bio.terra.tanagra.service.underlay.NauticalUnderlayUtils.BOAT_COLOR;
import static bio.terra.tanagra.service.underlay.NauticalUnderlayUtils.BOAT_COLOR_COL;
import static bio.terra.tanagra.service.underlay.NauticalUnderlayUtils.BOAT_ENGINE;
import static bio.terra.tanagra.service.underlay.NauticalUnderlayUtils.BOAT_ENGINE_ID;
import static bio.terra.tanagra.service.underlay.NauticalUnderlayUtils.BOAT_ENGINE_NAME;
import static bio.terra.tanagra.service.underlay.NauticalUnderlayUtils.BOAT_ID;
import static bio.terra.tanagra.service.underlay.NauticalUnderlayUtils.BOAT_ID_COL;
import static bio.terra.tanagra.service.underlay.NauticalUnderlayUtils.BOAT_NAME;
import static bio.terra.tanagra.service.underlay.NauticalUnderlayUtils.BOAT_NAME_COL;
import static bio.terra.tanagra.service.underlay.NauticalUnderlayUtils.BOAT_PARTS_ID_COL;
import static bio.terra.tanagra.service.underlay.NauticalUnderlayUtils.BOAT_PARTS_NAME_COL;
import static bio.terra.tanagra.service.underlay.NauticalUnderlayUtils.BOAT_PARTS_TYPE_COL;
import static bio.terra.tanagra.service.underlay.NauticalUnderlayUtils.BOAT_PARTS_TYPE_COL_ENGINE_VALUE;
import static bio.terra.tanagra.service.underlay.NauticalUnderlayUtils.BOAT_PARTS_TYPE_COL_OPERATOR;
import static bio.terra.tanagra.service.underlay.NauticalUnderlayUtils.BOAT_RESERVATION_RELATIONSHIP;
import static bio.terra.tanagra.service.underlay.NauticalUnderlayUtils.BOAT_TYPE_DESCENDANTS_ANCESTOR_COL;
import static bio.terra.tanagra.service.underlay.NauticalUnderlayUtils.BOAT_TYPE_DESCENDANTS_DESCENDANT_COL;
import static bio.terra.tanagra.service.underlay.NauticalUnderlayUtils.BOAT_TYPE_ID;
import static bio.terra.tanagra.service.underlay.NauticalUnderlayUtils.BOAT_TYPE_ID_COL;
import static bio.terra.tanagra.service.underlay.NauticalUnderlayUtils.BOAT_TYPE_NAME;
import static bio.terra.tanagra.service.underlay.NauticalUnderlayUtils.BOAT_TYPE_NAME_COL;
import static bio.terra.tanagra.service.underlay.NauticalUnderlayUtils.RESERVATION;
import static bio.terra.tanagra.service.underlay.NauticalUnderlayUtils.RESERVATION_B_ID;
import static bio.terra.tanagra.service.underlay.NauticalUnderlayUtils.RESERVATION_B_ID_COL;
import static bio.terra.tanagra.service.underlay.NauticalUnderlayUtils.RESERVATION_DAY;
import static bio.terra.tanagra.service.underlay.NauticalUnderlayUtils.RESERVATION_DAY_COL;
import static bio.terra.tanagra.service.underlay.NauticalUnderlayUtils.RESERVATION_ID;
import static bio.terra.tanagra.service.underlay.NauticalUnderlayUtils.RESERVATION_ID_COL;
import static bio.terra.tanagra.service.underlay.NauticalUnderlayUtils.RESERVATION_S_ID;
import static bio.terra.tanagra.service.underlay.NauticalUnderlayUtils.RESERVATION_S_ID_COL;
import static bio.terra.tanagra.service.underlay.NauticalUnderlayUtils.SAILOR;
import static bio.terra.tanagra.service.underlay.NauticalUnderlayUtils.SAILORS_FAVORITE_BOATS_B_ID_COL;
import static bio.terra.tanagra.service.underlay.NauticalUnderlayUtils.SAILORS_FAVORITE_BOATS_S_ID_COL;
import static bio.terra.tanagra.service.underlay.NauticalUnderlayUtils.SAILOR_BOAT_RELATIONSHIP;
import static bio.terra.tanagra.service.underlay.NauticalUnderlayUtils.SAILOR_ID;
import static bio.terra.tanagra.service.underlay.NauticalUnderlayUtils.SAILOR_ID_COL;
import static bio.terra.tanagra.service.underlay.NauticalUnderlayUtils.SAILOR_NAME;
import static bio.terra.tanagra.service.underlay.NauticalUnderlayUtils.SAILOR_NAME_COL;
import static bio.terra.tanagra.service.underlay.NauticalUnderlayUtils.SAILOR_RATING;
import static bio.terra.tanagra.service.underlay.NauticalUnderlayUtils.SAILOR_RATING_COL;
import static bio.terra.tanagra.service.underlay.NauticalUnderlayUtils.SAILOR_RESERVATION_RELATIONSHIP;
import static bio.terra.tanagra.service.underlay.NauticalUnderlayUtils.loadNauticalUnderlayProto;
import static org.junit.jupiter.api.Assertions.assertEquals;

import bio.terra.tanagra.proto.underlay.EnumHint;
import bio.terra.tanagra.proto.underlay.EnumHintValue;
import bio.terra.tanagra.proto.underlay.FilterableAttribute;
import bio.terra.tanagra.proto.underlay.IntegerBoundsHint;
import bio.terra.tanagra.service.search.Attribute;
import bio.terra.tanagra.service.underlay.AttributeMapping.LookupColumn;
import bio.terra.tanagra.service.underlay.Hierarchy.DescendantsTable;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableTable;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
public class UnderlayConversionTest {
  @Test
  void convertUnderlay() throws Exception {
    Underlay nautical = UnderlayConversion.convert(loadNauticalUnderlayProto());

    assertEquals("nautical_underlay", nautical.name());
    assertEquals(
        ImmutableMap.builder()
            .put(SAILOR.name(), SAILOR)
            .put(BOAT.name(), BOAT)
            .put(RESERVATION.name(), RESERVATION)
            .put(BOAT_ENGINE.name(), BOAT_ENGINE)
            .build(),
        nautical.entities());
    assertEquals(
        ImmutableTable.builder()
            .put(SAILOR, "id", SAILOR_ID)
            .put(SAILOR, "name", SAILOR_NAME)
            .put(SAILOR, "rating", SAILOR_RATING)
            .put(BOAT, "id", BOAT_ID)
            .put(BOAT, "name", BOAT_NAME)
            .put(BOAT, "color", BOAT_COLOR)
            .put(BOAT, "type_name", BOAT_TYPE_NAME)
            .put(BOAT, "type_id", BOAT_TYPE_ID)
            .put(RESERVATION, "id", RESERVATION_ID)
            .put(RESERVATION, "boats_id", RESERVATION_B_ID)
            .put(RESERVATION, "sailors_id", RESERVATION_S_ID)
            .put(RESERVATION, "day", RESERVATION_DAY)
            .put(BOAT_ENGINE, "id", BOAT_ENGINE_ID)
            .put(BOAT_ENGINE, "name", BOAT_ENGINE_NAME)
            .build(),
        nautical.attributes());
    assertEquals(
        ImmutableMap.builder()
            .put(SAILOR_RESERVATION_RELATIONSHIP.name(), SAILOR_RESERVATION_RELATIONSHIP)
            .put(BOAT_RESERVATION_RELATIONSHIP.name(), BOAT_RESERVATION_RELATIONSHIP)
            .put(SAILOR_BOAT_RELATIONSHIP.name(), SAILOR_BOAT_RELATIONSHIP)
            .build(),
        nautical.relationships());
    assertEquals(
        ImmutableMap.builder()
            .put(SAILOR, SAILOR_ID_COL)
            .put(BOAT, BOAT_ID_COL)
            .put(RESERVATION, RESERVATION_ID_COL)
            .put(BOAT_ENGINE, BOAT_PARTS_ID_COL)
            .build(),
        nautical.primaryKeys());
    assertEquals(
        ImmutableMap.builder()
            .put(
                BOAT_ENGINE,
                TableFilter.builder()
                    .binaryColumnFilter(
                        BinaryColumnFilter.create(
                            BOAT_PARTS_TYPE_COL,
                            BOAT_PARTS_TYPE_COL_OPERATOR,
                            BOAT_PARTS_TYPE_COL_ENGINE_VALUE))
                    .build())
            .build(),
        nautical.tableFilters());
    assertEquals(
        ImmutableMap.builder()
            .put(SAILOR_ID, AttributeMapping.SimpleColumn.create(SAILOR_ID, SAILOR_ID_COL))
            .put(SAILOR_NAME, AttributeMapping.SimpleColumn.create(SAILOR_NAME, SAILOR_NAME_COL))
            .put(
                SAILOR_RATING,
                AttributeMapping.SimpleColumn.create(SAILOR_RATING, SAILOR_RATING_COL))
            .put(BOAT_ID, AttributeMapping.SimpleColumn.create(BOAT_ID, BOAT_ID_COL))
            .put(BOAT_NAME, AttributeMapping.SimpleColumn.create(BOAT_NAME, BOAT_NAME_COL))
            .put(BOAT_COLOR, AttributeMapping.SimpleColumn.create(BOAT_COLOR, BOAT_COLOR_COL))
            .put(BOAT_TYPE_ID, AttributeMapping.SimpleColumn.create(BOAT_TYPE_ID, BOAT_BT_ID_COL))
            .put(
                BOAT_TYPE_NAME,
                LookupColumn.builder()
                    .attribute(BOAT_TYPE_NAME)
                    .primaryTableLookupKey(BOAT_BT_ID_COL)
                    .lookupTableKey(BOAT_TYPE_ID_COL)
                    .lookupColumn(BOAT_TYPE_NAME_COL)
                    .build())
            .put(
                RESERVATION_ID,
                AttributeMapping.SimpleColumn.create(RESERVATION_ID, RESERVATION_ID_COL))
            .put(
                RESERVATION_S_ID,
                AttributeMapping.SimpleColumn.create(RESERVATION_S_ID, RESERVATION_S_ID_COL))
            .put(
                RESERVATION_B_ID,
                AttributeMapping.SimpleColumn.create(RESERVATION_B_ID, RESERVATION_B_ID_COL))
            .put(
                RESERVATION_DAY,
                AttributeMapping.SimpleColumn.create(RESERVATION_DAY, RESERVATION_DAY_COL))
            .put(
                BOAT_ENGINE_ID,
                AttributeMapping.SimpleColumn.create(BOAT_ENGINE_ID, BOAT_PARTS_ID_COL))
            .put(
                BOAT_ENGINE_NAME,
                AttributeMapping.SimpleColumn.create(BOAT_ENGINE_NAME, BOAT_PARTS_NAME_COL))
            .build(),
        nautical.attributeMappings());
    assertEquals(
        ImmutableMap.builder()
            .put(
                SAILOR_RESERVATION_RELATIONSHIP,
                ForeignKey.builder()
                    .primaryKey(SAILOR_ID_COL)
                    .foreignKey(RESERVATION_S_ID_COL)
                    .build())
            .put(
                BOAT_RESERVATION_RELATIONSHIP,
                ForeignKey.builder()
                    .primaryKey(BOAT_ID_COL)
                    .foreignKey(RESERVATION_B_ID_COL)
                    .build())
            .put(
                SAILOR_BOAT_RELATIONSHIP,
                IntermediateTable.builder()
                    .entity1EntityTableKey(SAILOR_ID_COL)
                    .entity1IntermediateTableKey(SAILORS_FAVORITE_BOATS_S_ID_COL)
                    .entity2EntityTableKey(BOAT_ID_COL)
                    .entity2IntermediateTableKey(SAILORS_FAVORITE_BOATS_B_ID_COL)
                    .build())
            .build(),
        nautical.relationshipMappings());
    assertEquals(
        ImmutableMap.builder()
            .put(
                BOAT_TYPE_ID,
                Hierarchy.builder()
                    .descendantsTable(
                        DescendantsTable.builder()
                            .ancestor(BOAT_TYPE_DESCENDANTS_ANCESTOR_COL)
                            .descendant(BOAT_TYPE_DESCENDANTS_DESCENDANT_COL)
                            .build())
                    .build())
            .build(),
        nautical.hierarchies());
    assertEquals(
        ImmutableMap.builder()
            .put(
                SAILOR,
                EntityFiltersSchema.builder()
                    .entity(SAILOR)
                    .filterableAttributes(
                        ImmutableMap.<Attribute, FilterableAttribute>builder()
                            .put(
                                SAILOR_NAME,
                                FilterableAttribute.newBuilder()
                                    .setAttributeName(SAILOR_NAME.name())
                                    .build())
                            .put(
                                SAILOR_RATING,
                                FilterableAttribute.newBuilder()
                                    .setAttributeName(SAILOR_RATING.name())
                                    .setIntegerBoundsHint(
                                        IntegerBoundsHint.newBuilder().setMin(0).setMax(10))
                                    .build())
                            .build())
                    .filterableRelationships(ImmutableSet.of(SAILOR_RESERVATION_RELATIONSHIP))
                    .build())
            .put(
                RESERVATION,
                EntityFiltersSchema.builder()
                    .entity(RESERVATION)
                    .filterableAttributes(
                        ImmutableMap.<Attribute, FilterableAttribute>builder()
                            .put(
                                RESERVATION_DAY,
                                FilterableAttribute.newBuilder()
                                    .setAttributeName(RESERVATION_DAY.name())
                                    .setEnumHint(
                                        EnumHint.newBuilder()
                                            .addEnumHintValues(
                                                EnumHintValue.newBuilder()
                                                    .setDisplayName("Monday")
                                                    .setStringVal("MONDAY"))
                                            .addEnumHintValues(
                                                EnumHintValue.newBuilder()
                                                    .setDisplayName("Tuesday")
                                                    .setStringVal("TUESDAY"))
                                            .addEnumHintValues(
                                                EnumHintValue.newBuilder()
                                                    .setDisplayName("Wednesday")
                                                    .setStringVal("WEDNESDAY")))
                                    .build())
                            .build())
                    .filterableRelationships(ImmutableSet.of())
                    .build())
            .build(),
        nautical.entityFiltersSchemas());
  }
}

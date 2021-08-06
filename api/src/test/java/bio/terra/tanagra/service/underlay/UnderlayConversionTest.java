package bio.terra.tanagra.service.underlay;

import static bio.terra.tanagra.service.underlay.NauticalUnderlayUtils.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableTable;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
public class UnderlayConversionTest {
  @Test
  void convertUnderlay() throws Exception {
    Underlay nautical = UnderlayConversion.convert(loadNauticalUnderlayProto());

    assertEquals("Nautical Underlay", nautical.name());
    assertEquals(
        ImmutableMap.builder()
            .put(SAILOR.name(), SAILOR)
            .put(BOAT.name(), BOAT)
            .put(RESERVATION.name(), RESERVATION)
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
            .put(RESERVATION, "id", RESERVATION_ID)
            .put(RESERVATION, "boats_id", RESERVATION_B_ID)
            .put(RESERVATION, "sailors_id", RESERVATION_S_ID)
            .put(RESERVATION, "day", RESERVATION_DAY)
            .build(),
        nautical.attributes());
    assertEquals(
        ImmutableMap.builder()
            .put(SAILOR_RESERVATION_RELATIONSHIP.name(), SAILOR_RESERVATION_RELATIONSHIP)
            .put(BOAT_RESERVATION_RELATIONSHIP.name(), BOAT_RESERVATION_RELATIONSHIP)
            .build(),
        nautical.relationships());
    assertEquals(
        ImmutableMap.builder()
            .put(SAILOR, SAILOR_ID_COL)
            .put(BOAT, BOAT_ID_COL)
            .put(RESERVATION, RESERVATION_ID_COL)
            .build(),
        nautical.primaryKeys());
    assertEquals(
        ImmutableMap.builder()
            .put(SAILOR_ID, SAILOR_ID_COL)
            .put(SAILOR_NAME, SAILOR_NAME_COL)
            .put(SAILOR_RATING, SAILOR_RATING_COL)
            .put(BOAT_ID, BOAT_ID_COL)
            .put(BOAT_NAME, BOAT_NAME_COL)
            .put(BOAT_COLOR, BOAT_COLOR_COL)
            .put(RESERVATION_ID, RESERVATION_ID_COL)
            .put(RESERVATION_S_ID, RESERVATION_S_ID_COL)
            .put(RESERVATION_B_ID, RESERVATION_B_ID_COL)
            .put(RESERVATION_DAY, RESERVATION_DAY_COL)
            .build(),
        nautical.simpleAttributesToColumns());
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
            .build(),
        nautical.foreignKeys());
  }
}

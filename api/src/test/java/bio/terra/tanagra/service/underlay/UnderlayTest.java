package bio.terra.tanagra.service.underlay;

import static bio.terra.tanagra.service.underlay.NauticalUnderlayUtils.BOAT;
import static bio.terra.tanagra.service.underlay.NauticalUnderlayUtils.BOAT_ENGINE;
import static bio.terra.tanagra.service.underlay.NauticalUnderlayUtils.BOAT_RESERVATION_RELATIONSHIP;
import static bio.terra.tanagra.service.underlay.NauticalUnderlayUtils.RESERVATION;
import static bio.terra.tanagra.service.underlay.NauticalUnderlayUtils.SAILOR;
import static bio.terra.tanagra.service.underlay.NauticalUnderlayUtils.SAILOR_BOAT_RELATIONSHIP;
import static bio.terra.tanagra.service.underlay.NauticalUnderlayUtils.SAILOR_RESERVATION_RELATIONSHIP;
import static bio.terra.tanagra.service.underlay.NauticalUnderlayUtils.loadNauticalUnderlay;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import bio.terra.tanagra.service.search.Entity;
import java.util.Optional;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
public class UnderlayTest {
  private static final Underlay NAUTICAL_UNDERLAY = loadNauticalUnderlay();

  @Test
  void getRelationship() {
    assertEquals(
        Optional.of(SAILOR_RESERVATION_RELATIONSHIP),
        NAUTICAL_UNDERLAY.getRelationship(SAILOR, RESERVATION));
    assertEquals(
        Optional.of(SAILOR_RESERVATION_RELATIONSHIP),
        NAUTICAL_UNDERLAY.getRelationship(RESERVATION, SAILOR));
    assertEquals(
        Optional.of(SAILOR_BOAT_RELATIONSHIP), NAUTICAL_UNDERLAY.getRelationship(SAILOR, BOAT));
    assertEquals(Optional.empty(), NAUTICAL_UNDERLAY.getRelationship(SAILOR, BOAT_ENGINE));
  }

  @Test
  void getRelationshipsOf() {
    assertThat(
        NAUTICAL_UNDERLAY.getRelationshipsOf(SAILOR),
        Matchers.hasItems(SAILOR_RESERVATION_RELATIONSHIP, SAILOR_BOAT_RELATIONSHIP));
    assertThat(
        NAUTICAL_UNDERLAY.getRelationshipsOf(BOAT),
        Matchers.hasItems(BOAT_RESERVATION_RELATIONSHIP, SAILOR_BOAT_RELATIONSHIP));
    assertThat(
        NAUTICAL_UNDERLAY.getRelationshipsOf(Entity.builder().underlay("foo").name("bar").build()),
        Matchers.empty());
  }
}

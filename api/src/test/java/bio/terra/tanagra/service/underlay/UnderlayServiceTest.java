package bio.terra.tanagra.service.underlay;

import static bio.terra.tanagra.service.underlay.NauticalUnderlayUtils.NAUTICAL_UNDERLAY_NAME;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import bio.terra.tanagra.testing.BaseSpringUnitTest;
import java.util.Optional;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("nautical") // Use the 'application-nautical.yml' Spring profile.
public class UnderlayServiceTest extends BaseSpringUnitTest {
  @Autowired private UnderlayService underlayService;

  @Test
  void getUnderlay() {
    assertEquals(
        Optional.of(NauticalUnderlayUtils.loadNauticalUnderlay()),
        underlayService.getUnderlay(NAUTICAL_UNDERLAY_NAME));

    assertEquals(Optional.empty(), underlayService.getUnderlay("bogus-underlay"));
  }

  @Test
  void getUnderlays() {
    assertThat(
        underlayService.getUnderlays(),
        Matchers.hasItem(NauticalUnderlayUtils.loadNauticalUnderlay()));
  }
}

package bio.terra.tanagra.app.configuration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import bio.terra.tanagra.proto.underlay.Underlay;
import com.google.common.collect.ImmutableList;
import java.util.List;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
public class UnderlayConfigurationTest {
  /**
   * Test that we can load configuration from both yaml & prototext.
   *
   * <p>The test files should be kept in sync so that we can do an equivalence assertion in this
   * test.
   */
  @Test
  void prototextAndYamlUnderlayEquivalence() throws Exception {
    UnderlayConfiguration config = new UnderlayConfiguration();
    config.setUnderlayPrototextFiles(ImmutableList.of("underlays/nautical.pbtext"));
    config.setUnderlayYamlFiles(ImmutableList.of("underlays/nautical.yaml"));

    List<Underlay> underlays = config.getUnderlays();
    assertThat(underlays, Matchers.hasSize(2));
    assertEquals(underlays.get(0), underlays.get(1));
  }
}

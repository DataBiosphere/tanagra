package bio.terra.tanagra.plugin;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bio.terra.tanagra.service.accesscontrol.AccessControlPlugin;
import bio.terra.tanagra.service.accesscontrol.OpenAccessControlPlugin;
import bio.terra.tanagra.testing.BaseSpringUnitTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("plugin")
public class PluginTest extends BaseSpringUnitTest {
  private static final String TEST_UNDERLAY_DEFAULT = "test-no-plugins";
  private static final String TEST_UNDERLAY_CONFIGURED = "test-plugins-configured";

  @Autowired private PluginService pluginService;

  @Test
  void defaultPluginLoading() {
    AccessControlPlugin accessControlPlugin =
        pluginService.getAccessControlPlugin(TEST_UNDERLAY_DEFAULT);
    assertTrue(accessControlPlugin instanceof OpenAccessControlPlugin);
  }

  @Test
  void configuredPluginLoading() {
    AccessControlPlugin accessControlPlugin =
        pluginService.getAccessControlPlugin(TEST_UNDERLAY_CONFIGURED);
    assertFalse(accessControlPlugin instanceof ConfiguredAccessControlPlugin);
  }
}

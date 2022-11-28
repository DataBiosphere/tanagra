package bio.terra.tanagra.plugin;

import static org.junit.jupiter.api.Assertions.assertTrue;

import bio.terra.tanagra.service.accesscontrol.AccessControlPlugin;
import bio.terra.tanagra.service.accesscontrol.OpenAccessControlPlugin;
import bio.terra.tanagra.testing.BaseSpringUnitTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("plugin")
public class PluginTest extends BaseSpringUnitTest {
  @Autowired private PluginService pluginService;

  @Test
  void pluginLoading() {
    AccessControlPlugin accessControlPlugin = pluginService.getPlugin(AccessControlPlugin.class);
    assertTrue(accessControlPlugin instanceof OpenAccessControlPlugin);
  }
}

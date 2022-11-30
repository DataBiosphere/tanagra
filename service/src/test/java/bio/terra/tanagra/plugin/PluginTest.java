package bio.terra.tanagra.plugin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bio.terra.tanagra.testing.BaseSpringUnitTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("plugin")
public class PluginTest extends BaseSpringUnitTest {
  private static final String TEST_UNDERLAY = "cms_synpuf";

  @Autowired private PluginService pluginService;

  @Test
  void pluginLoading() {
    TestPlugin testPlugin = pluginService.getPlugin(TEST_UNDERLAY, TestPlugin.class);
    assertTrue(testPlugin instanceof TestPluginExternalImplementation);
  }

  @Test
  void pluginParameters() {
    TestPlugin test = pluginService.getPlugin(TEST_UNDERLAY, TestPlugin.class);
    assertEquals("configured value", test.getParameter("test"));
  }
}

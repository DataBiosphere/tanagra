package bio.terra.tanagra.plugin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bio.terra.tanagra.testing.BaseSpringUnitTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("plugin")
public class PluginTest extends BaseSpringUnitTest {
  @Autowired private PluginService pluginService;

  private String getUnderlayName() {
    return "pluginTest";
  }

  @Test
  void pluginLoading() {
    TestPlugin testPlugin = pluginService.getPlugin(getUnderlayName(), TestPlugin.class);
    assertTrue(testPlugin instanceof TestPluginExternalImplementation);
  }

  @Test
  void pluginParameters() {
    TestPlugin test = pluginService.getPlugin(getUnderlayName(), TestPlugin.class);
    assertEquals("configured value", test.getParameter("test"));
  }
}

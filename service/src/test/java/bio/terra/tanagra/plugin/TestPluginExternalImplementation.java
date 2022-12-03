package bio.terra.tanagra.plugin;

import java.util.Map;

public class TestPluginExternalImplementation implements TestPlugin {
  private Map<String, String> parameters;

  @Override
  public void init(PluginConfig config) {
    this.parameters = config.getParameters();
  }

  @Override
  public String getDescription() {
    return "External test implementation";
  }

  @Override
  public String getParameter(String name) {
    return parameters.get(name);
  }
}

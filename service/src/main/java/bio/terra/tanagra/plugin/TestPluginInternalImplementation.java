package bio.terra.tanagra.plugin;

public class TestPluginInternalImplementation implements TestPlugin {

  @Override
  public void init(PluginConfig config) {
    // do nothing
  }

  @Override
  public String getDescription() {
    return "Internal default implementation";
  }

  @Override
  public String getParameter(String name) {
    return null;
  }
}

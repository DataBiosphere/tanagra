package bio.terra.tanagra.underlay.uiplugin;

public final class SelectionData {
  private final String plugin;
  private final String pluginData;

  public SelectionData(String plugin, String pluginData) {
    this.plugin = plugin;
    this.pluginData = pluginData;
  }

  public String getPlugin() {
    return plugin;
  }

  public String getPluginData() {
    return pluginData;
  }
}

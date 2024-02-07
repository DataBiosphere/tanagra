package bio.terra.tanagra.filterbuilder;

public final class SelectionData {
  private final String pluginName;
  private final String serialized;

  public SelectionData(String pluginName, String serialized) {
    this.pluginName = pluginName;
    this.serialized = serialized;
  }

  public String getPluginName() {
    return pluginName;
  }

  public String getSerialized() {
    return serialized;
  }
}

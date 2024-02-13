package bio.terra.tanagra.underlay.uiplugin;

public final class SelectionData {
  private final String plugin;
  private final String serialized;

  public SelectionData(String plugin, String serialized) {
    this.plugin = plugin;
    this.serialized = serialized;
  }

  public String getPlugin() {
    return plugin;
  }

  public String getSerialized() {
    return serialized;
  }
}

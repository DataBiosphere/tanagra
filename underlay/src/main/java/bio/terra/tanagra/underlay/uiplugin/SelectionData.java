package bio.terra.tanagra.underlay.uiplugin;

public final class SelectionData {
  private final String selectorOrModifierName;
  private final String pluginData;

  public SelectionData(String selectorOrModifierName, String pluginData) {
    this.selectorOrModifierName = selectorOrModifierName;
    this.pluginData = pluginData;
  }

  public String getSelectorOrModifierName() {
    return selectorOrModifierName;
  }

  public String getPluginData() {
    return pluginData;
  }
}

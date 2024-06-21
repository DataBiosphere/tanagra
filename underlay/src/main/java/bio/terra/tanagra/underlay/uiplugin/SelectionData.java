package bio.terra.tanagra.underlay.uiplugin;

import jakarta.annotation.Nullable;

public final class SelectionData {
  private final @Nullable String modifierName;
  private final String pluginData;

  public SelectionData(@Nullable String modifierName, String pluginData) {
    this.modifierName = modifierName;
    this.pluginData = pluginData;
  }

  public @Nullable String getModifierName() {
    return modifierName;
  }

  public String getPluginData() {
    return pluginData;
  }
}

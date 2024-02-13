package bio.terra.tanagra.underlay.uiplugin;

import java.util.List;

public class CriteriaSelector {
  private final String name;
  private final boolean isEnabledForCohorts;
  private final boolean isEnabledForDataFeatureSets;
  private final String filterBuilder;
  private final String plugin;
  private final String pluginConfig;
  private final List<Modifier> modifiers;

  public CriteriaSelector(
      String name,
      boolean isEnabledForCohorts,
      boolean isEnabledForDataFeatureSets,
      String filterBuilder,
      String plugin,
      String pluginConfig,
      List<Modifier> modifiers) {
    this.name = name;
    this.isEnabledForCohorts = isEnabledForCohorts;
    this.isEnabledForDataFeatureSets = isEnabledForDataFeatureSets;
    this.filterBuilder = filterBuilder;
    this.plugin = plugin;
    this.pluginConfig = pluginConfig;
    this.modifiers = modifiers;
  }

  public String getName() {
    return name;
  }

  public boolean isEnabledForCohorts() {
    return isEnabledForCohorts;
  }

  public boolean isEnabledForDataFeatureSets() {
    return isEnabledForDataFeatureSets;
  }

  public String getFilterBuilder() {
    // TODO: Change this getter to return the FilterBuilder class.
    return filterBuilder;
  }

  public String getPlugin() {
    return plugin;
  }

  public String getPluginConfig() {
    return pluginConfig;
  }

  public List<Modifier> getModifiers() {
    return modifiers;
  }

  public static class Modifier {
    private final String name;
    private final String plugin;
    private final String pluginConfig;

    public Modifier(String name, String plugin, String pluginConfig) {
      this.name = name;
      this.plugin = plugin;
      this.pluginConfig = pluginConfig;
    }

    public String getName() {
      return name;
    }

    public String getPlugin() {
      return plugin;
    }

    public String getPluginConfig() {
      return pluginConfig;
    }
  }
}

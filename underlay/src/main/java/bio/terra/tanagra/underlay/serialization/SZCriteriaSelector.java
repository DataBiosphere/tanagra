package bio.terra.tanagra.underlay.serialization;

import java.util.List;

public class SZCriteriaSelector {
  public String name;

  public boolean isEnabledForCohorts;
  public boolean isEnabledForDataFeatureSets;
  public Display display;
  public String filterBuilder;
  public String plugin;
  public String pluginConfig;
  public String pluginConfigFile;
  public List<Modifier> modifiers;

  public static class Display {
    public String displayName;
    public String category;
    public List<String> tags;
  }

  public static class Modifier {
    public String name;
    public String displayName;
    public String plugin;
    public String pluginConfig;
    public String pluginConfigFile;
  }
}

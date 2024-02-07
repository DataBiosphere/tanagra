package bio.terra.tanagra.underlay.serialization;

import java.util.List;

public class SZCriteriaSelector {
  public String name;
  public String filterBuilderClass;
  public String plugin;
  public String pluginConfigFile;
  public boolean isEnabledForCohorts;
  public boolean isEnabledForDataFeatureSets;
  public Display display;

  public static class Display {
    public String title;
    public String category;
    public List<String> tags;
  }
}

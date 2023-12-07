package bio.terra.tanagra.service.filterbuilder;

public class FilterBuilderInput {
  private final String underlay;
  private final String entity;
  private final int pluginVersion;

  public FilterBuilderInput(String underlay, String entity, int pluginVersion) {
    this.underlay = underlay;
    this.entity = entity;
    this.pluginVersion = pluginVersion;
  }

  public String getUnderlay() {
    return underlay;
  }

  public String getEntity() {
    return entity;
  }

  public int getPluginVersion() {
    return pluginVersion;
  }
}

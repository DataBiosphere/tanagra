package bio.terra.tanagra.service.artifact.model;

import java.util.*;
import org.apache.commons.lang3.RandomStringUtils;

public class Criteria {
  private final String id;
  private final String displayName;
  private final String pluginName;
  private final String selectionData;
  private final String uiConfig;
  private final Map<String, String> tags;

  private Criteria(
      String id,
      String displayName,
      String pluginName,
      String selectionData,
      String uiConfig,
      Map<String, String> tags) {
    this.id = id;
    this.displayName = displayName;
    this.pluginName = pluginName;
    this.selectionData = selectionData;
    this.uiConfig = uiConfig;
    this.tags = tags;
  }

  public static Builder builder() {
    return new Builder();
  }

  public String getId() {
    return id;
  }

  public String getDisplayName() {
    return displayName;
  }

  public String getPluginName() {
    return pluginName;
  }

  public String getSelectionData() {
    return selectionData;
  }

  public String getUiConfig() {
    return uiConfig;
  }

  public Map<String, String> getTags() {
    return tags;
  }

  public static class Builder {
    private String id;
    private String displayName;
    private String pluginName;
    private String selectionData;
    private String uiConfig;
    private Map<String, String> tags = new HashMap<>();

    public Builder id(String id) {
      this.id = id;
      return this;
    }

    public Builder displayName(String displayName) {
      this.displayName = displayName;
      return this;
    }

    public Builder pluginName(String pluginName) {
      this.pluginName = pluginName;
      return this;
    }

    public Builder selectionData(String selectionData) {
      this.selectionData = selectionData;
      return this;
    }

    public Builder uiConfig(String uiConfig) {
      this.uiConfig = uiConfig;
      return this;
    }

    public Builder tags(Map<String, String> tags) {
      this.tags = tags;
      return this;
    }

    public Criteria build() {
      if (id == null) {
        id = RandomStringUtils.randomAlphanumeric(10);
      }
      return new Criteria(id, displayName, pluginName, selectionData, uiConfig, tags);
    }

    public String getId() {
      return id;
    }

    public void addTag(String key, String value) {
      if (tags == null) {
        tags = new HashMap<>();
      }
      tags.put(key, value);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Criteria criteria = (Criteria) o;
    return id.equals(criteria.id)
        && displayName.equals(criteria.displayName)
        && pluginName.equals(criteria.pluginName)
        && selectionData.equals(criteria.selectionData)
        && uiConfig.equals(criteria.uiConfig)
        && tags.equals(criteria.tags);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, displayName, pluginName, selectionData, uiConfig, tags);
  }
}

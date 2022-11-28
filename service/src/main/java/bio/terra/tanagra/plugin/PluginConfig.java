package bio.terra.tanagra.plugin;

import java.util.Map;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

public class PluginConfig {
  private String type;
  @NestedConfigurationProperty private Map<String, String> parameters;

  public void setType(String type) {
    this.type = type;
  }

  public String getType() {
    return this.type;
  }

  public void setParameters(Map<String, String> parameters) {
    this.parameters = parameters;
  }

  public Map<String, String> getParameters() {
    return this.parameters;
  }

  public String getValue(String key) {
    return this.parameters.get(key);
  }
}

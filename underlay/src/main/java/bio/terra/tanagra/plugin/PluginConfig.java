package bio.terra.tanagra.plugin;

import bio.terra.tanagra.exception.InvalidConfigException;
import bio.terra.tanagra.serialization.UFPluginConfig;
import com.google.common.base.Strings;
import java.util.Map;

public class PluginConfig {
  private final String type;
  private final Map<String, String> parameters;

  public PluginConfig(String type, Map<String, String> parameters) {
    this.type = type;
    this.parameters = parameters;
  }

  public String getType() {
    return this.type;
  }

  public Map<String, String> getParameters() {
    return this.parameters;
  }

  public String getValue(String key) {
    return this.parameters.get(key);
  }

  public static PluginConfig fromSerialized(UFPluginConfig serialized) {
    if (Strings.isNullOrEmpty(serialized.getType())) {
      throw new InvalidConfigException("Plugin type is undefined");
    }

    return new PluginConfig(serialized.getType(), serialized.getParameters());
  }
}

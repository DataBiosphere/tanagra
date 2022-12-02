package bio.terra.tanagra.plugin;

import bio.terra.tanagra.exception.InvalidConfigException;
import bio.terra.tanagra.serialization.UFPluginConfig;
import com.google.common.base.Strings;
import java.util.Collections;
import java.util.Map;

public class PluginConfig {
  private final String implementationClassName;
  private final Map<String, String> parameters;

  public PluginConfig(String type, Map<String, String> parameters) {
    this.implementationClassName = type;
    this.parameters = parameters;
  }

  public String getImplementationClassName() {
    return this.implementationClassName;
  }

  public Map<String, String> getParameters() {
    return Collections.unmodifiableMap(this.parameters);
  }

  public String getParameterValue(String key) {
    return this.parameters.get(key);
  }

  public static PluginConfig fromSerialized(UFPluginConfig serialized) {
    if (Strings.isNullOrEmpty(serialized.getImplementationClassName())) {
      throw new InvalidConfigException("Plugin type is undefined");
    }

    return new PluginConfig(serialized.getImplementationClassName(), serialized.getParameters());
  }
}

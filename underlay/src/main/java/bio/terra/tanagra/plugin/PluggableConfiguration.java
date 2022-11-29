package bio.terra.tanagra.plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import bio.terra.tanagra.exception.InvalidConfigException;
import bio.terra.tanagra.serialization.UFPluggableConfiguration;
import bio.terra.tanagra.serialization.UFPluginConfig;
import com.google.common.base.Strings;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

public class PluggableConfiguration {
  private final Map<String, PluginConfig> plugins;

  private PluggableConfiguration(Map<String, PluginConfig> plugins) {
    this.plugins = plugins;
  }

  public Map<String, PluginConfig> getPlugins() {
    return this.plugins;
  }

  public boolean isConfigured() {
    return this.plugins != null && !plugins.isEmpty();
  }

  public static PluggableConfiguration fromSerialized(UFPluggableConfiguration serialized) {
    Map<String, PluginConfig> plugins = serialized.getPlugins().entrySet().stream()
        .collect(Collectors.toMap(
            Map.Entry::getKey,
            kvp -> new PluginConfig(kvp.getValue().getType(), kvp.getValue().getParameters())
        ));

    return new PluggableConfiguration(plugins);
  }
}

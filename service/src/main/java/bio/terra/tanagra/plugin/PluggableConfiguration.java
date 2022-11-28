package bio.terra.tanagra.plugin;

import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties
@ConfigurationProperties(prefix = "tanagra.pluggable")
public class PluggableConfiguration {
  private Map<String, PluginConfig> plugins;

  public void setPlugins(Map<String, PluginConfig> plugins) {
    this.plugins = plugins;
  }

  public Map<String, PluginConfig> getPlugins() {
    return this.plugins;
  }

  public boolean isConfigured() {
    return this.plugins != null && !plugins.isEmpty();
  }
}

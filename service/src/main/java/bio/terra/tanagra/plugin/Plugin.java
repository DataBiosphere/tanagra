package bio.terra.tanagra.plugin;

/**
 * Plugin interface which all plugins inherit from
 *
 * <p>Plugin implementations must implement a default constructor in order to loadable by the
 * PluginService
 */
public interface Plugin {
  void init(PluginConfig config);
}

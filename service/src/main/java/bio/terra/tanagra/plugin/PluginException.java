package bio.terra.tanagra.plugin;

public class PluginException extends RuntimeException {
  public PluginException(String message, Throwable cause) {
    super(message, cause);
  }

  public PluginException(String message) {
    super(message);
  }
}

package bio.terra.tanagra.plugin;

public class PluginException extends RuntimeException {
  public PluginException(Exception e) {
    super(e);
  }

  public PluginException(String message) {
    super(message);
  }
}

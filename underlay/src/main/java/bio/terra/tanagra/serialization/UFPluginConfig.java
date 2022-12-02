package bio.terra.tanagra.serialization;

import bio.terra.tanagra.plugin.PluginConfig;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.util.Map;

/**
 * Plugin configuration
 *
 * <p>This is a POJO class intended for serialization. This JSON format is user-facing.
 */
@JsonDeserialize(builder = UFPluginConfig.Builder.class)
public class UFPluginConfig {
  private final String implementationClassName;
  private final Map<String, String> parameters;

  public UFPluginConfig(PluginConfig config) {
    this.implementationClassName = config.getImplementationClassName();
    this.parameters = config.getParameters();
  }

  private UFPluginConfig(UFPluginConfig.Builder builder) {
    this.implementationClassName = builder.implementationClassName;
    this.parameters = builder.parameters;
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {
    private String implementationClassName;
    private Map<String, String> parameters;

    public UFPluginConfig.Builder type(String type) {
      this.implementationClassName = type;
      return this;
    }

    public UFPluginConfig.Builder parameters(Map<String, String> parameters) {
      this.parameters = parameters;
      return this;
    }

    /** Call the private constructor. */
    public UFPluginConfig build() {
      return new UFPluginConfig(this);
    }
  }

  public String getImplementationClassName() {
    return implementationClassName;
  }

  public Map<String, String> getParameters() {
    return parameters;
  }
}

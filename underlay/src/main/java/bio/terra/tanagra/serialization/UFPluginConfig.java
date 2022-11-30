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
@JsonDeserialize(builder = UFUnderlay.Builder.class)
public class UFPluginConfig {
  private final String type;
  private final Map<String, String> parameters;

  public UFPluginConfig(PluginConfig config) {
    this.type = config.getType();
    this.parameters = config.getParameters();
  }

  private UFPluginConfig(UFPluginConfig.Builder builder) {
    this.type = builder.type;
    this.parameters = builder.parameters;
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {
    private String type;
    private Map<String, String> parameters;

    public UFPluginConfig.Builder type(String type) {
      this.type = type;
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

  public String getType() {
    return type;
  }

  public Map<String, String> getParameters() {
    return parameters;
  }
}

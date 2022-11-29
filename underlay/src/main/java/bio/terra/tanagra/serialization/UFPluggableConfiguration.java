package bio.terra.tanagra.serialization;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import bio.terra.tanagra.plugin.PluggableConfiguration;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

/**
 * Plugin configuration
 *
 * <p>This is a POJO class intended for serialization. This JSON format is user-facing.
 */
@JsonDeserialize(builder = UFPluggableConfiguration.Builder.class)
public class UFPluggableConfiguration {
    private final Map<String, UFPluginConfig> plugins;

    public UFPluggableConfiguration(PluggableConfiguration configuration) {
        this.plugins = configuration.getPlugins().entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                kvp -> new UFPluginConfig(kvp.getValue())
            ));
    }

    private UFPluggableConfiguration(UFPluggableConfiguration.Builder builder) {
        this.plugins = builder.plugins;
    }

    @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
    public static class Builder {
        private Map<String, UFPluginConfig> plugins;

        public UFPluggableConfiguration.Builder plugins(Map<String, UFPluginConfig> plugins) {
            this.plugins = plugins;
            return this;
        }

        /** Call the private constructor. */
        public UFPluggableConfiguration build() {
            return new UFPluggableConfiguration(this);
        }
    }

    public Map<String, UFPluginConfig> getPlugins() {
        return plugins;
    }
}

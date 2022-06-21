package bio.terra.tanagra.app.configuration;

import bio.terra.tanagra.proto.underlay.Underlay;
import bio.terra.tanagra.underlay.UnderlayYamlParser;
import com.google.common.io.Resources;
import com.google.protobuf.TextFormat;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties
@ConfigurationProperties(prefix = "tanagra.underlay")
public class UnderlayConfiguration {
  /** Paths to {@link Underlay} prototext files to load. */
  private List<String> underlayPrototextFiles = new ArrayList<>();

  /**
   * Paths to {@link Underlay} yaml files to load. The yaml is converted to JSON and decoded with
   * the standard protobuf json encoder.
   */
  private List<String> underlayYamlFiles = new ArrayList<>();

  public List<String> getUnderlayPrototextFiles() {
    return underlayPrototextFiles;
  }

  public void setUnderlayPrototextFiles(List<String> underlayPrototextFiles) {
    this.underlayPrototextFiles = underlayPrototextFiles;
  }

  public List<String> getUnderlayYamlFiles() {
    return underlayYamlFiles;
  }

  public void setUnderlayYamlFiles(List<String> underlayYamlFiles) {
    this.underlayYamlFiles = underlayYamlFiles;
  }

  public List<Underlay> getUnderlays() throws IOException {
    List<Underlay> result = new ArrayList<>();
    for (String file : getUnderlayYamlFiles()) {
      result.add(UnderlayYamlParser.parse(readFile(file)));
    }

    for (String file : getUnderlayPrototextFiles()) {
      Underlay.Builder builder = Underlay.newBuilder();
      TextFormat.merge(readFile(file), builder);
      result.add(builder.build());
    }
    return result;
  }

  private static String readFile(String filePath) throws IOException {
    return Resources.toString(Resources.getResource(filePath), StandardCharsets.UTF_8);
  }
}

package bio.terra.tanagra.app.configuration;

import bio.terra.tanagra.proto.underlay.Underlay;
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
  private List<String> underlayFiles = new ArrayList<>();

  public List<String> getUnderlayFiles() {
    return underlayFiles;
  }

  public void setUnderlayFiles(List<String> underlayFiles) {
    this.underlayFiles = underlayFiles;
  }

  public List<Underlay> getUnderlays() throws IOException {
    List<Underlay> result = new ArrayList<>();
    for (String databankFile : getUnderlayFiles()) {
      Underlay.Builder builder = Underlay.newBuilder();
      TextFormat.merge(
          Resources.toString(Resources.getResource(databankFile), StandardCharsets.UTF_8), builder);
      result.add(builder.build());
    }
    return result;
  }
}

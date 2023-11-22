package bio.terra.tanagra.app.configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties
@ConfigurationProperties(prefix = "tanagra.underlay")
public class UnderlayConfiguration {
  private static final Logger LOGGER = LoggerFactory.getLogger(UnderlayConfiguration.class);

  private List<String> files = new ArrayList<>();

  public List<String> getFiles() {
    return files;
  }

  public void setFiles(List<String> files) {
    this.files = files;
  }

  public void log() {
    LOGGER.info("Underlay: files: {}", getFiles().stream().collect(Collectors.joining(",")));
  }
}

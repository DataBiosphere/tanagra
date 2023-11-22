package bio.terra.tanagra.app.configuration;

import bio.terra.tanagra.annotation.AnnotatedClass;
import bio.terra.tanagra.annotation.AnnotatedField;
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
@AnnotatedClass(name = "Underlays", markdown = "Configure the underlays served by the deployment.")
public class UnderlayConfiguration {
  private static final Logger LOGGER = LoggerFactory.getLogger(UnderlayConfiguration.class);

  @AnnotatedField(
      name = "tanagra.underlay.files",
      markdown =
          "Comma-separated list of service configurations. "
              + "Use the name of the service configuration file only, no extension or path.",
      exampleValue = "cmssynpuf_broad,aouSR2019q4r4_broad",
      environmentVariable = "TANAGRA_UNDERLAY_FILES")
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

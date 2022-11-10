package bio.terra.tanagra.app.configuration;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties
@ConfigurationProperties(prefix = "tanagra.underlay")
public class UnderlayConfiguration {
  // The list of underlay config files in the resources/config directory. (e.g.
  // 'broad/aou_synthetic/expanded/aou_synthetic.json')
  private List<String> underlayFiles = new ArrayList<>();

  public List<String> getUnderlayFiles() {
    return underlayFiles;
  }

  public void setUnderlayFiles(List<String> underlayFiles) {
    this.underlayFiles = underlayFiles;
  }
}

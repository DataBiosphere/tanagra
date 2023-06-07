package bio.terra.tanagra.app.configuration;

import bio.terra.tanagra.service.accesscontrol.AccessControl;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties
@ConfigurationProperties(prefix = "tanagra.access-control")
public class AccessControlConfiguration {
  private static final Logger LOGGER = LoggerFactory.getLogger(AccessControlConfiguration.class);

  private AccessControl.Model model;
  private List<String> params;
  private String basePath;
  private String oauthClientId;

  /** Default this property to the OPEN_ACCESS model. */
  public AccessControl.Model getModel() {
    return model != null ? model : AccessControl.Model.OPEN_ACCESS;
  }

  public List<String> getParams() {
    return Collections.unmodifiableList(params);
  }

  public String getBasePath() {
    return basePath;
  }

  public String getOauthClientId() {
    return oauthClientId;
  }

  public void setModel(AccessControl.Model model) {
    this.model = model;
  }

  public void setParams(List<String> params) {
    this.params = params;
  }

  public void setBasePath(String basePath) {
    this.basePath = basePath;
  }

  public void setOauthClientId(String oauthClientId) {
    this.oauthClientId = oauthClientId;
  }

  /** Write the access control flags into the log. Add an entry here for each new flag. */
  public void logConfig() {
    LOGGER.info("Access control: model: {}", getModel());
    LOGGER.info(
        "Access control: params: {}", getParams().stream().collect(Collectors.joining(",")));
    LOGGER.info("Access control: base-path: {}", getBasePath());
    LOGGER.info("Access control: oauth-client-id: {}", getOauthClientId());
  }
}

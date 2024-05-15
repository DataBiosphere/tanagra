package bio.terra.tanagra.app.configuration;

import bio.terra.tanagra.annotation.AnnotatedClass;
import bio.terra.tanagra.annotation.AnnotatedField;
import bio.terra.tanagra.service.accesscontrol.model.CoreModel;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties
@ConfigurationProperties(prefix = "tanagra.access-control")
@AnnotatedClass(
    name = "Access Control",
    markdown = "Configure the access control or authorization model.")
public class AccessControlConfiguration {
  private static final Logger LOGGER = LoggerFactory.getLogger(AccessControlConfiguration.class);

  @AnnotatedField(
      name = "tanagra.access-control.model",
      markdown =
          "Pointer to the access control model Java class. Currently this must be one of the enum values in the"
              + "`bio.terra.tanagra.service.accesscontrol.model.CoreModel` Java class, or the full name of a class "
              + "that implements the `bio.terra.tanagra.service.accesscontrol.model.FineGrainedAccessControl` interface "
              + "and is on the classpath.",
      optional = true,
      defaultValue = "OPEN_ACCESS",
      environmentVariable = "TANAGRA_ACCESS_CONTROL_MODEL")
  private String model;

  @AnnotatedField(
      name = "tanagra.access-control.basePath",
      markdown = "URL of another service the access control model will call. e.g. Workbench URL.",
      environmentVariable = "TANAGRA_ACCESS_CONTROL_BASE_PATH",
      optional = true,
      exampleValue = "https://www.workbench.com")
  private String basePath;

  @AnnotatedField(
      name = "tanagra.access-control.oauthClientId",
      markdown =
          "OAuth client id of another service the access control model will call. e.g. Workbench client id.",
      environmentVariable = "TANAGRA_ACCESS_CONTROL_OAUTH_CLIENT_ID",
      optional = true,
      exampleValue = "abcdefghijklmnopqrstuvwxyz.apps.googleusercontent.com")
  private String oauthClientId;

  @AnnotatedField(
      name = "tanagra.access-control.params",
      markdown =
          "Map of parameters to pass to the access control model. Pass the map as a list e.g. key1,value1,key2,value2,... "
              + "This is useful when you want to parameterize a model beyond just the base path and OAuth client id. "
              + "e.g. Name of a Google Group you want to use to restrict access.",
      environmentVariable = "TANAGRA_ACCESS_CONTROL_PARAMS",
      optional = true,
      exampleValue = "googleGroupName,admin-users@googlegroups.com")
  private List<String> params;

  public String getModel() {
    return model != null ? model : CoreModel.OPEN_ACCESS.name();
  }

  public List<String> getParams() {
    return params == null ? Collections.emptyList() : Collections.unmodifiableList(params);
  }

  public String getBasePath() {
    return basePath;
  }

  public String getOauthClientId() {
    return oauthClientId;
  }

  public void setModel(String model) {
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

  public void log() {
    LOGGER.info("Access control: model: {}", getModel());
    LOGGER.info("Access control: params: {}", String.join(",", getParams()));
    LOGGER.info("Access control: base-path: {}", getBasePath());
    LOGGER.info("Access control: oauth-client-id: {}", getOauthClientId());

    if (model == null) {
      LOGGER.warn("Access control: No model specified, using default");
    }
    // Each access control plugin will validate the configuration here in its constructor.
  }
}

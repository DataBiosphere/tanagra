package bio.terra.tanagra.app.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties
@ConfigurationProperties(prefix = "tanagra.auth")
public class AuthConfiguration {
  private static final Logger LOGGER = LoggerFactory.getLogger(AuthConfiguration.class);

  private boolean iapGkeJwt;
  private boolean iapAppEngineJwt;
  private boolean bearerToken;

  private String gcpProjectNumber;
  private String gcpProjectId;
  private String gkeBackendServiceId;

  public boolean isIapGkeJwt() {
    return iapGkeJwt;
  }

  public boolean isIapAppEngineJwt() {
    return iapAppEngineJwt;
  }

  public boolean isBearerToken() {
    return bearerToken;
  }

  public long getGcpProjectNumber() {
    return Long.valueOf(gcpProjectNumber);
  }

  public String getGcpProjectId() {
    return gcpProjectId;
  }

  public long getGkeBackendServiceId() {
    return Long.valueOf(gkeBackendServiceId);
  }

  public void setIapGkeJwt(boolean iapGkeJwt) {
    this.iapGkeJwt = iapGkeJwt;
  }

  public void setIapAppEngineJwt(boolean iapAppEngineJwt) {
    this.iapAppEngineJwt = iapAppEngineJwt;
  }

  public void setBearerToken(boolean bearerToken) {
    this.bearerToken = bearerToken;
  }

  public void setGcpProjectNumber(String gcpProjectNumber) {
    this.gcpProjectNumber = gcpProjectNumber;
  }

  public void setGcpProjectId(String gcpProjectId) {
    this.gcpProjectId = gcpProjectId;
  }

  public void setGkeBackendServiceId(String gkeBackendServiceId) {
    this.gkeBackendServiceId = gkeBackendServiceId;
  }

  /** Write the auth flags into the log. Add an entry here for each new auth flag. */
  public void logConfig() {
    LOGGER.info("Auth config: iap-gke-jwt: {}", isIapGkeJwt());
    LOGGER.info("Auth config: iap-appengine-jwt: {}", isIapAppEngineJwt());
    LOGGER.info("Auth config: bearer-token: {}", isBearerToken());
    LOGGER.info("Auth config: gcp-project-number: {}", getGcpProjectNumber());
    LOGGER.info("Auth config: gcp-project-id: {}", getGcpProjectId());
    LOGGER.info("Auth config: gke-backend-service-id: {}", getGkeBackendServiceId());
  }
}

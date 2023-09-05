package bio.terra.tanagra.app.configuration;

import java.util.Collections;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/** Read from the version.properties file auto-generated at build time. */
@Configuration
@PropertySource("classpath:/generated/version.properties")
@ConfigurationProperties(prefix = "version")
public class VersionConfiguration implements InitializingBean {
  private static final String GITHUB_COMMIT_URL =
      "https://github.com/DataBiosphere/tanagra/commit/";
  private String gitHash;
  private String gitTag;
  private String build;

  private final ConfigurableEnvironment configurableEnvironment;

  @Autowired
  public VersionConfiguration(ConfigurableEnvironment configurableEnvironment) {
    this.configurableEnvironment = configurableEnvironment;
  }

  public String getGitHash() {
    return gitHash;
  }

  public void setGitHash(String gitHash) {
    this.gitHash = gitHash;
  }

  public String getGitTag() {
    return gitTag;
  }

  public void setGitTag(String gitTag) {
    this.gitTag = gitTag;
  }

  public String getBuild() {
    return build;
  }

  public void setBuild(String build) {
    this.build = build;
  }

  public String getGithubUrl() {
    return GITHUB_COMMIT_URL + gitHash;
  }

  public static String getGithubUrl(String gitHash) {
    return GITHUB_COMMIT_URL + gitHash;
  }

  /**
   * Copies the version.build property to spring.application.version, for consumption by the common
   * logging module's JSON layout.
   */
  @Override
  public void afterPropertiesSet() {
    configurableEnvironment
        .getPropertySources()
        .addFirst(
            new MapPropertySource(
                "version", Collections.singletonMap("spring.application.version", getBuild())));
  }
}

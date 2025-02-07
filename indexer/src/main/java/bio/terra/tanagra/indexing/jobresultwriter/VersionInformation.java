package bio.terra.tanagra.indexing.jobresultwriter;

import bio.terra.tanagra.cli.exception.InternalErrorException;
import bio.terra.tanagra.utils.FileUtils;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Properties;

public record VersionInformation(String gitHash, String gitTag, String build) {
  private static final String GITHUB_COMMIT_URL =
      "https://github.com/DataBiosphere/tanagra/commit/";

  public static VersionInformation fromResourceFile() {
    final String resourcePath = "generated/version.properties";
    Properties versionProperties = new Properties();
    try {
      versionProperties.load(FileUtils.getResourceFileStream(Path.of(resourcePath)));
    } catch (IOException ioEx) {
      throw new InternalErrorException("Error reading version information", ioEx);
    }
    final String gitHashProp = "version.gitHash";
    final String gitTagProp = "version.gitTag";
    final String buildProp = "version.build";
    return new VersionInformation(
        versionProperties.getProperty(gitHashProp),
        versionProperties.getProperty(gitTagProp),
        versionProperties.getProperty(buildProp));
  }

  public String getGithubUrl() {
    return GITHUB_COMMIT_URL + gitHash;
  }
}

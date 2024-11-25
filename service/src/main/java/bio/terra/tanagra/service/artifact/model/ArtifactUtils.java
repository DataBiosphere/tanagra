package bio.terra.tanagra.service.artifact.model;

import org.apache.commons.lang3.RandomStringUtils;

public class ArtifactUtils {
  public static String newId() {
    return RandomStringUtils.randomAlphanumeric(10);
  }
}

package bio.terra.tanagra.service;

import org.apache.commons.lang3.RandomStringUtils;

public class ServiceUtils {
  private ServiceUtils() {}

  public static String newArtifactId() {
    return RandomStringUtils.randomAlphanumeric(10);
  }
}

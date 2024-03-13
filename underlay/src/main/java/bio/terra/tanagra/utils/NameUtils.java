package bio.terra.tanagra.utils;

public final class NameUtils {
  private static final String SIMPLIFY_TO_NAME_REGEX = "[^a-zA-Z0-9_]";

  private NameUtils() {}

  public static String simplifyStringForName(String str) {
    return str.replaceAll(SIMPLIFY_TO_NAME_REGEX, "");
  }
}

package bio.terra.tanagra.model;

import com.google.common.base.Preconditions;
import java.util.regex.Pattern;

/**
 * Utilities for checking Tanagra names.
 *
 * <p>Some names traverse Tanagra from HTTP requests to SQL generation. For these names, it's
 * important that they are relatively friendly and simple.
 */
final class NameUtils {
  private NameUtils() {}

  // Start with simple lower case letters, numbers, and underscores of less than 32 characters.
  // We could add more valid characters, or allow different characters for different places, but
  // this is a simple starting point.
  private static final String NAME_REGEX = "^[a-z][a-zA-Z0-9_]{0,31}$";
  private static final Pattern NAME_VALIDATOR = Pattern.compile(NAME_REGEX);

  /**
   * Check that the {@code name} matches the {@link #NAME_REGEX}, or else throws an
   * IllegalArgumentException.
   */
  public static void checkName(String name, String fieldName) {
    Preconditions.checkArgument(
        NAME_VALIDATOR.matcher(name).matches(),
        "%s must match regex '%s' but name was '%s'",
        fieldName,
        NAME_REGEX,
        name);
  }
}

package bio.terra.tanagra.service.query.api;

import java.util.Objects;
import java.util.stream.Stream;

/** Utilities for converting from API model classes to Tanagra classes. */
public final class ConversionUtils {
  private ConversionUtils() {}

  /** Returns if exactly one of the objects is non-null. */
  public static boolean exactlyOneNonNull(Object... objects) {
    return Stream.of(objects).filter(Objects::nonNull).count() == 1;
  }
}

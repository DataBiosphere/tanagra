package bio.terra.tanagra.service.query.api;

import bio.terra.common.exception.BadRequestException;
import bio.terra.tanagra.service.search.Variable;
import java.util.Objects;
import java.util.stream.Stream;

/** Utilities for converting from API model classes to Tanagra classes. */
public final class ConversionUtils {
  private ConversionUtils() {}

  /** Returns if exactly one of the objects is non-null. */
  public static boolean exactlyOneNonNull(Object... objects) {
    return Stream.of(objects).filter(Objects::nonNull).count() == 1;
  }

  public static Variable createVariable(String name) {
    if (name.isEmpty()) {
      throw new BadRequestException("A variable must have a non-empty name.");
    }
    return Variable.create(name);
  }
}

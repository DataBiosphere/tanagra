package bio.terra.tanagra.underlay2;

import javax.annotation.Nullable;

public class NameHelper {
  private static final String GENERATED_FIELD_PREFIX = "T_";
  private final @Nullable String generatedTablePrefix;

  public NameHelper(@Nullable String generatedTablePrefix) {
    this.generatedTablePrefix = generatedTablePrefix;
  }

  public String getReservedTableName(String baseName) {
    // TODO: add "_"
    return (generatedTablePrefix == null ? "" : generatedTablePrefix) + baseName;
  }

  public static String getReservedFieldName(String baseName) {
    return GENERATED_FIELD_PREFIX + baseName;
  }
}

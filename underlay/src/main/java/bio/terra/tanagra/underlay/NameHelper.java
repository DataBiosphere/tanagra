package bio.terra.tanagra.underlay;

import jakarta.annotation.Nullable;

public class NameHelper {
  private static final String GENERATED_FIELD_PREFIX = "T_";
  private final @Nullable String generatedTablePrefix;

  public NameHelper(@Nullable String generatedTablePrefix) {
    this.generatedTablePrefix = generatedTablePrefix;
  }

  public String getReservedTableName(String baseName) {
    return (generatedTablePrefix == null ? "" : generatedTablePrefix + "_") + baseName;
  }

  public static String getReservedFieldName(String baseName) {
    return GENERATED_FIELD_PREFIX + baseName;
  }
}

package bio.terra.tanagra.underlay2.indexschema;

import bio.terra.tanagra.exception.SystemException;
import bio.terra.tanagra.query.TablePointer;
import bio.terra.tanagra.underlay.DataPointer;
import javax.annotation.Nullable;

public final class SchemaUtils {
  private final DataPointer generatedIndexDataPointer;
  private final @Nullable String generatedTablePrefix;
  private static final String GENERATED_FIELD_PREFIX = "T_";

  private static SchemaUtils singleton;

  private SchemaUtils(DataPointer generatedIndexDataPointer, String generatedTablePrefix) {
    this.generatedIndexDataPointer = generatedIndexDataPointer;
    this.generatedTablePrefix = generatedTablePrefix;
  }

  public static void initialize(
      DataPointer generatedIndexDataPointer, String generatedTablePrefix) {
    singleton = new SchemaUtils(generatedIndexDataPointer, generatedTablePrefix);
  }

  public static SchemaUtils getSingleton() {
    if (singleton == null) {
      throw new SystemException("Singleton not initialized");
    }
    return singleton;
  }

  public String getReservedTableName(String baseName) {
    return (generatedTablePrefix == null ? "" : generatedTablePrefix) + baseName;
  }

  public static String getReservedFieldName(String baseName) {
    return GENERATED_FIELD_PREFIX + baseName;
  }

  public TablePointer getIndexTable(String tableName) {
    return TablePointer.fromTableName(tableName, generatedIndexDataPointer);
  }
}

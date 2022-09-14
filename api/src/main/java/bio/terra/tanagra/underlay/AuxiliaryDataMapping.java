package bio.terra.tanagra.underlay;

import bio.terra.tanagra.serialization.UFAuxiliaryDataMapping;
import bio.terra.tanagra.serialization.UFFieldPointer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class AuxiliaryDataMapping {
  private final TablePointer tablePointer;
  private final Map<String, FieldPointer> fieldPointers;

  public AuxiliaryDataMapping(TablePointer tablePointer, Map<String, FieldPointer> fieldPointers) {
    this.tablePointer = tablePointer;
    this.fieldPointers = fieldPointers;
  }

  public static AuxiliaryDataMapping fromSerialized(
      UFAuxiliaryDataMapping serialized, DataPointer dataPointer, AuxiliaryData auxiliaryData) {
    // if the table is defined, then deserialize it
    // otherwise generate a default table pointer: a table with the same name as the entity
    TablePointer tablePointer =
        (serialized == null || serialized.getTablePointer() == null)
            ? TablePointer.fromTableName(auxiliaryData.getName(), dataPointer)
            : TablePointer.fromSerialized(serialized.getTablePointer(), dataPointer);

    Map<String, UFFieldPointer> serializedFieldPointers =
        (serialized == null || serialized.getFieldPointers() == null)
            ? new HashMap<>()
            : serialized.getFieldPointers();
    Map<String, FieldPointer> fieldPointers = new HashMap<>();
    for (String fieldName : auxiliaryData.getFields()) {
      // if the field pointer is defined, then deserialize it
      // otherwise generate a default field pointer: a column in the table of the same name
      FieldPointer fieldPointer =
          serializedFieldPointers.get(fieldName) != null
              ? FieldPointer.fromSerialized(serializedFieldPointers.get(fieldName), tablePointer)
              : new FieldPointer.Builder().tablePointer(tablePointer).columnName(fieldName).build();
      fieldPointers.put(fieldName, fieldPointer);
    }
    serializedFieldPointers.keySet().stream()
        .forEach(
            serializedFieldName -> {
              if (!auxiliaryData.getFields().contains(serializedFieldName)) {
                throw new IllegalArgumentException(
                    "A mapping is defined for a non-existent field: " + serializedFieldName);
              }
            });

    return new AuxiliaryDataMapping(tablePointer, fieldPointers);
  }

  public TablePointer getTablePointer() {
    return tablePointer;
  }

  public Map<String, FieldPointer> getFieldPointers() {
    return Collections.unmodifiableMap(fieldPointers);
  }
}

package bio.terra.tanagra.underlay;

import bio.terra.tanagra.query.FieldVariable;
import bio.terra.tanagra.query.Query;
import bio.terra.tanagra.query.TableVariable;
import bio.terra.tanagra.serialization.UFAttributeMapping;
import bio.terra.tanagra.serialization.UFEntityMapping;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EntityMapping {
  private TablePointer tablePointer;
  private Map<String, AttributeMapping> attributeMappings;
  private TextSearchMapping textSearchMapping;

  private EntityMapping(
      TablePointer tablePointer,
      Map<String, AttributeMapping> attributeMappings,
      TextSearchMapping textSearchMapping) {
    this.tablePointer = tablePointer;
    this.attributeMappings = attributeMappings;
    this.textSearchMapping = textSearchMapping;
  }

  public static EntityMapping fromSerialized(
      UFEntityMapping serialized,
      Map<String, DataPointer> dataPointers,
      Map<String, Attribute> attributes,
      String entityName) {
    if (serialized.dataPointer == null || serialized.dataPointer.isEmpty()) {
      throw new IllegalArgumentException("No Data Pointer defined");
    }
    if (!dataPointers.containsKey(serialized.dataPointer)) {
      throw new IllegalArgumentException("Data Pointer not found: " + serialized.dataPointer);
    }
    DataPointer dataPointer = dataPointers.get(serialized.dataPointer);

    // if the table is defined, then deserialize it
    // otherwise generate a default table pointer: a table with the same name as the entity
    TablePointer tablePointer =
        serialized.tablePointer != null
            ? TablePointer.fromSerialized(serialized.tablePointer, dataPointer)
            : new TablePointer(entityName, dataPointer);

    Map<String, UFAttributeMapping> serializedAttributeMappings =
        serialized.attributeMappings == null ? new HashMap<>() : serialized.attributeMappings;
    Map<String, AttributeMapping> attributeMappings = new HashMap<>();
    for (Attribute attribute : attributes.values()) {
      AttributeMapping attributeMapping =
          AttributeMapping.fromSerialized(
              serializedAttributeMappings.get(attribute.getName()), tablePointer, attribute);
      attributeMappings.put(attribute.getName(), attributeMapping);
    }
    serializedAttributeMappings.keySet().stream()
        .forEach(
            serializedAttributeName -> {
              if (!attributes.containsKey(serializedAttributeName)) {
                throw new IllegalArgumentException(
                    "A mapping is defined for a non-existent attribute: "
                        + serializedAttributeName);
              }
            });

    TextSearchMapping textSearchMapping =
        serialized.textSearchMapping == null
            ? null
            : TextSearchMapping.fromSerialized(
                serialized.textSearchMapping, tablePointer, attributes);

    return new EntityMapping(tablePointer, attributeMappings, textSearchMapping);
  }

  public TablePointer getTablePointer() {
    return tablePointer;
  }

  public String selectAllQuery() {
    List<TableVariable> tables = new ArrayList<>();
    TableVariable primaryTable = TableVariable.forPrimary(tablePointer);
    tables.add(primaryTable);

    List<FieldVariable> select = new ArrayList<>();
    for (Map.Entry<String, AttributeMapping> attributeToMapping : attributeMappings.entrySet()) {
      String attributeName = attributeToMapping.getKey();
      select.addAll(
          attributeToMapping.getValue().buildFieldVariables(primaryTable, tables, attributeName));
    }

    return new Query(select, tables, null, null, null).renderSQL();
  }
}

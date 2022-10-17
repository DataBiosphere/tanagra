package bio.terra.tanagra.underlay;

import bio.terra.tanagra.exception.InvalidConfigException;
import bio.terra.tanagra.exception.SystemException;
import bio.terra.tanagra.query.FieldPointer;
import bio.terra.tanagra.query.FieldVariable;
import bio.terra.tanagra.query.Query;
import bio.terra.tanagra.query.TablePointer;
import bio.terra.tanagra.query.TableVariable;
import bio.terra.tanagra.serialization.UFAttributeMapping;
import bio.terra.tanagra.serialization.UFEntityMapping;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class EntityMapping {
  private final TablePointer tablePointer;
  private TextSearchMapping textSearchMapping;

  private final Map<String, AttributeMapping> attributeMappings;
  private Map<String, HierarchyMapping> hierarchyMappings;
  private final Attribute idAttribute;

  private EntityMapping(
      TablePointer tablePointer,
      Map<String, AttributeMapping> attributeMappings,
      TextSearchMapping textSearchMapping,
      Map<String, HierarchyMapping> hierarchyMappings,
      Attribute idAttribute) {
    this.tablePointer = tablePointer;
    this.attributeMappings = attributeMappings;
    this.textSearchMapping = textSearchMapping;
    this.hierarchyMappings = hierarchyMappings;
    this.idAttribute = idAttribute;
  }

  public static EntityMapping fromSerialized(
      UFEntityMapping serialized,
      Map<String, DataPointer> dataPointers,
      Map<String, Attribute> attributes,
      String entityName,
      String idAttributeName) {
    if (serialized.getDataPointer() == null || serialized.getDataPointer().isEmpty()) {
      throw new InvalidConfigException("No Data Pointer defined");
    }
    if (!dataPointers.containsKey(serialized.getDataPointer())) {
      throw new InvalidConfigException("Data Pointer not found: " + serialized.getDataPointer());
    }
    DataPointer dataPointer = dataPointers.get(serialized.getDataPointer());

    // if the table is defined, then deserialize it
    // otherwise generate a default table pointer: a table with the same name as the entity
    TablePointer tablePointer =
        serialized.getTablePointer() != null
            ? TablePointer.fromSerialized(serialized.getTablePointer(), dataPointer)
            : TablePointer.fromTableName(entityName, dataPointer);

    Map<String, UFAttributeMapping> serializedAttributeMappings =
        serialized.getAttributeMappings() == null
            ? new HashMap<>()
            : serialized.getAttributeMappings();
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
                throw new InvalidConfigException(
                    "A mapping is defined for a non-existent attribute: "
                        + serializedAttributeName);
              }
            });

    TextSearchMapping textSearchMapping =
        serialized.getTextSearchMapping() == null
            ? null
            : TextSearchMapping.fromSerialized(
                serialized.getTextSearchMapping(), tablePointer, attributes, idAttributeName);

    Map<String, HierarchyMapping> hierarchyMappings =
        serialized.getHierarchyMappings() == null
            ? new HashMap<>()
            : serialized.getHierarchyMappings().entrySet().stream()
                .collect(
                    Collectors.toMap(
                        shm -> shm.getKey(),
                        shm ->
                            HierarchyMapping.fromSerialized(
                                shm.getValue(), shm.getKey(), dataPointer)));

    return new EntityMapping(
        tablePointer,
        attributeMappings,
        textSearchMapping,
        hierarchyMappings,
        attributes.get(idAttributeName));
  }

  public Query queryAttributes(List<Attribute> selectedAttributes) {
    return queryAttributesAndFields(selectedAttributes, null);
  }

  public Query queryAttributes(Map<String, Attribute> selectedAttributes) {
    return queryAttributesAndFields(selectedAttributes, null);
  }

  public Query queryFields(List<FieldPointer> selectedFields) {
    return queryAttributesAndFields(null, selectedFields);
  }

  public Query queryFields(Map<String, FieldPointer> selectedFields) {
    return queryAttributesAndFields(null, selectedFields);
  }

  public Query queryAttributesAndFields(
      List<Attribute> selectedAttributes, List<FieldPointer> selectedFields) {
    Map<String, Attribute> nameToAttribute =
        selectedAttributes == null
            ? null
            : selectedAttributes.stream()
                .collect(Collectors.toMap(Attribute::getName, Function.identity()));
    Map<String, FieldPointer> nameToFieldPointer =
        selectedFields == null
            ? null
            : selectedFields.stream()
                .collect(Collectors.toMap(FieldPointer::getColumnName, Function.identity()));
    return queryAttributesAndFields(nameToAttribute, nameToFieldPointer);
  }

  public Query queryAttributesAndFields(
      Map<String, Attribute> selectedAttributes, Map<String, FieldPointer> selectedFields) {
    List<TableVariable> tables = new ArrayList<>();
    TableVariable primaryTable = TableVariable.forPrimary(tablePointer);
    tables.add(primaryTable);

    List<FieldVariable> select = new ArrayList<>();
    if (selectedAttributes != null) {
      selectedAttributes.entrySet().stream()
          .forEach(
              nameToAttr ->
                  select.addAll(
                      attributeMappings
                          .get(nameToAttr.getValue().getName())
                          .buildFieldVariables(primaryTable, tables, nameToAttr.getKey())));
    }
    if (selectedFields != null) {
      selectedFields.entrySet().stream()
          .forEach(
              nameToFP ->
                  select.add(
                      nameToFP.getValue().buildVariable(primaryTable, tables, nameToFP.getKey())));
    }

    return new Query.Builder().select(select).tables(tables).build();
  }

  public Query queryTextSearchStrings() {
    return textSearchMapping.queryTextSearchStrings(this);
  }

  public TablePointer getTextSearchTablePointer() {
    return textSearchMapping.getTablePointer(this);
  }

  public TablePointer getTablePointer() {
    return tablePointer;
  }

  public Map<String, AttributeMapping> getAttributeMappings() {
    return Collections.unmodifiableMap(attributeMappings);
  }

  public AttributeMapping getIdAttributeMapping() {
    return attributeMappings.get(idAttribute.getName());
  }

  public AttributeMapping getAttributeMapping(String attributeName) {
    return attributeMappings.get(attributeName);
  }

  public boolean hasTextSearchMapping() {
    return textSearchMapping != null;
  }

  public TextSearchMapping getTextSearchMapping() {
    return textSearchMapping;
  }

  public void setTextSearchMapping(TextSearchMapping textSearchMapping) {
    this.textSearchMapping = textSearchMapping;
  }

  public boolean hasHierarchyMappings() {
    return hierarchyMappings != null && !hierarchyMappings.isEmpty();
  }

  public Map<String, HierarchyMapping> getHierarchyMappings() {
    return Collections.unmodifiableMap(hierarchyMappings);
  }

  public void setHierarchyMappings(Map<String, HierarchyMapping> hierarchyMappings) {
    this.hierarchyMappings = hierarchyMappings;
  }

  public HierarchyMapping getHierarchyMapping(String hierarchyName) {
    if (!hasHierarchyMappings() || !hierarchyMappings.containsKey(hierarchyName)) {
      throw new SystemException("Hierarchy mapping is undefined: " + hierarchyName);
    }
    return hierarchyMappings.get(hierarchyName);
  }
}

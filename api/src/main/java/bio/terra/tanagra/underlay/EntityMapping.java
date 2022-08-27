package bio.terra.tanagra.underlay;

import bio.terra.tanagra.query.FieldVariable;
import bio.terra.tanagra.query.FilterVariable;
import bio.terra.tanagra.query.Query;
import bio.terra.tanagra.query.SQLExpression;
import bio.terra.tanagra.query.TableVariable;
import bio.terra.tanagra.query.UnionQuery;
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
  private final Map<String, AttributeMapping> attributeMappings;
  private TextSearchMapping textSearchMapping;
  private final Attribute idAttribute;

  private EntityMapping(
      TablePointer tablePointer,
      Map<String, AttributeMapping> attributeMappings,
      TextSearchMapping textSearchMapping,
      Attribute idAttribute) {
    this.tablePointer = tablePointer;
    this.attributeMappings = attributeMappings;
    this.textSearchMapping = textSearchMapping;
    this.idAttribute = idAttribute;
  }

  public static EntityMapping fromSerialized(
      UFEntityMapping serialized,
      Map<String, DataPointer> dataPointers,
      Map<String, Attribute> attributes,
      String entityName,
      String idAttributeName) {
    if (serialized.getDataPointer() == null || serialized.getDataPointer().isEmpty()) {
      throw new IllegalArgumentException("No Data Pointer defined");
    }
    if (!dataPointers.containsKey(serialized.getDataPointer())) {
      throw new IllegalArgumentException("Data Pointer not found: " + serialized.getDataPointer());
    }
    DataPointer dataPointer = dataPointers.get(serialized.getDataPointer());

    // if the table is defined, then deserialize it
    // otherwise generate a default table pointer: a table with the same name as the entity
    TablePointer tablePointer =
        serialized.getTablePointer() != null
            ? TablePointer.fromSerialized(serialized.getTablePointer(), dataPointer)
            : new TablePointer(entityName, dataPointer);

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
                throw new IllegalArgumentException(
                    "A mapping is defined for a non-existent attribute: "
                        + serializedAttributeName);
              }
            });

    TextSearchMapping textSearchMapping =
        serialized.getTextSearchMapping() == null
            ? null
            : TextSearchMapping.fromSerialized(
                serialized.getTextSearchMapping(), tablePointer, attributes);

    return new EntityMapping(
        tablePointer, attributeMappings, textSearchMapping, attributes.get(idAttributeName));
  }

  public SQLExpression queryTextSearchInformation() {
    if (!hasTextSearchMapping()) {
      throw new UnsupportedOperationException("Text search mapping is undefined");
    }

    if (textSearchMapping.definedByAttributes()) {
      return new UnionQuery(
          textSearchMapping.getAttributes().stream()
              .map(attr -> queryAttributes(Map.of("node", idAttribute, "text", attr)))
              .collect(Collectors.toList()));
    } else if (textSearchMapping.definedBySearchString()) {
      return queryAttributesAndFields(
          Map.of("node", idAttribute), Map.of("text", textSearchMapping.getSearchString()));
    } else {
      throw new IllegalArgumentException("Unknown text search mapping type");
    }
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

    FilterVariable where = tablePointer.getFilterVariable(primaryTable, tables);
    return new Query(select, tables, where);
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

  public boolean hasTextSearchMapping() {
    return textSearchMapping != null;
  }

  public TextSearchMapping getTextSearchMapping() {
    return textSearchMapping;
  }

  public void setTextSearchMapping(TextSearchMapping textSearchMapping) {
    this.textSearchMapping = textSearchMapping;
  }
}

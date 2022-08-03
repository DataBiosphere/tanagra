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
import java.util.stream.Collectors;

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

    return new EntityMapping(tablePointer, attributeMappings, textSearchMapping);
  }

  public SQLExpression queryTextSearchStrings() {
    if (!hasTextSearchMapping()) {
      throw new UnsupportedOperationException("Text search mapping is undefined");
    }

    if (textSearchMapping.definedByAttributes()) {
      return new UnionQuery(
          textSearchMapping.getAttributes().stream()
              .map(attr -> queryAttributes(List.of(attr)))
              .collect(Collectors.toList()));
    } else if (textSearchMapping.definedBySearchString()) {
      return queryFields(List.of(textSearchMapping.getSearchString()));
    } else {
      throw new IllegalArgumentException("Unknown text search mapping type");
    }
  }

  public Query queryAttributes(List<Attribute> selectedAttributes) {
    return queryAttributesAndFields(selectedAttributes, null);
  }

  public Query queryFields(List<FieldPointer> selectedFields) {
    return queryAttributesAndFields(null, selectedFields);
  }

  public Query queryAttributesAndFields(
      List<Attribute> selectedAttributes, List<FieldPointer> selectedFields) {
    List<TableVariable> tables = new ArrayList<>();
    TableVariable primaryTable = TableVariable.forPrimary(tablePointer);
    tables.add(primaryTable);

    List<FieldVariable> select = new ArrayList<>();
    if (selectedAttributes != null) {
      selectedAttributes.stream()
          .forEach(
              attr ->
                  select.addAll(
                      attributeMappings
                          .get(attr.getName())
                          .buildFieldVariables(primaryTable, tables, attr.getName())));
    }
    if (selectedFields != null) {
      selectedFields.stream().forEach(fp -> select.add(fp.buildVariable(primaryTable, tables)));
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

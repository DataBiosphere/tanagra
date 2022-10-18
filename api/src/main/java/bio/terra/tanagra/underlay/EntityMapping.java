package bio.terra.tanagra.underlay;

import bio.terra.tanagra.exception.InvalidConfigException;
import bio.terra.tanagra.query.FieldPointer;
import bio.terra.tanagra.query.FieldVariable;
import bio.terra.tanagra.query.Query;
import bio.terra.tanagra.query.TablePointer;
import bio.terra.tanagra.query.TableVariable;
import bio.terra.tanagra.serialization.UFEntityMapping;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class EntityMapping {
  private final TablePointer tablePointer;
  private Entity entity;
  private final Underlay.MappingType mappingType;

  private EntityMapping(TablePointer tablePointer, Underlay.MappingType mappingType) {
    this.tablePointer = tablePointer;
    this.mappingType = mappingType;
  }

  public void initialize(Entity entity) {
    this.entity = entity;
  }

  public static EntityMapping fromSerialized(
      UFEntityMapping serialized,
      Map<String, DataPointer> dataPointers,
      String entityName,
      Underlay.MappingType mappingType) {
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

    return new EntityMapping(tablePointer, mappingType);
  }

  public Query queryAttributes(List<Attribute> selectedAttributes) {
    return queryAttributesAndFields(selectedAttributes, null);
  }

  public Query queryAttributes(Map<String, Attribute> selectedAttributes) {
    return queryAttributesAndFields(selectedAttributes, null);
  }

  private Query queryAttributesAndFields(
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
                      entity
                          .getAttribute(nameToAttr.getKey())
                          .getMapping(mappingType)
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

  public Underlay.MappingType getMappingType() {
    return mappingType;
  }

  public TablePointer getTablePointer() {
    return tablePointer;
  }

  public Entity getEntity() {
    return entity;
  }
}

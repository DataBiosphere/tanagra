package bio.terra.tanagra.underlay;

import bio.terra.tanagra.serialization.UFAuxiliaryDataMapping;
import bio.terra.tanagra.serialization.UFEntityGroupMapping;
import bio.terra.tanagra.serialization.UFRelationshipMapping;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class EntityGroupMapping {
  private DataPointer dataPointer;
  private Map<String, RelationshipMapping> relationshipMappings;
  private Map<String, AuxiliaryDataMapping> auxiliaryDataMappings;

  private EntityGroupMapping(
      DataPointer dataPointer,
      Map<String, RelationshipMapping> relationshipMappings,
      Map<String, AuxiliaryDataMapping> auxiliaryDataMappings) {
    this.dataPointer = dataPointer;
    this.relationshipMappings = relationshipMappings;
    this.auxiliaryDataMappings = auxiliaryDataMappings;
  }

  public static EntityGroupMapping fromSerializedForSourceData(
      UFEntityGroupMapping serialized,
      Map<String, DataPointer> dataPointers,
      Map<String, Relationship> relationships,
      Map<String, AuxiliaryData> auxiliaryData) {
    return fromSerialized(serialized, dataPointers, relationships, auxiliaryData, true, false);
  }

  public static EntityGroupMapping fromSerializedForIndexData(
      UFEntityGroupMapping serialized,
      Map<String, DataPointer> dataPointers,
      Map<String, Relationship> relationships,
      Map<String, AuxiliaryData> auxiliaryData) {
    return fromSerialized(serialized, dataPointers, relationships, auxiliaryData, false, true);
  }

  private static EntityGroupMapping fromSerialized(
      UFEntityGroupMapping serialized,
      Map<String, DataPointer> dataPointers,
      Map<String, Relationship> relationships,
      Map<String, AuxiliaryData> auxiliaryData,
      boolean requireRelationships,
      boolean requireAuxiliaryData) {
    if (serialized.getDataPointer() == null || serialized.getDataPointer().isEmpty()) {
      throw new IllegalArgumentException("No Data Pointer defined");
    }
    if (!dataPointers.containsKey(serialized.getDataPointer())) {
      throw new IllegalArgumentException("Data Pointer not found: " + serialized.getDataPointer());
    }
    DataPointer dataPointer = dataPointers.get(serialized.getDataPointer());

    Map<String, UFRelationshipMapping> serializedRelationshipMappings =
        serialized.getRelationshipMappings() == null
            ? new HashMap<>()
            : serialized.getRelationshipMappings();
    Map<String, RelationshipMapping> relationshipMappings = new HashMap<>();
    for (Relationship relationship : relationships.values()) {
      UFRelationshipMapping serializedRelationship =
          serializedRelationshipMappings.get(relationship.getName());
      if (serializedRelationship == null) {
        if (requireRelationships) {
          throw new IllegalArgumentException(
              "Relationship mapping for " + relationship.getName() + " is undefined");
        } else {
          continue;
        }
      }
      relationshipMappings.put(
          relationship.getName(),
          RelationshipMapping.fromSerialized(serializedRelationship, dataPointer));
    }
    serializedRelationshipMappings
        .keySet()
        .forEach(
            srm -> {
              if (relationships.values().stream().filter(r -> r.getName().equals(srm)).findFirst()
                  == null) {
                throw new IllegalArgumentException("Unexpected relationship mapping: " + srm);
              }
            });

    Map<String, UFAuxiliaryDataMapping> serializedAuxiliaryDataMappings =
        serialized.getAuxiliaryDataMappings() == null
            ? new HashMap<>()
            : serialized.getAuxiliaryDataMappings();
    Map<String, AuxiliaryDataMapping> auxiliaryDataMappings = new HashMap<>();
    for (AuxiliaryData auxiliaryDataPointer : auxiliaryData.values()) {
      UFAuxiliaryDataMapping serializedAuxiliaryData =
          serializedAuxiliaryDataMappings.get(auxiliaryDataPointer.getName());
      if (serializedAuxiliaryData == null && !requireAuxiliaryData) {
        continue;
      }
      AuxiliaryDataMapping auxiliaryDataMapping =
          AuxiliaryDataMapping.fromSerialized(
              serializedAuxiliaryData, dataPointer, auxiliaryDataPointer);
      auxiliaryDataMappings.put(auxiliaryDataPointer.getName(), auxiliaryDataMapping);
    }
    serializedAuxiliaryDataMappings
        .keySet()
        .forEach(
            srad -> {
              if (auxiliaryData.values().stream()
                      .filter(ad -> ad.getName().equals(srad))
                      .findFirst()
                  == null) {
                throw new IllegalArgumentException("Unexpected auxiliary data mapping: " + srad);
              }
            });

    return new EntityGroupMapping(dataPointer, relationshipMappings, auxiliaryDataMappings);
  }

  public DataPointer getDataPointer() {
    return dataPointer;
  }

  public Map<String, RelationshipMapping> getRelationshipMappings() {
    return Collections.unmodifiableMap(relationshipMappings);
  }

  public Map<String, AuxiliaryDataMapping> getAuxiliaryDataMappings() {
    return Collections.unmodifiableMap(auxiliaryDataMappings);
  }
}

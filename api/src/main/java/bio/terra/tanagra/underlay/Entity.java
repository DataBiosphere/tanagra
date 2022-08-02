package bio.terra.tanagra.underlay;

import bio.terra.tanagra.indexing.WorkflowCommand;
import bio.terra.tanagra.indexing.command.DenormalizeAllNodes;
import bio.terra.tanagra.serialization.UFEntity;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Entity {
  private String name;
  private String idAttributeName;
  private Map<String, Attribute> attributes;
  private EntityMapping sourceDataMapping;
  private EntityMapping indexDataMapping;

  private Entity(
      String name,
      String idAttributeName,
      Map<String, Attribute> attributes,
      EntityMapping sourceDataMapping,
      EntityMapping indexDataMapping) {
    this.name = name;
    this.idAttributeName = idAttributeName;
    this.attributes = attributes;
    this.sourceDataMapping = sourceDataMapping;
    this.indexDataMapping = indexDataMapping;
  }

  public static Entity deserialize(UFEntity serialized, Map<String, DataPointer> dataPointers) {
    // deserialize attributes
    if (serialized.attributes == null || serialized.attributes.size() == 0) {
      throw new IllegalArgumentException("No Attributes defined: " + serialized.name);
    }
    Map<String, Attribute> attributes = new HashMap<>();
    serialized.attributes.forEach(as -> attributes.put(as.name, Attribute.fromSerialized(as)));

    if (serialized.idAttribute == null || serialized.idAttribute.isEmpty()) {
      throw new IllegalArgumentException("No id Attribute defined");
    }
    if (!attributes.containsKey(serialized.idAttribute)) {
      throw new IllegalArgumentException("Id Attribute not found in the set of Attributes");
    }

    if (serialized.sourceDataMapping == null) {
      throw new IllegalArgumentException("No source Data Mapping defined");
    }
    EntityMapping sourceDataMapping =
        EntityMapping.fromSerialized(
            serialized.sourceDataMapping, dataPointers, attributes, serialized.name);

    if (serialized.indexDataMapping == null) {
      throw new IllegalArgumentException("No index Data Mapping defined");
    }
    EntityMapping indexDataMapping =
        EntityMapping.fromSerialized(
            serialized.indexDataMapping, dataPointers, attributes, serialized.name);

    return new Entity(
        serialized.name, serialized.idAttribute, attributes, sourceDataMapping, indexDataMapping);
  }

  public List<WorkflowCommand> getIndexingCommands() {
    return List.of(DenormalizeAllNodes.forEntity(this));
  }

  public String getName() {
    return name;
  }

  public Attribute getIdAttribute() {
    return attributes.get(idAttributeName);
  }

  public List<Attribute> getAttributes() {
    return Collections.unmodifiableList(attributes.values().stream().collect(Collectors.toList()));
  }

  public EntityMapping getSourceDataMapping() {
    return sourceDataMapping;
  }

  public EntityMapping getIndexDataMapping() {
    return indexDataMapping;
  }
}

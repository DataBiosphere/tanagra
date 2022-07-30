package bio.terra.tanagra.underlay;

import bio.terra.tanagra.indexing.WorkflowCommand;
import bio.terra.tanagra.indexing.command.DenormalizeAllNodes;
import bio.terra.tanagra.serialization.UFEntity;
import bio.terra.tanagra.utils.JacksonMapper;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

  public static Entity fromJSON(String resourceFilePath, Map<String, DataPointer> dataPointers) {
    // read in entity file
    UFEntity serialized;
    try {
      serialized = JacksonMapper.readFileIntoJavaObject(resourceFilePath, UFEntity.class);
    } catch (IOException ioEx) {
      throw new RuntimeException("Error deserializing Entity from JSON", ioEx);
    }

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

  public EntityMapping getSourceDataMapping() {
    return sourceDataMapping;
  }

  public EntityMapping getIndexDataMapping() {
    return indexDataMapping;
  }
}

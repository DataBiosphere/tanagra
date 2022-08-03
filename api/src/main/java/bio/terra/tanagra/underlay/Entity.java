package bio.terra.tanagra.underlay;

import bio.terra.tanagra.indexing.WorkflowCommand;
import bio.terra.tanagra.indexing.command.BuildTextSearch;
import bio.terra.tanagra.indexing.command.DenormalizeAllNodes;
import bio.terra.tanagra.serialization.UFEntity;
import bio.terra.tanagra.utils.JacksonMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
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

  public static Entity fromJSON(
      String entityFilePath,
      Function<String, InputStream> getFileInputStreamFunction,
      Map<String, DataPointer> dataPointers)
      throws IOException {
    // read in entity file
    UFEntity serialized =
        JacksonMapper.readFileIntoJavaObject(
            getFileInputStreamFunction.apply(entityFilePath), UFEntity.class);

    // deserialize attributes
    if (serialized.getAttributes() == null || serialized.getAttributes().size() == 0) {
      throw new IllegalArgumentException("No Attributes defined: " + serialized.getName());
    }
    Map<String, Attribute> attributes = new HashMap<>();
    serialized
        .getAttributes()
        .forEach(as -> attributes.put(as.getName(), Attribute.fromSerialized(as)));

    if (serialized.getIdAttribute() == null || serialized.getIdAttribute().isEmpty()) {
      throw new IllegalArgumentException("No id Attribute defined");
    }
    if (!attributes.containsKey(serialized.getIdAttribute())) {
      throw new IllegalArgumentException("Id Attribute not found in the set of Attributes");
    }

    if (serialized.getSourceDataMapping() == null) {
      throw new IllegalArgumentException("No source Data Mapping defined");
    }
    EntityMapping sourceDataMapping =
        EntityMapping.fromSerialized(
            serialized.getSourceDataMapping(), dataPointers, attributes, serialized.getName());

    if (serialized.getIndexDataMapping() == null) {
      throw new IllegalArgumentException("No index Data Mapping defined");
    }
    EntityMapping indexDataMapping =
        EntityMapping.fromSerialized(
            serialized.getIndexDataMapping(), dataPointers, attributes, serialized.getName());

    // if the source data mapping includes text search, then expand it in the index data mapping
    if (sourceDataMapping.hasTextSearchMapping() && !indexDataMapping.hasTextSearchMapping()) {
      indexDataMapping.setTextSearchMapping(
          TextSearchMapping.getDefault(indexDataMapping.getTablePointer()));
    }

    return new Entity(
        serialized.getName(),
        serialized.getIdAttribute(),
        attributes,
        sourceDataMapping,
        indexDataMapping);
  }

  public List<WorkflowCommand> getIndexingCommands() {
    List<WorkflowCommand> cmds = new ArrayList<>();
    cmds.add(DenormalizeAllNodes.forEntity(this));
    if (sourceDataMapping.hasTextSearchMapping()) {
      cmds.add(BuildTextSearch.forEntity(this));
    }
    return cmds;
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

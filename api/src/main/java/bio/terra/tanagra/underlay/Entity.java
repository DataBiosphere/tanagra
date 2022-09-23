package bio.terra.tanagra.underlay;

import bio.terra.tanagra.exception.InvalidConfigException;
import bio.terra.tanagra.indexing.FileIO;
import bio.terra.tanagra.indexing.WorkflowCommand;
import bio.terra.tanagra.indexing.command.BuildTextSearch;
import bio.terra.tanagra.indexing.command.ComputeAncestorDescendant;
import bio.terra.tanagra.indexing.command.ComputePathNumChildren;
import bio.terra.tanagra.indexing.command.DenormalizeAllNodes;
import bio.terra.tanagra.indexing.command.WriteParentChild;
import bio.terra.tanagra.serialization.UFEntity;
import bio.terra.tanagra.utils.JacksonMapper;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class Entity {
  private static final String ENTITY_DIRECTORY_NAME = "entity";

  private final String name;
  private final String idAttributeName;
  private final Map<String, Attribute> attributes;
  private final EntityMapping sourceDataMapping;
  private final EntityMapping indexDataMapping;

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

  public static Entity fromJSON(String entityFileName, Map<String, DataPointer> dataPointers)
      throws IOException {
    // read in entity file
    Path entityFilePath =
        FileIO.getInputParentDir().resolve(ENTITY_DIRECTORY_NAME).resolve(entityFileName);
    UFEntity serialized =
        JacksonMapper.readFileIntoJavaObject(
            FileIO.getGetFileInputStreamFunction().apply(entityFilePath), UFEntity.class);

    // deserialize attributes
    if (serialized.getAttributes() == null || serialized.getAttributes().size() == 0) {
      throw new InvalidConfigException("No Attributes defined: " + serialized.getName());
    }
    Map<String, Attribute> attributes = new HashMap<>();
    serialized
        .getAttributes()
        .forEach(as -> attributes.put(as.getName(), Attribute.fromSerialized(as)));

    if (serialized.getIdAttribute() == null || serialized.getIdAttribute().isEmpty()) {
      throw new InvalidConfigException("No id Attribute defined");
    }
    if (!attributes.containsKey(serialized.getIdAttribute())) {
      throw new InvalidConfigException("Id Attribute not found in the set of Attributes");
    }

    if (serialized.getSourceDataMapping() == null) {
      throw new InvalidConfigException("No source Data Mapping defined");
    }
    EntityMapping sourceDataMapping =
        EntityMapping.fromSerialized(
            serialized.getSourceDataMapping(),
            dataPointers,
            attributes,
            serialized.getName(),
            serialized.getIdAttribute());

    if (serialized.getIndexDataMapping() == null) {
      throw new InvalidConfigException("No index Data Mapping defined");
    }
    EntityMapping indexDataMapping =
        EntityMapping.fromSerialized(
            serialized.getIndexDataMapping(),
            dataPointers,
            attributes,
            serialized.getName(),
            serialized.getIdAttribute());

    // if the source data mapping includes text search, then expand it in the index data mapping
    if (sourceDataMapping.hasTextSearchMapping() && !indexDataMapping.hasTextSearchMapping()) {
      indexDataMapping.setTextSearchMapping(
          TextSearchMapping.defaultIndexMapping(indexDataMapping.getTablePointer()));
    }

    // if the source data mapping includes hierarchies, then expand them in the index data mapping
    if (sourceDataMapping.hasHierarchyMappings() && !indexDataMapping.hasHierarchyMappings()) {
      indexDataMapping.setHierarchyMappings(
          sourceDataMapping.getHierarchyMappings().keySet().stream()
              .collect(
                  Collectors.toMap(
                      Function.identity(),
                      hierarchyName ->
                          HierarchyMapping.defaultIndexMapping(
                              serialized.getName(),
                              hierarchyName,
                              indexDataMapping.getTablePointer(),
                              indexDataMapping.getIdAttributeMapping().getValue()))));
    }

    return new Entity(
        serialized.getName(),
        serialized.getIdAttribute(),
        attributes,
        sourceDataMapping,
        indexDataMapping);
  }

  public void scanSourceData() {
    // lookup the data type and calculate a display hint for each attribute
    attributes.values().stream()
        .forEach(
            attribute -> {
              AttributeMapping attributeMapping =
                  sourceDataMapping.getAttributeMapping(attribute.getName());

              // lookup the datatype
              attribute.setDataType(attributeMapping.computeDataType());

              // generate the display hint
              if (!isIdAttribute(attribute)) {
                attribute.setDisplayHint(attributeMapping.computeDisplayHint(attribute));
              }
            });
  }

  public List<WorkflowCommand> getIndexingCommands() {
    List<WorkflowCommand> cmds = new ArrayList<>();
    cmds.add(DenormalizeAllNodes.forEntity(this));
    if (sourceDataMapping.hasTextSearchMapping()) {
      cmds.add(BuildTextSearch.forEntity(this));
    }
    if (sourceDataMapping.hasHierarchyMappings()) {
      sourceDataMapping.getHierarchyMappings().keySet().stream()
          .forEach(
              hierarchyName -> {
                cmds.add(WriteParentChild.forHierarchy(this, hierarchyName));
                cmds.add(ComputeAncestorDescendant.forHierarchy(this, hierarchyName));
                cmds.add(ComputePathNumChildren.forHierarchy(this, hierarchyName));
              });
    }
    return cmds;
  }

  public String getName() {
    return name;
  }

  public Attribute getIdAttribute() {
    return attributes.get(idAttributeName);
  }

  public boolean isIdAttribute(Attribute attribute) {
    return attribute.getName().equals(idAttributeName);
  }

  public Attribute getAttribute(String name) {
    return attributes.get(name);
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

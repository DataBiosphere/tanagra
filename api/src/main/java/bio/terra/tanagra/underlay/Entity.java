package bio.terra.tanagra.underlay;

import bio.terra.tanagra.exception.InvalidConfigException;
import bio.terra.tanagra.indexing.FileIO;
import bio.terra.tanagra.indexing.IndexingJob;
import bio.terra.tanagra.indexing.job.BuildNumChildrenAndPaths;
import bio.terra.tanagra.indexing.job.BuildTextSearchStrings;
import bio.terra.tanagra.indexing.job.DenormalizeEntityInstances;
import bio.terra.tanagra.indexing.job.WriteAncestorDescendantIdPairs;
import bio.terra.tanagra.indexing.job.WriteParentChildIdPairs;
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
  public static final String ENTITY_DIRECTORY_NAME = "entity";

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
          TextSearchMapping.defaultIndexMapping(
              serialized.getName(),
              indexDataMapping.getTablePointer(),
              attributes.get(serialized.getIdAttribute())));
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
                              indexDataMapping.getTablePointer()))));
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

  public List<IndexingJob> getIndexingJobs() {
    List<IndexingJob> jobs = new ArrayList<>();
    jobs.add(new DenormalizeEntityInstances(this));
    if (sourceDataMapping.hasTextSearchMapping()) {
      jobs.add(new BuildTextSearchStrings(this));
    }
    sourceDataMapping.getHierarchyMappings().keySet().stream()
        .forEach(
            hierarchyName -> {
              jobs.add(new WriteParentChildIdPairs(this, hierarchyName));
              jobs.add(new WriteAncestorDescendantIdPairs(this, hierarchyName));
              jobs.add(new BuildNumChildrenAndPaths(this, hierarchyName));
            });
    return jobs;
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

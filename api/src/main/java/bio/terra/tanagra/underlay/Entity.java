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
import bio.terra.tanagra.serialization.UFHierarchyMapping;
import bio.terra.tanagra.utils.JacksonMapper;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class Entity {
  public static final String ENTITY_DIRECTORY_NAME = "entity";

  private final String name;
  private final String idAttributeName;
  private final Map<String, Attribute> attributes;
  private final Map<String, Hierarchy> hierarchies;
  private final TextSearch textSearch;
  private final EntityMapping sourceDataMapping;
  private final EntityMapping indexDataMapping;

  private Entity(
      String name,
      String idAttributeName,
      Map<String, Attribute> attributes,
      Map<String, Hierarchy> hierarchies,
      TextSearch textSearch,
      EntityMapping sourceDataMapping,
      EntityMapping indexDataMapping) {
    this.name = name;
    this.idAttributeName = idAttributeName;
    this.attributes = attributes;
    this.hierarchies = hierarchies;
    this.textSearch = textSearch;
    this.sourceDataMapping = sourceDataMapping;
    this.indexDataMapping = indexDataMapping;
  }

  public static Entity fromJSON(String entityFileName, Map<String, DataPointer> dataPointers)
      throws IOException {
    // Read in entity file.
    Path entityFilePath =
        FileIO.getInputParentDir().resolve(ENTITY_DIRECTORY_NAME).resolve(entityFileName);
    UFEntity serialized =
        JacksonMapper.readFileIntoJavaObject(
            FileIO.getGetFileInputStreamFunction().apply(entityFilePath), UFEntity.class);

    // Attributes.
    if (serialized.getAttributes() == null || serialized.getAttributes().size() == 0) {
      throw new InvalidConfigException("No Attributes defined: " + serialized.getName());
    }
    Map<String, Attribute> attributes = new HashMap<>();
    serialized
        .getAttributes()
        .forEach(as -> attributes.put(as.getName(), Attribute.fromSerialized(as)));

    // ID attribute.
    if (serialized.getIdAttribute() == null || serialized.getIdAttribute().isEmpty()) {
      throw new InvalidConfigException("No id Attribute defined");
    }
    if (!attributes.containsKey(serialized.getIdAttribute())) {
      throw new InvalidConfigException("Id Attribute not found in the set of Attributes");
    }

    // Source+index entity mappings.
    if (serialized.getSourceDataMapping() == null) {
      throw new InvalidConfigException("No source Data Mapping defined");
    }
    EntityMapping sourceDataMapping =
        EntityMapping.fromSerialized(
            serialized.getSourceDataMapping(),
            dataPointers,
            serialized.getName(),
            Underlay.MappingType.SOURCE);
    if (serialized.getIndexDataMapping() == null) {
      throw new InvalidConfigException("No index Data Mapping defined");
    }
    EntityMapping indexDataMapping =
        EntityMapping.fromSerialized(
            serialized.getIndexDataMapping(),
            dataPointers,
            serialized.getName(),
            Underlay.MappingType.INDEX);

    // Source+index attribute mappings.
    for (Attribute attribute : attributes.values()) {
      AttributeMapping sourceAttributeMapping =
          AttributeMapping.fromSerialized(
              serialized.getSourceDataMapping().getAttributeMappings().get(attribute.getName()),
              sourceDataMapping.getTablePointer(),
              attribute);
      AttributeMapping indexAttributeMapping =
          serialized.getIndexDataMapping().getAttributeMappings() != null
              ? AttributeMapping.fromSerialized(
                  serialized.getIndexDataMapping().getAttributeMappings().get(attribute.getName()),
                  indexDataMapping.getTablePointer(),
                  attribute)
              : AttributeMapping.fromSerialized(
                  null, indexDataMapping.getTablePointer(), attribute);
      attribute.initialize(sourceAttributeMapping, indexAttributeMapping);
    }
    serialized.getSourceDataMapping().getAttributeMappings().keySet().stream()
        .forEach(
            serializedAttributeName -> {
              if (!attributes.containsKey(serializedAttributeName)) {
                throw new InvalidConfigException(
                    "A source mapping is defined for a non-existent attribute: "
                        + serializedAttributeName);
              }
            });

    // Source+index text search mapping.
    TextSearch textSearch = null;
    if (serialized.getSourceDataMapping().getTextSearchMapping() != null) {
      TextSearchMapping sourceTextSearchMapping =
          TextSearchMapping.fromSerialized(
              serialized.getSourceDataMapping().getTextSearchMapping(),
              sourceDataMapping.getTablePointer(),
              attributes,
              Underlay.MappingType.SOURCE);
      TextSearchMapping indexTextSearchMapping =
          serialized.getIndexDataMapping().getTextSearchMapping() == null
              ? TextSearchMapping.defaultIndexMapping(
                  serialized.getName(),
                  indexDataMapping.getTablePointer(),
                  attributes.get(serialized.getIdAttribute()))
              : TextSearchMapping.fromSerialized(
                  serialized.getIndexDataMapping().getTextSearchMapping(),
                  indexDataMapping.getTablePointer(),
                  attributes,
                  Underlay.MappingType.INDEX);
      textSearch = new TextSearch(sourceTextSearchMapping, indexTextSearchMapping);
    }

    // Source+index hierarchy mappings.
    Map<String, UFHierarchyMapping> sourceHierarchyMappingsSerialized =
        serialized.getSourceDataMapping().getHierarchyMappings();
    Map<String, UFHierarchyMapping> indexHierarchyMappingsSerialized =
        serialized.getIndexDataMapping().getHierarchyMappings();
    Map<String, Hierarchy> hierarchies = new HashMap<>();
    if (sourceHierarchyMappingsSerialized != null) {
      sourceHierarchyMappingsSerialized.entrySet().stream()
          .forEach(
              sourceHierarchyMappingSerialized -> {
                HierarchyMapping sourceMapping =
                    HierarchyMapping.fromSerialized(
                        sourceHierarchyMappingSerialized.getValue(),
                        sourceDataMapping.getTablePointer().getDataPointer());
                HierarchyMapping indexMapping =
                    indexHierarchyMappingsSerialized == null
                        ? HierarchyMapping.defaultIndexMapping(
                            serialized.getName(),
                            sourceHierarchyMappingSerialized.getKey(),
                            indexDataMapping.getTablePointer())
                        : HierarchyMapping.fromSerialized(
                            indexHierarchyMappingsSerialized.get(
                                sourceHierarchyMappingSerialized.getKey()),
                            indexDataMapping.getTablePointer().getDataPointer());
                hierarchies.put(
                    sourceHierarchyMappingSerialized.getKey(),
                    new Hierarchy(
                        sourceHierarchyMappingSerialized.getKey(), sourceMapping, indexMapping));
              });
    }

    Entity entity =
        new Entity(
            serialized.getName(),
            serialized.getIdAttribute(),
            attributes,
            hierarchies,
            textSearch,
            sourceDataMapping,
            indexDataMapping);

    sourceDataMapping.initialize(entity);
    indexDataMapping.initialize(entity);
    if (textSearch != null) {
      textSearch.initialize(entity);
    }

    return entity;
  }

  public void scanSourceData() {
    // lookup the data type and calculate a display hint for each attribute
    attributes.values().stream()
        .forEach(
            attribute -> {
              AttributeMapping attributeMapping = attribute.getMapping(Underlay.MappingType.SOURCE);

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
    if (hasTextSearch()) {
      jobs.add(new BuildTextSearchStrings(this));
    }
    getHierarchies().stream()
        .forEach(
            hierarchy -> {
              jobs.add(new WriteParentChildIdPairs(this, hierarchy.getName()));
              jobs.add(new WriteAncestorDescendantIdPairs(this, hierarchy.getName()));
              jobs.add(new BuildNumChildrenAndPaths(this, hierarchy.getName()));
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

  public Hierarchy getHierarchy(String name) {
    return hierarchies.get(name);
  }

  public List<Hierarchy> getHierarchies() {
    return Collections.unmodifiableList(hierarchies.values().stream().collect(Collectors.toList()));
  }

  public boolean hasHierarchies() {
    return hierarchies != null;
  }

  public TextSearch getTextSearch() {
    return textSearch;
  }

  public boolean hasTextSearch() {
    return textSearch != null;
  }

  public EntityMapping getMapping(Underlay.MappingType mappingType) {
    return Underlay.MappingType.SOURCE.equals(mappingType) ? sourceDataMapping : indexDataMapping;
  }
}

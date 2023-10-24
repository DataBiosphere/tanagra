package bio.terra.tanagra.serialization2;

import bio.terra.tanagra.exception.InvalidConfigException;
import bio.terra.tanagra.query.FieldPointer;
import bio.terra.tanagra.query.TablePointer;
import bio.terra.tanagra.query.bigquery.BigQueryExecutor;
import bio.terra.tanagra.underlay.DataPointer;
import bio.terra.tanagra.underlay.datapointer.BigQueryDataset;
import bio.terra.tanagra.underlay2.*;
import bio.terra.tanagra.underlay2.entitygroup.EntityGroup;
import bio.terra.tanagra.underlay2.indexschema.SchemaUtils;
import bio.terra.tanagra.utils.FileUtils;
import bio.terra.tanagra.utils.JacksonMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@SuppressFBWarnings(
    value = "NP_UNWRITTEN_PUBLIC_OR_PROTECTED_FIELD",
    justification = "Jackson object mapper writes the POJO fields during deserialization")
public final class DeserializationUtils {
  private static final String RESOURCES_CONFIG_PATH = "config/";
  private static final String INDEXER_CONFIG_SUBDIR = "indexer/";
  private static final String SERVICE_CONFIG_SUBDIR = "service/";
  private static final String UNDERLAY_CONFIG_SUBDIR = "underlay/";
  private static final String ENTITY_CONFIG_SUBDIR = "entity/";
  private static final String FILE_EXTENSION = ".json";
  private static final String UNDERLAY_FILE_NAME = "underlay";
  private static final String ENTITY_FILE_NAME = "entity";

  private DeserializationUtils() {}

  public static Underlay convertToInternalObject(SZBigQuery szBigQuery, SZUnderlay szUnderlay) {
    // Build the BigQuery source data pointer.
    BigQueryDataset sourceDataPointer =
        new BigQueryDataset(
            "source_datapointer",
            szBigQuery.sourceData.projectId,
            szBigQuery.sourceData.datasetId,
            szBigQuery.queryProjectId,
            null,
            null,
            null,
            null,
            false,
            null);

    // Initialize the SchemaUtils singleton that generates the index table names.
    BigQueryDataset indexDataPointer =
        new BigQueryDataset(
            "index_datapointer",
            szBigQuery.indexData.projectId,
            szBigQuery.indexData.datasetId,
            szBigQuery.queryProjectId,
            null,
            null,
            null,
            null,
            false,
            null);
    SchemaUtils.initialize(indexDataPointer, szBigQuery.indexData.tablePrefix);

    // Build the entities.
    List<Entity> entities = new ArrayList<>();
    szUnderlay.entities.stream()
        .forEach(
            entityName -> {
              SZEntity szEntity = deserializeEntity(szUnderlay.name, entityName);
              TablePointer sourceEntityTable =
                  readEntitySql(
                      szUnderlay.name, entityName, szEntity.allInstancesSqlFile, sourceDataPointer);

              // Build the attributes.
              List<Attribute> attributes = new ArrayList<>();
              szEntity.attributes.stream()
                  .forEach(
                      szAttribute -> {
                        FieldPointer sourceValueField =
                            new FieldPointer.Builder()
                                .tablePointer(sourceEntityTable)
                                .columnName(szAttribute.valueFieldName)
                                .build();
                        FieldPointer sourceDisplayField =
                            new FieldPointer.Builder()
                                .tablePointer(sourceEntityTable)
                                .columnName(szAttribute.displayFieldName)
                                .build();
                        attributes.add(
                            new Attribute(
                                szEntity.name,
                                szAttribute.name,
                                szAttribute.dataType,
                                szAttribute.displayFieldName != null
                                    && !szAttribute.displayFieldName.isEmpty(),
                                szEntity.idAttribute.equals(szAttribute.name),
                                sourceValueField,
                                sourceDisplayField,
                                szAttribute.isComputeDisplayHint));
                      });

              // Build the hierarchies.
              List<Hierarchy> hierarchies = new ArrayList<>();
              if (szEntity.hierarchies != null) {
                szEntity.hierarchies.stream()
                    .forEach(
                        szHierarchy -> {
                          TablePointer childParentTable =
                              readEntitySql(
                                  szUnderlay.name,
                                  szEntity.name,
                                  szHierarchy.childParentIdPairsSqlFile,
                                  sourceDataPointer);
                          FieldPointer sourceChildField =
                              new FieldPointer.Builder()
                                  .tablePointer(childParentTable)
                                  .columnName(szHierarchy.childIdFieldName)
                                  .build();
                          FieldPointer sourceParentField =
                              new FieldPointer.Builder()
                                  .tablePointer(childParentTable)
                                  .columnName(szHierarchy.parentIdFieldName)
                                  .build();
                          FieldPointer sourceRootField = null;
                          if (szHierarchy.rootNodeIdsSqlFile != null) {
                            TablePointer rootTable =
                                readEntitySql(
                                    szUnderlay.name,
                                    szEntity.name,
                                    szHierarchy.rootNodeIdsSqlFile,
                                    sourceDataPointer);
                            sourceRootField =
                                new FieldPointer.Builder()
                                    .tablePointer(rootTable)
                                    .columnName(szHierarchy.rootIdFieldName)
                                    .build();
                          }
                          hierarchies.add(
                              new Hierarchy(
                                  szEntity.name,
                                  szHierarchy.name,
                                  sourceChildField,
                                  sourceParentField,
                                  sourceRootField,
                                  szHierarchy.maxDepth,
                                  szHierarchy.keepOrphanNodes));
                        });
              }

              // Build the text search.
              TextSearch textSearch = null;
              if (szEntity.textSearch != null) {
                if (szEntity.textSearch.attributes != null
                    && !szEntity.textSearch.attributes.isEmpty()) {
                  List<Attribute> textSearchAttributes =
                      attributes.stream()
                          .filter(
                              attribute ->
                                  szEntity.textSearch.attributes.contains(attribute.getName()))
                          .collect(Collectors.toList());
                  textSearch = new TextSearch(szEntity.name, textSearchAttributes, null, null);
                } else {
                  TablePointer textSearchTable =
                      readEntitySql(
                          szUnderlay.name,
                          szEntity.name,
                          szEntity.textSearch.idTextPairsSqlFile,
                          sourceDataPointer);
                  FieldPointer textSearchIdField =
                      new FieldPointer.Builder()
                          .tablePointer(textSearchTable)
                          .columnName(szEntity.textSearch.idFieldName)
                          .build();
                  FieldPointer textSearchTextField =
                      new FieldPointer.Builder()
                          .tablePointer(textSearchTable)
                          .columnName(szEntity.textSearch.textFieldName)
                          .build();
                  textSearch =
                      new TextSearch(szEntity.name, null, textSearchIdField, textSearchTextField);
                }
              }

              List<Attribute> optimizeGroupByAttributes =
                  attributes.stream()
                      .filter(
                          attribute ->
                              szEntity.optimizeGroupByAttributes.contains(attribute.getName()))
                      .collect(Collectors.toList());
              entities.add(
                  new Entity(
                      szEntity.name,
                      szEntity.displayName,
                      szEntity.description,
                      szUnderlay.primaryEntity.equals(szEntity.name),
                      attributes,
                      optimizeGroupByAttributes,
                      hierarchies,
                      textSearch,
                      sourceEntityTable));
            });

    // Build the entity groups.
    List<EntityGroup> entityGroups = new ArrayList<>();

    // Build the query executor.
    BigQueryExecutor bigQueryExecutor = null;

    return new Underlay(
        szUnderlay.name,
        szUnderlay.metadata.displayName,
        szUnderlay.metadata.description,
        szUnderlay.metadata.properties,
        entities,
        entityGroups,
        bigQueryExecutor);
  }

  public static SZIndexer deserializeIndexer(String indexer) {
    Path indexerFile =
        Path.of(RESOURCES_CONFIG_PATH)
            .resolve(INDEXER_CONFIG_SUBDIR)
            .resolve(indexer + FILE_EXTENSION);
    try {
      return JacksonMapper.readFileIntoJavaObject(
          FileUtils.getResourceFileStream(indexerFile), SZIndexer.class);
    } catch (IOException ioEx) {
      throw new InvalidConfigException("Error deserializing indexer config file", ioEx);
    }
  }

  public static SZService deserializeService(String service) {
    Path serviceFile =
        Path.of(RESOURCES_CONFIG_PATH)
            .resolve(SERVICE_CONFIG_SUBDIR)
            .resolve(service + FILE_EXTENSION);
    try {
      return JacksonMapper.readFileIntoJavaObject(
          FileUtils.getResourceFileStream(serviceFile), SZService.class);
    } catch (IOException ioEx) {
      throw new InvalidConfigException("Error deserializing service config file", ioEx);
    }
  }

  public static SZUnderlay deserializeUnderlay(String underlay) {
    try {
      return JacksonMapper.readFileIntoJavaObject(
          FileUtils.getResourceFileStream(
              resolveUnderlayDir(underlay).resolve(UNDERLAY_FILE_NAME + FILE_EXTENSION)),
          SZUnderlay.class);
    } catch (IOException ioEx) {
      throw new InvalidConfigException("Error deserializing underlay config file", ioEx);
    }
  }

  public static SZEntity deserializeEntity(String underlay, String entity) {
    try {
      return JacksonMapper.readFileIntoJavaObject(
          FileUtils.getResourceFileStream(
              resolveEntityDir(underlay, entity).resolve(ENTITY_FILE_NAME + FILE_EXTENSION)),
          SZEntity.class);
    } catch (IOException ioEx) {
      throw new InvalidConfigException("Error deserializing entity config file", ioEx);
    }
  }

  public static SZGroupItems deserializeGroupItems(String underlay, String groupItems) {
    return null;
  }

  public static SZCriteriaOccurrence deserializeCriteriaOccurrence(
      String underlay, String criteriaOccurrence) {
    return null;
  }

  private static TablePointer readEntitySql(
      String underlay, String entity, String fileName, DataPointer dataPointer) {
    Path sqlFile = resolveEntityDir(underlay, entity).resolve(fileName);
    String sqlStr = FileUtils.readStringFromFile(FileUtils.getResourceFileStream(sqlFile));
    return TablePointer.fromRawSql(sqlStr, dataPointer);
  }

  private static Path resolveUnderlayDir(String underlay) {
    return Path.of(RESOURCES_CONFIG_PATH).resolve(UNDERLAY_CONFIG_SUBDIR).resolve(underlay);
  }

  private static Path resolveEntityDir(String underlay, String entity) {
    return resolveUnderlayDir(underlay).resolve(ENTITY_CONFIG_SUBDIR).resolve(entity);
  }
}

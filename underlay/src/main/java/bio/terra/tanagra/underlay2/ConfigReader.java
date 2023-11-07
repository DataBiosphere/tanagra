package bio.terra.tanagra.underlay2;

import bio.terra.tanagra.exception.InvalidConfigException;
import bio.terra.tanagra.underlay2.serialization.SZCriteriaOccurrence;
import bio.terra.tanagra.underlay2.serialization.SZEntity;
import bio.terra.tanagra.underlay2.serialization.SZGroupItems;
import bio.terra.tanagra.underlay2.serialization.SZIndexer;
import bio.terra.tanagra.underlay2.serialization.SZService;
import bio.terra.tanagra.underlay2.serialization.SZUnderlay;
import bio.terra.tanagra.utils.FileUtils;
import bio.terra.tanagra.utils.JacksonMapper;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.text.StringSubstitutor;

@SuppressFBWarnings(
    value = "NP_UNWRITTEN_PUBLIC_OR_PROTECTED_FIELD",
    justification = "Jackson object mapper writes the POJO fields during deserialization")
public final class ConfigReader {
  private static final String RESOURCES_CONFIG_PATH = "config/";
  private static final String INDEXER_CONFIG_SUBDIR = "indexer/";
  private static final String SERVICE_CONFIG_SUBDIR = "service/";
  private static final String UNDERLAY_CONFIG_SUBDIR = "underlay/";
  private static final String DATA_MAPPING_CONFIG_SUBDIR = "datamapping/";
  private static final String ENTITY_CONFIG_SUBDIR = "entity/";
  private static final String ENTITY_GROUP_CONFIG_SUBDIR = "entitygroup/";
  private static final String FILE_EXTENSION = ".json";
  private static final String UNDERLAY_FILE_NAME = "underlay";
  private static final String ENTITY_FILE_NAME = "entity";
  private static final String ENTITY_GROUP_FILE_NAME = "entityGroup";

  private final Map<String, SZEntity> szEntityCache = new HashMap<>();
  private final Map<String, SZGroupItems> szGroupItemsCache = new HashMap<>();
  private final Map<String, SZCriteriaOccurrence> szCriteriaOccurrenceCache = new HashMap<>();
  private final Map<Pair<String, String>, String> entitySqlCache = new HashMap<>();
  private final Map<Pair<String, String>, String> entityGroupSqlCache = new HashMap<>();
  private final String underlay;
  private final ImmutableMap<String, String> sqlSubstitutions;

  public ConfigReader(String underlay, Map<String, String> sqlSubstitutions) {
    this.underlay = underlay;
    this.sqlSubstitutions =
        sqlSubstitutions == null ? ImmutableMap.of() : ImmutableMap.copyOf(sqlSubstitutions);
  }

  public SZEntity readEntity(String entityPath) {
    if (!szEntityCache.containsKey(entityPath)) {
      szEntityCache.put(entityPath, ConfigReader.deserializeEntity(entityPath));
    }
    return szEntityCache.get(entityPath);
  }

  public SZGroupItems readGroupItems(String groupItemsPath) {
    if (!szGroupItemsCache.containsKey(groupItemsPath)) {
      szGroupItemsCache.put(
          groupItemsPath, ConfigReader.deserializeGroupItems(groupItemsPath));
    }
    return szGroupItemsCache.get(groupItemsPath);
  }

  public SZCriteriaOccurrence readCriteriaOccurrence(String criteriaOccurrencePath) {
    if (!szCriteriaOccurrenceCache.containsKey(criteriaOccurrencePath)) {
      szCriteriaOccurrenceCache.put(
          criteriaOccurrencePath,
          ConfigReader.deserializeCriteriaOccurrence(criteriaOccurrencePath));
    }
    return szCriteriaOccurrenceCache.get(criteriaOccurrencePath);
  }

  public String readEntitySql(String entityPath, String fileName) {
    if (!entitySqlCache.containsKey(Pair.of(entityPath, fileName))) {
      Path sqlFile = resolveEntityDir(entityPath).resolve(fileName);
      String sql = FileUtils.readStringFromFile(FileUtils.getResourceFileStream(sqlFile));
      entitySqlCache.put(
          Pair.of(entityPath, fileName), StringSubstitutor.replace(sql, sqlSubstitutions));
    }
    return entitySqlCache.get(Pair.of(entityPath, fileName));
  }

  public String readEntityGroupSql(String entityGroupPath, String fileName) {
    if (!entityGroupSqlCache.containsKey(Pair.of(entityGroupPath, fileName))) {
      Path sqlFile = resolveEntityGroupDir(entityGroupPath).resolve(fileName);
      String sql = FileUtils.readStringFromFile(FileUtils.getResourceFileStream(sqlFile));
      entityGroupSqlCache.put(
          Pair.of(entityGroupPath, fileName), StringSubstitutor.replace(sql, sqlSubstitutions));
    }
    return entityGroupSqlCache.get(Pair.of(entityGroupPath, fileName));
  }
  public String readUIConfig(String fileName) {
    Path uiConfigFile = resolveUnderlayDir(underlay).resolve(fileName);
    return FileUtils.readStringFromFile(FileUtils.getResourceFileStream(uiConfigFile));
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

  @VisibleForTesting
  public static SZUnderlay deserializeUnderlay(String underlay) {
    try {
      SZUnderlay szUnderlay =
          JacksonMapper.readFileIntoJavaObject(
              FileUtils.getResourceFileStream(
                  resolveUnderlayDir(underlay).resolve(UNDERLAY_FILE_NAME + FILE_EXTENSION)),
              SZUnderlay.class);

      // Initialize null collections to empty collections.
      szUnderlay.entities = szUnderlay.entities == null ? new HashSet<>() : szUnderlay.entities;
      szUnderlay.groupItemsEntityGroups =
          szUnderlay.groupItemsEntityGroups == null
              ? new HashSet<>()
              : szUnderlay.groupItemsEntityGroups;
      szUnderlay.criteriaOccurrenceEntityGroups =
          szUnderlay.criteriaOccurrenceEntityGroups == null
              ? new HashSet<>()
              : szUnderlay.criteriaOccurrenceEntityGroups;
      szUnderlay.metadata.properties =
          szUnderlay.metadata.properties == null ? new HashMap<>() : szUnderlay.metadata.properties;
      return szUnderlay;
    } catch (IOException ioEx) {
      throw new InvalidConfigException("Error deserializing underlay config file", ioEx);
    }
  }

  @VisibleForTesting
  public static SZEntity deserializeEntity(String entityPath) {
    try {
      SZEntity szEntity =
          JacksonMapper.readFileIntoJavaObject(
              FileUtils.getResourceFileStream(
                  resolveEntityDir(entityPath).resolve(ENTITY_FILE_NAME + FILE_EXTENSION)),
              SZEntity.class);

      // Initialize null collections to empty collections.
      szEntity.attributes = szEntity.attributes == null ? new HashSet<>() : szEntity.attributes;
      szEntity.hierarchies = szEntity.hierarchies == null ? new HashSet<>() : szEntity.hierarchies;

      return szEntity;
    } catch (IOException ioEx) {
      throw new InvalidConfigException("Error deserializing entity config file", ioEx);
    }
  }

  private static SZGroupItems deserializeGroupItems(String groupItemsPath) {
    try {
      return JacksonMapper.readFileIntoJavaObject(
          FileUtils.getResourceFileStream(
              resolveEntityGroupDir(groupItemsPath)
                  .resolve(ENTITY_GROUP_FILE_NAME + FILE_EXTENSION)),
          SZGroupItems.class);
    } catch (IOException ioEx) {
      throw new InvalidConfigException(
          "Error deserializing group items entity group config file", ioEx);
    }
  }

  @VisibleForTesting
  public static SZCriteriaOccurrence deserializeCriteriaOccurrence(String criteriaOccurrencePath) {
    try {
      SZCriteriaOccurrence szCriteriaOccurrence =
          JacksonMapper.readFileIntoJavaObject(
              FileUtils.getResourceFileStream(
                  resolveEntityGroupDir(criteriaOccurrencePath)
                      .resolve(ENTITY_GROUP_FILE_NAME + FILE_EXTENSION)),
              SZCriteriaOccurrence.class);

      // Initialize null collections to empty collections.
      szCriteriaOccurrence.occurrenceEntities =
          szCriteriaOccurrence.occurrenceEntities == null
              ? new HashSet<>()
              : szCriteriaOccurrence.occurrenceEntities;
      szCriteriaOccurrence.occurrenceEntities.stream()
          .forEach(
              szOccurrenceEntity ->
                  szOccurrenceEntity.attributesWithInstanceLevelHints =
                      szOccurrenceEntity.attributesWithInstanceLevelHints == null
                          ? new HashSet<>()
                          : szOccurrenceEntity.attributesWithInstanceLevelHints);

      return szCriteriaOccurrence;
    } catch (IOException ioEx) {
      throw new InvalidConfigException(
          "Error deserializing criteria occurrence entity group config file", ioEx);
    }
  }

  private static Path resolveUnderlayDir(String underlay) {
    return Path.of(RESOURCES_CONFIG_PATH).resolve(UNDERLAY_CONFIG_SUBDIR).resolve(underlay);
  }

  private static Path resolveEntityDir(String entityPath) {
    Pair<String, String> underlayEntity = parseEntityOrGroupPath(entityPath);
    return Path.of(RESOURCES_CONFIG_PATH).resolve(DATA_MAPPING_CONFIG_SUBDIR).resolve(underlayEntity.getLeft()).resolve(ENTITY_CONFIG_SUBDIR).resolve(underlayEntity.getRight());
  }

  private static Path resolveEntityGroupDir(String entityGroupPath) {
    Pair<String, String> underlayEntityGroup = parseEntityOrGroupPath(entityGroupPath);
    return Path.of(RESOURCES_CONFIG_PATH).resolve(DATA_MAPPING_CONFIG_SUBDIR).resolve(underlayEntityGroup.getLeft()).resolve(ENTITY_GROUP_CONFIG_SUBDIR).resolve(underlayEntityGroup.getRight());
  }

  private static Pair<String, String> parseEntityOrGroupPath(String path) {
    String[] underlayEntityPathSplit = path.split("/");
    if (underlayEntityPathSplit.length != 2) {
      throw new InvalidConfigException("Invalid underlay/entity or underlay/entityGroup path: " + path);
    }
    String underlay = underlayEntityPathSplit[0];
    String entityOrGroup = underlayEntityPathSplit[1];
    return Pair.of(underlay, entityOrGroup);
  }
}

package bio.terra.tanagra.underlay;

import bio.terra.tanagra.api.shared.DataType;
import bio.terra.tanagra.exception.InvalidConfigException;
import bio.terra.tanagra.underlay.entitymodel.Hierarchy;
import bio.terra.tanagra.underlay.serialization.SZCriteriaOccurrence;
import bio.terra.tanagra.underlay.serialization.SZCriteriaSelector;
import bio.terra.tanagra.underlay.serialization.SZDataType;
import bio.terra.tanagra.underlay.serialization.SZEntity;
import bio.terra.tanagra.underlay.serialization.SZGroupItems;
import bio.terra.tanagra.underlay.serialization.SZIndexer;
import bio.terra.tanagra.underlay.serialization.SZPrepackagedCriteria;
import bio.terra.tanagra.underlay.serialization.SZService;
import bio.terra.tanagra.underlay.serialization.SZUnderlay;
import bio.terra.tanagra.utils.FileUtils;
import bio.terra.tanagra.utils.JacksonMapper;
import com.google.common.collect.ImmutableMap;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import javax.annotation.Nullable;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.text.StringSubstitutor;

@SuppressFBWarnings(
    value = "NP_UNWRITTEN_PUBLIC_OR_PROTECTED_FIELD",
    justification = "Jackson object mapper writes the POJO fields during deserialization")
public final class ConfigReader {
  private static final String RESOURCES_DIR_DISK_PATH = "underlay/src/main/resources/";
  private static final String RESOURCES_CONFIG_PATH = "config/";
  private static final String INDEXER_CONFIG_SUBDIR = "indexer/";
  private static final String SERVICE_CONFIG_SUBDIR = "service/";
  private static final String UNDERLAY_CONFIG_SUBDIR = "underlay/";
  private static final String DATA_MAPPING_CONFIG_SUBDIR = "datamapping/";
  private static final String ENTITY_CONFIG_SUBDIR = "entity/";
  private static final String ENTITY_GROUP_CONFIG_SUBDIR = "entitygroup/";
  private static final String DISPLAY_CONFIG_SUBDIR = "display/";
  private static final String CRITERIA_SELECTOR_CONFIG_SUBDIR = "criteriaselector/";
  private static final String PREPACKAGED_CRITERIA_CONFIG_SUBDIR = "prepackagedcriteria/";
  private static final String FILE_EXTENSION = ".json";
  private static final String UNDERLAY_FILE_NAME = "underlay";
  private static final String ENTITY_FILE_NAME = "entity";
  private static final String ENTITY_GROUP_FILE_NAME = "entityGroup";
  private static final String CRITERIA_SELECTOR_FILE_NAME = "selector";
  private static final String PREPACKAGED_CRITERIA_FILE_NAME = "prepackaged";
  private final Map<String, SZEntity> szEntityCache = new HashMap<>();
  private final Map<String, SZGroupItems> szGroupItemsCache = new HashMap<>();
  private final Map<String, SZCriteriaOccurrence> szCriteriaOccurrenceCache = new HashMap<>();
  private final Map<String, SZCriteriaSelector> szCriteriaSelectorCache = new HashMap<>();
  private final Map<String, SZPrepackagedCriteria> szPrepackagedCriteriaCache = new HashMap<>();
  private final Map<Pair<String, String>, String> entitySqlCache = new HashMap<>();
  private final Map<Pair<String, String>, String> entityGroupSqlCache = new HashMap<>();
  private final Map<Pair<String, String>, String> criteriaSelectorPluginConfigCache =
      new HashMap<>();
  private final Map<Pair<String, String>, String> prepackagedCriteriaPluginConfigCache =
      new HashMap<>();
  private String underlay;
  private ImmutableMap<String, String> sqlSubstitutions = ImmutableMap.of();
  private final boolean useResourcesInputStream;
  private final @Nullable Path topLevelProjectDir;

  private ConfigReader(boolean useResourcesInputStream, @Nullable Path topLevelProjectDir) {
    this.useResourcesInputStream = useResourcesInputStream;
    this.topLevelProjectDir = topLevelProjectDir;
  }

  public static ConfigReader fromJarResources() {
    return new ConfigReader(true, null);
  }

  public static ConfigReader fromDiskFile(Path topLevelProjectDir) {
    return new ConfigReader(false, topLevelProjectDir);
  }

  public ConfigReader setUnderlay(String underlay) {
    this.underlay = underlay;
    return this;
  }

  public ConfigReader setSqlSubstitutions(Map<String, String> sqlSubstitutions) {
    this.sqlSubstitutions = ImmutableMap.copyOf(sqlSubstitutions);
    return this;
  }

  public SZEntity readEntity(String entityPath) {
    if (!szEntityCache.containsKey(entityPath)) {
      SZEntity szEntity = deserializeEntity(entityPath);
      if (szEntity.sourceQueryTableName != null) {
        szEntity.sourceQueryTableName =
            StringSubstitutor.replace(szEntity.sourceQueryTableName, sqlSubstitutions);
        szEntity.attributes.stream()
            .filter(
                szAttribute ->
                    szAttribute.sourceQuery != null
                        && szAttribute.sourceQuery.displayFieldTable != null)
            .forEach(
                szAttribute -> {
                  szAttribute.sourceQuery.displayFieldTable =
                      StringSubstitutor.replace(
                          szAttribute.sourceQuery.displayFieldTable, sqlSubstitutions);
                });
      }
      szEntityCache.put(entityPath, szEntity);
    }
    return szEntityCache.get(entityPath);
  }

  public SZGroupItems readGroupItems(String groupItemsPath) {
    if (!szGroupItemsCache.containsKey(groupItemsPath)) {
      szGroupItemsCache.put(groupItemsPath, deserializeGroupItems(groupItemsPath));
    }
    return szGroupItemsCache.get(groupItemsPath);
  }

  public SZCriteriaOccurrence readCriteriaOccurrence(String criteriaOccurrencePath) {
    if (!szCriteriaOccurrenceCache.containsKey(criteriaOccurrencePath)) {
      szCriteriaOccurrenceCache.put(
          criteriaOccurrencePath, deserializeCriteriaOccurrence(criteriaOccurrencePath));
    }
    return szCriteriaOccurrenceCache.get(criteriaOccurrencePath);
  }

  public SZCriteriaSelector readCriteriaSelector(String criteriaSelectorPath) {
    if (!szCriteriaSelectorCache.containsKey(criteriaSelectorPath)) {
      szCriteriaSelectorCache.put(
          criteriaSelectorPath, deserializeCriteriaSelector(criteriaSelectorPath));
    }
    return szCriteriaSelectorCache.get(criteriaSelectorPath);
  }

  public SZPrepackagedCriteria readPrepackagedCriteria(String prepackagedCriteriaPath) {
    if (!szPrepackagedCriteriaCache.containsKey(prepackagedCriteriaPath)) {
      szPrepackagedCriteriaCache.put(
          prepackagedCriteriaPath, deserializePrepackagedCriteria(prepackagedCriteriaPath));
    }
    return szPrepackagedCriteriaCache.get(prepackagedCriteriaPath);
  }

  public String readEntitySql(String entityPath, String fileName) {
    if (!entitySqlCache.containsKey(Pair.of(entityPath, fileName))) {
      Path sqlFile = resolveEntityDir(entityPath).resolve(fileName);
      String sql = FileUtils.readStringFromFile(getStream(sqlFile));
      entitySqlCache.put(
          Pair.of(entityPath, fileName), StringSubstitutor.replace(sql, sqlSubstitutions));
    }
    return entitySqlCache.get(Pair.of(entityPath, fileName));
  }

  public String readEntityGroupSql(String entityGroupPath, String fileName) {
    if (!entityGroupSqlCache.containsKey(Pair.of(entityGroupPath, fileName))) {
      Path sqlFile = resolveEntityGroupDir(entityGroupPath).resolve(fileName);
      String sql = FileUtils.readStringFromFile(getStream(sqlFile));
      entityGroupSqlCache.put(
          Pair.of(entityGroupPath, fileName), StringSubstitutor.replace(sql, sqlSubstitutions));
    }
    return entityGroupSqlCache.get(Pair.of(entityGroupPath, fileName));
  }

  public String readCriteriaSelectorPluginConfig(String criteriaSelectorPath, String fileName) {
    if (!criteriaSelectorPluginConfigCache.containsKey(Pair.of(criteriaSelectorPath, fileName))) {
      Path pluginConfigFile = resolveCriteriaSelectorDir(criteriaSelectorPath).resolve(fileName);
      String config = FileUtils.readStringFromFile(getStream(pluginConfigFile));
      criteriaSelectorPluginConfigCache.put(Pair.of(criteriaSelectorPath, fileName), config);
    }
    return criteriaSelectorPluginConfigCache.get(Pair.of(criteriaSelectorPath, fileName));
  }

  public String readPrepackagedCriteriaPluginConfig(
      String prepackagedCriteriaPath, String fileName) {
    if (!prepackagedCriteriaPluginConfigCache.containsKey(
        Pair.of(prepackagedCriteriaPath, fileName))) {
      Path pluginConfigFile =
          resolvePrepackagedCriteriaDir(prepackagedCriteriaPath).resolve(fileName);
      String config = FileUtils.readStringFromFile(getStream(pluginConfigFile));
      prepackagedCriteriaPluginConfigCache.put(Pair.of(prepackagedCriteriaPath, fileName), config);
    }
    return prepackagedCriteriaPluginConfigCache.get(Pair.of(prepackagedCriteriaPath, fileName));
  }

  public String readUIConfig(String fileName) {
    Path uiConfigFile = resolveUnderlayDir(underlay).resolve(fileName);
    return FileUtils.readStringFromFile(getStream(uiConfigFile));
  }

  public SZIndexer readIndexer(String indexer) {
    Path indexerFile =
        Path.of(RESOURCES_CONFIG_PATH)
            .resolve(INDEXER_CONFIG_SUBDIR)
            .resolve(indexer + FILE_EXTENSION);
    try {
      return JacksonMapper.readFileIntoJavaObject(getStream(indexerFile), SZIndexer.class);
    } catch (IOException ioEx) {
      throw new InvalidConfigException("Error deserializing indexer config file", ioEx);
    }
  }

  public SZService readService(String service) {
    Path serviceFile =
        Path.of(RESOURCES_CONFIG_PATH)
            .resolve(SERVICE_CONFIG_SUBDIR)
            .resolve(service + FILE_EXTENSION);
    try {
      return JacksonMapper.readFileIntoJavaObject(getStream(serviceFile), SZService.class);
    } catch (IOException ioEx) {
      throw new InvalidConfigException("Error deserializing service config file", ioEx);
    }
  }

  public SZUnderlay readUnderlay(String underlay) {
    try {
      SZUnderlay szUnderlay =
          JacksonMapper.readFileIntoJavaObject(
              getStream(resolveUnderlayDir(underlay).resolve(UNDERLAY_FILE_NAME + FILE_EXTENSION)),
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

  private SZEntity deserializeEntity(String entityPath) {
    try {
      SZEntity szEntity =
          JacksonMapper.readFileIntoJavaObject(
              getStream(resolveEntityDir(entityPath).resolve(ENTITY_FILE_NAME + FILE_EXTENSION)),
              SZEntity.class);

      // Initialize null collections to empty collections.
      szEntity.attributes = szEntity.attributes == null ? new ArrayList<>() : szEntity.attributes;
      szEntity.hierarchies = szEntity.hierarchies == null ? new HashSet<>() : szEntity.hierarchies;

      // Set hierarchy names to default, if not otherwise specified.
      szEntity.hierarchies.stream()
          .forEach(
              szHierarchy -> {
                if (szHierarchy.name == null) {
                  szHierarchy.name = Hierarchy.DEFAULT_NAME;
                }
              });

      return szEntity;
    } catch (IOException ioEx) {
      throw new InvalidConfigException("Error deserializing entity config file", ioEx);
    }
  }

  private SZGroupItems deserializeGroupItems(String groupItemsPath) {
    try {
      return JacksonMapper.readFileIntoJavaObject(
          getStream(
              resolveEntityGroupDir(groupItemsPath)
                  .resolve(ENTITY_GROUP_FILE_NAME + FILE_EXTENSION)),
          SZGroupItems.class);
    } catch (IOException ioEx) {
      throw new InvalidConfigException(
          "Error deserializing group items entity group config file", ioEx);
    }
  }

  private SZCriteriaOccurrence deserializeCriteriaOccurrence(String criteriaOccurrencePath) {
    try {
      SZCriteriaOccurrence szCriteriaOccurrence =
          JacksonMapper.readFileIntoJavaObject(
              getStream(
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

  private SZCriteriaSelector deserializeCriteriaSelector(String criteriaSelectorPath) {
    try {
      SZCriteriaSelector szCriteriaSelector =
          JacksonMapper.readFileIntoJavaObject(
              getStream(
                  resolveCriteriaSelectorDir(criteriaSelectorPath)
                      .resolve(CRITERIA_SELECTOR_FILE_NAME + FILE_EXTENSION)),
              SZCriteriaSelector.class);

      // Initialize null collections to empty collections.
      szCriteriaSelector.modifiers =
          szCriteriaSelector.modifiers == null ? new ArrayList<>() : szCriteriaSelector.modifiers;

      return szCriteriaSelector;
    } catch (IOException ioEx) {
      throw new InvalidConfigException("Error deserializing criteria selector config file", ioEx);
    }
  }

  private SZPrepackagedCriteria deserializePrepackagedCriteria(String prepackagedCriteriaPath) {
    try {
      return JacksonMapper.readFileIntoJavaObject(
          getStream(
              resolvePrepackagedCriteriaDir(prepackagedCriteriaPath)
                  .resolve(PREPACKAGED_CRITERIA_FILE_NAME + FILE_EXTENSION)),
          SZPrepackagedCriteria.class);
    } catch (IOException ioEx) {
      throw new InvalidConfigException(
          "Error deserializing prepackaged criteria config file", ioEx);
    }
  }

  private InputStream getStream(Path resourcesPath) {
    try {
      return useResourcesInputStream
          ? FileUtils.getResourceFileStream(resourcesPath)
          : FileUtils.getFileStream(
              topLevelProjectDir.resolve(RESOURCES_DIR_DISK_PATH).resolve(resourcesPath));
    } catch (IOException ioEx) {
      throw new InvalidConfigException("Error loading config file: " + resourcesPath, ioEx);
    }
  }

  private static Path resolveUnderlayDir(String underlay) {
    return Path.of(RESOURCES_CONFIG_PATH).resolve(UNDERLAY_CONFIG_SUBDIR).resolve(underlay);
  }

  private static Path resolveEntityDir(String entityPath) {
    Pair<String, String> underlayEntity = parseTwoPartPath(entityPath);
    return Path.of(RESOURCES_CONFIG_PATH)
        .resolve(DATA_MAPPING_CONFIG_SUBDIR)
        .resolve(underlayEntity.getLeft())
        .resolve(ENTITY_CONFIG_SUBDIR)
        .resolve(underlayEntity.getRight());
  }

  private static Path resolveEntityGroupDir(String entityGroupPath) {
    Pair<String, String> underlayEntityGroup = parseTwoPartPath(entityGroupPath);
    return Path.of(RESOURCES_CONFIG_PATH)
        .resolve(DATA_MAPPING_CONFIG_SUBDIR)
        .resolve(underlayEntityGroup.getLeft())
        .resolve(ENTITY_GROUP_CONFIG_SUBDIR)
        .resolve(underlayEntityGroup.getRight());
  }

  private static Path resolveCriteriaSelectorDir(String criteriaSelectorPath) {
    Pair<String, String> underlayCriteriaSelector = parseTwoPartPath(criteriaSelectorPath);
    return Path.of(RESOURCES_CONFIG_PATH)
        .resolve(DISPLAY_CONFIG_SUBDIR)
        .resolve(underlayCriteriaSelector.getLeft())
        .resolve(CRITERIA_SELECTOR_CONFIG_SUBDIR)
        .resolve(underlayCriteriaSelector.getRight());
  }

  private static Path resolvePrepackagedCriteriaDir(String prepackagedCriteriaPath) {
    Pair<String, String> underlayPrepackagedCriteria = parseTwoPartPath(prepackagedCriteriaPath);
    return Path.of(RESOURCES_CONFIG_PATH)
        .resolve(DISPLAY_CONFIG_SUBDIR)
        .resolve(underlayPrepackagedCriteria.getLeft())
        .resolve(PREPACKAGED_CRITERIA_CONFIG_SUBDIR)
        .resolve(underlayPrepackagedCriteria.getRight());
  }

  private static Pair<String, String> parseTwoPartPath(String path) {
    String[] underlayEntityPathSplit = path.split("/");
    if (underlayEntityPathSplit.length <= 1) {
      throw new InvalidConfigException(
          "Invalid underlay/entity or underlay/entityGroup path: " + path);
    }
    String underlay = underlayEntityPathSplit[0];
    String entityOrGroup = underlayEntityPathSplit[1];
    return Pair.of(underlay, entityOrGroup);
  }

  public static DataType deserializeDataType(@Nullable SZDataType szDataType) {
    return szDataType == null ? null : DataType.valueOf(szDataType.name());
  }
}

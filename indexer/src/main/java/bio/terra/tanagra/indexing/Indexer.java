package bio.terra.tanagra.indexing;

import static bio.terra.tanagra.underlay.Entity.ENTITY_DIRECTORY_NAME;
import static bio.terra.tanagra.underlay.EntityGroup.ENTITY_GROUP_DIRECTORY_NAME;
import static bio.terra.tanagra.underlay.Underlay.OUTPUT_UNDERLAY_FILE_EXTENSION;

import bio.terra.tanagra.indexing.job.BuildNumChildrenAndPaths;
import bio.terra.tanagra.indexing.job.BuildTextSearchStrings;
import bio.terra.tanagra.indexing.job.ComputeDisplayHints;
import bio.terra.tanagra.indexing.job.ComputeRollupCounts;
import bio.terra.tanagra.indexing.job.CreateEntityTable;
import bio.terra.tanagra.indexing.job.DenormalizeEntityInstances;
import bio.terra.tanagra.indexing.job.WriteAncestorDescendantIdPairs;
import bio.terra.tanagra.indexing.job.WriteParentChildIdPairs;
import bio.terra.tanagra.indexing.job.WriteRelationshipIdPairs;
import bio.terra.tanagra.serialization.UFEntity;
import bio.terra.tanagra.serialization.UFEntityGroup;
import bio.terra.tanagra.serialization.UFUnderlay;
import bio.terra.tanagra.underlay.Entity;
import bio.terra.tanagra.underlay.EntityGroup;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.entitygroup.CriteriaOccurrence;
import bio.terra.tanagra.utils.FileIO;
import bio.terra.tanagra.utils.HttpUtils;
import bio.terra.tanagra.utils.JacksonMapper;
import com.google.common.annotations.VisibleForTesting;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Indexer {
  private static final Logger LOGGER = LoggerFactory.getLogger(Indexer.class);

  private final Underlay underlay;
  private UFUnderlay expandedUnderlay;
  private List<UFEntity> expandedEntities;
  private List<UFEntityGroup> expandedEntityGroups;

  private Indexer(Underlay underlay) {
    this.underlay = underlay;
  }

  /** Deserialize the POJOs to the internal objects and expand all defaults. */
  public static Indexer deserializeUnderlay(String underlayFileName) throws IOException {
    return new Indexer(Underlay.fromJSON(underlayFileName));
  }

  /** Scan the source data to validate data pointers, lookup data types, generate UI hints, etc. */
  public void scanSourceData() {
    // TODO: Validate existence and access for data/table/field pointers.
    underlay
        .getEntities()
        .values()
        .forEach(
            e -> {
              LOGGER.info(
                  "Looking up attribute data types and generating UI hints for entity: "
                      + e.getName());
              e.scanSourceData();
            });
  }

  /** Convert the internal objects, now expanded, back to POJOs. */
  public void serializeUnderlay() {
    LOGGER.info("Serializing expanded underlay objects");
    expandedUnderlay = new UFUnderlay(underlay);
    expandedEntities =
        underlay.getEntities().values().stream()
            .map(e -> new UFEntity(e))
            .collect(Collectors.toList());
    expandedEntityGroups =
        underlay.getEntityGroups().values().stream()
            .map(eg -> eg.serialize())
            .collect(Collectors.toList());
  }

  /** Write out the expanded POJOs. */
  public void writeSerializedUnderlay() throws IOException {
    // Write out the underlay POJO to the top-level directory.
    Path underlayPath =
        FileIO.getOutputParentDir()
            .resolve(expandedUnderlay.getName() + OUTPUT_UNDERLAY_FILE_EXTENSION);
    JacksonMapper.writeJavaObjectToFile(underlayPath, expandedUnderlay);

    // Write out the entity POJOs to the entity/ sub-directory.
    Path entitySubDir = FileIO.getOutputParentDir().resolve(ENTITY_DIRECTORY_NAME);
    for (UFEntity expandedEntity : expandedEntities) {
      JacksonMapper.writeJavaObjectToFile(
          entitySubDir.resolve(expandedEntity.getName() + OUTPUT_UNDERLAY_FILE_EXTENSION),
          expandedEntity);
    }

    // Write out the entity group POJOs to the entity_group/ sub-directory.
    Path entityGroupSubDir = FileIO.getOutputParentDir().resolve(ENTITY_GROUP_DIRECTORY_NAME);
    for (UFEntityGroup expandedEntityGroup : expandedEntityGroups) {
      JacksonMapper.writeJavaObjectToFile(
          entityGroupSubDir.resolve(expandedEntityGroup.getName() + OUTPUT_UNDERLAY_FILE_EXTENSION),
          expandedEntityGroup);
    }
  }

  public void runJobsForAllEntities(boolean isDryRun) {
    underlay.getEntities().keySet().stream()
        .sorted()
        .forEach(name -> runJobsForEntity(name, isDryRun));
  }

  public void runJobsForEntity(String name, boolean isDryRun) {
    LOGGER.info("RUN entity: {}", name);
    getJobsForEntity(underlay.getEntity(name))
        .forEach(ij -> tolerateJobExceptions(() -> ij.checkStatusAndRun(isDryRun), ij.getName()));
  }

  public void runJobsForAllEntityGroups(boolean isDryRun) {
    underlay.getEntityGroups().keySet().stream()
        .sorted()
        .forEach(name -> runJobsForEntityGroup(name, isDryRun));
  }

  public void runJobsForEntityGroup(String name, boolean isDryRun) {
    LOGGER.info("RUN entity group: {}", name);
    getJobsForEntityGroup(underlay.getEntityGroup(name))
        .forEach(ij -> tolerateJobExceptions(() -> ij.checkStatusAndRun(isDryRun), ij.getName()));
  }

  public void cleanAllEntities(boolean isDryRun) {
    underlay.getEntities().keySet().stream().sorted().forEach(name -> cleanEntity(name, isDryRun));
  }

  public void cleanEntity(String name, boolean isDryRun) {
    LOGGER.info("CLEAN entity: {}", name);
    getJobsForEntity(underlay.getEntity(name))
        .forEach(ij -> tolerateJobExceptions(() -> ij.checkStatusAndClean(isDryRun), ij.getName()));
  }

  public void cleanAllEntityGroups(boolean isDryRun) {
    underlay.getEntityGroups().keySet().stream()
        .sorted()
        .forEach(name -> cleanEntityGroup(name, isDryRun));
  }

  public void cleanEntityGroup(String name, boolean isDryRun) {
    LOGGER.info("CLEAN entity group: {}", name);
    getJobsForEntityGroup(underlay.getEntityGroup(name))
        .forEach(ij -> tolerateJobExceptions(() -> ij.checkStatusAndClean(isDryRun), ij.getName()));
  }

  @VisibleForTesting
  public static List<IndexingJob> getJobsForEntity(Entity entity) {
    List<IndexingJob> jobs = new ArrayList<>();
    jobs.add(new CreateEntityTable(entity));
    jobs.add(new DenormalizeEntityInstances(entity));
    if (entity.getTextSearch().isEnabled()) {
      jobs.add(new BuildTextSearchStrings(entity));
    }
    entity.getHierarchies().stream()
        .forEach(
            hierarchy -> {
              jobs.add(new WriteParentChildIdPairs(entity, hierarchy.getName()));
              jobs.add(new WriteAncestorDescendantIdPairs(entity, hierarchy.getName()));
              jobs.add(new BuildNumChildrenAndPaths(entity, hierarchy.getName()));
            });
    return jobs;
  }

  @VisibleForTesting
  public static List<IndexingJob> getJobsForEntityGroup(EntityGroup entityGroup) {
    List<IndexingJob> jobs = new ArrayList<>();

    // for each relationship, write the index relationship mapping
    entityGroup.getRelationships().values().stream()
        .forEach(
            // TODO: If the source relationship mapping table = one of the entity tables, then just
            // populate a new column on that entity table, instead of always writing a new table.
            relationship -> jobs.add(new WriteRelationshipIdPairs(relationship)));

    if (EntityGroup.Type.CRITERIA_OCCURRENCE.equals(entityGroup.getType())) {
      CriteriaOccurrence criteriaOccurrence = (CriteriaOccurrence) entityGroup;
      // Compute the criteria rollup counts for both the criteria-primary and criteria-occurrence
      // relationships.
      jobs.add(
          new ComputeRollupCounts(
              criteriaOccurrence.getCriteriaEntity(),
              criteriaOccurrence.getCriteriaPrimaryRelationship(),
              null));
      jobs.add(
          new ComputeRollupCounts(
              criteriaOccurrence.getCriteriaEntity(),
              criteriaOccurrence.getOccurrenceCriteriaRelationship(),
              null));

      // If the criteria entity has a hierarchy, then also compute the counts for each
      // hierarchy.
      if (criteriaOccurrence.getCriteriaEntity().hasHierarchies()) {
        criteriaOccurrence.getCriteriaEntity().getHierarchies().stream()
            .forEach(
                hierarchy -> {
                  jobs.add(
                      new ComputeRollupCounts(
                          criteriaOccurrence.getCriteriaEntity(),
                          criteriaOccurrence.getCriteriaPrimaryRelationship(),
                          hierarchy));
                  jobs.add(
                      new ComputeRollupCounts(
                          criteriaOccurrence.getCriteriaEntity(),
                          criteriaOccurrence.getOccurrenceCriteriaRelationship(),
                          hierarchy));
                });
      }

      // Compute display hints for the occurrence entity.
      if (!criteriaOccurrence.getModifierAttributes().isEmpty()) {
        jobs.add(
            new ComputeDisplayHints(
                criteriaOccurrence, criteriaOccurrence.getModifierAttributes()));
      }
    }

    return jobs;
  }

  public Underlay getUnderlay() {
    return underlay;
  }

  /**
   * Execute an indexing job. If an exception is thrown, make sure the error message and stack trace
   * are logged.
   *
   * @param runJob function with no return value
   * @param jobName name of the indexing job to include in log statements
   */
  private void tolerateJobExceptions(
      HttpUtils.RunnableWithCheckedException<Exception> runJob, String jobName) {
    try {
      runJob.run();
    } catch (Exception ex) {
      LOGGER.error("Error running indexing job: {}", jobName);
      LOGGER.error("Exception thrown: {}", ex);
    }
  }
}

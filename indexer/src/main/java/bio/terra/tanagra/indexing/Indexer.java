package bio.terra.tanagra.indexing;

import bio.terra.tanagra.exception.InvalidConfigException;
import bio.terra.tanagra.indexing.job.*;
import bio.terra.tanagra.indexing.jobexecutor.JobRunner;
import bio.terra.tanagra.indexing.jobexecutor.ParallelRunner;
import bio.terra.tanagra.indexing.jobexecutor.SequencedJobSet;
import bio.terra.tanagra.indexing.jobexecutor.SerialRunner;
import bio.terra.tanagra.query.Literal;
import bio.terra.tanagra.underlay.*;
import bio.terra.tanagra.underlay.entitygroup.CriteriaOccurrence;
import bio.terra.tanagra.underlay.entitygroup.GroupItems;
import com.google.common.annotations.VisibleForTesting;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Indexer {
  private static final Logger LOGGER = LoggerFactory.getLogger(Indexer.class);

  enum JobExecutor {
    PARALLEL,
    SERIAL;

    public JobRunner getRunner(
        List<SequencedJobSet> jobSets, boolean isDryRun, IndexingJob.RunType runType) {
      switch (this) {
        case SERIAL:
          return new SerialRunner(jobSets, isDryRun, runType);
        case PARALLEL:
          return new ParallelRunner(jobSets, isDryRun, runType);
        default:
          throw new IllegalArgumentException("Unknown JobExecution enum type: " + this);
      }
    }
  }

  private final Underlay underlay;

  private Indexer(Underlay underlay) {
    this.underlay = underlay;
  }

  /** Deserialize the POJOs to the internal objects and expand all defaults. */
  public static Indexer deserializeUnderlay(String underlayFileName) throws IOException {
    return new Indexer(Underlay.fromJSON(underlayFileName));
  }

  public void validateConfig() {
    // Check that the attribute data types are all defined and match the expected.
    Map<String, List<String>> errorsForEntity = new HashMap<>();
    underlay
        .getEntities()
        .values()
        .forEach(
            entity -> {
              List<String> errors = new ArrayList<>();
              entity.getAttributes().stream()
                  .forEach(
                      attribute -> {
                        Literal.DataType computedDataType =
                            attribute.getMapping(Underlay.MappingType.SOURCE).computeDataType();
                        if (attribute.getDataType() == null
                            || !attribute.getDataType().equals(computedDataType)) {
                          String msg =
                              "attribute: "
                                  + attribute.getName()
                                  + ", expected data type: "
                                  + computedDataType
                                  + ", actual data type: "
                                  + attribute.getDataType();
                          errors.add(msg);
                          LOGGER.debug("entity: {}, {}", entity.getName(), msg);
                        }
                      });
              if (!errors.isEmpty()) {
                errorsForEntity.put(entity.getName(), errors);
              }
            });

    // Output any error messages.
    if (errorsForEntity.isEmpty()) {
      LOGGER.info("Validation of attribute data types succeeded");
    } else {
      errorsForEntity.keySet().stream()
          .sorted()
          .forEach(
              entityName -> {
                LOGGER.warn("Validation of attribute data types for entity {} failed", entityName);
                errorsForEntity.get(entityName).stream().forEach(msg -> LOGGER.warn(msg));
              });
      throw new InvalidConfigException("Validation attribute data types had errors");
    }
  }

  public JobRunner runJobsForAllEntities(
      JobExecutor jobExecutor, boolean isDryRun, IndexingJob.RunType runType) {
    LOGGER.info("INDEXING all entities");
    List<SequencedJobSet> jobSets =
        underlay.getEntities().values().stream()
            .map(this::getJobSetForEntity)
            .collect(Collectors.toList());
    return runJobs(jobExecutor, isDryRun, runType, jobSets);
  }

  public JobRunner runJobsForSingleEntity(
      JobExecutor jobExecutor, boolean isDryRun, IndexingJob.RunType runType, String name) {
    LOGGER.info("INDEXING entity: {}", name);
    List<SequencedJobSet> jobSets = List.of(getJobSetForEntity(underlay.getEntity(name)));
    return runJobs(jobExecutor, isDryRun, runType, jobSets);
  }

  public JobRunner runJobsForAllEntityGroups(
      JobExecutor jobExecutor, boolean isDryRun, IndexingJob.RunType runType) {
    LOGGER.info("INDEXING all entity groups");
    List<SequencedJobSet> jobSets =
        underlay.getEntityGroups().values().stream()
            .map(Indexer::getJobSetForEntityGroup)
            .collect(Collectors.toList());
    return runJobs(jobExecutor, isDryRun, runType, jobSets);
  }

  public JobRunner runJobsForSingleEntityGroup(
      JobExecutor jobExecutor, boolean isDryRun, IndexingJob.RunType runType, String name) {
    LOGGER.info("INDEXING entity group: {}", name);
    List<SequencedJobSet> jobSets = List.of(getJobSetForEntityGroup(underlay.getEntityGroup(name)));
    return runJobs(jobExecutor, isDryRun, runType, jobSets);
  }

  private JobRunner runJobs(
      JobExecutor jobExecutor,
      boolean isDryRun,
      IndexingJob.RunType runType,
      List<SequencedJobSet> jobSets) {
    JobRunner jobRunner = jobExecutor.getRunner(jobSets, isDryRun, runType);
    jobRunner.runJobSets();
    return jobRunner;
  }

  @VisibleForTesting
  public SequencedJobSet getJobSetForEntity(Entity entity) {
    SequencedJobSet jobSet = new SequencedJobSet(entity.getName());
    jobSet.startNewStage();
    jobSet.addJob(new CreateEntityTable(entity));

    jobSet.startNewStage();
    jobSet.addJob(new DenormalizeEntityInstances(entity));
    jobSet.addJob(new ComputeEntityLevelDisplayHints(entity));

    if (entity.getTextSearch().isEnabled() || entity.hasHierarchies()) {
      jobSet.startNewStage();
    }

    if (entity.getTextSearch().isEnabled()) {
      jobSet.addJob(new BuildTextSearchStrings(entity));
    }
    entity.getHierarchies().stream()
        .forEach(
            hierarchy -> {
              jobSet.addJob(new WriteParentChildIdPairs(entity, hierarchy.getName()));
              jobSet.addJob(new WriteAncestorDescendantIdPairs(entity, hierarchy.getName()));
              jobSet.addJob(new BuildNumChildrenAndPaths(entity, hierarchy.getName()));
            });

    return jobSet;
  }

  @VisibleForTesting
  public static SequencedJobSet getJobSetForEntityGroup(EntityGroup entityGroup) {
    SequencedJobSet jobSet = new SequencedJobSet(entityGroup.getName());
    jobSet.startNewStage();

    // For each relationship, write the index relationship mapping.
    entityGroup.getRelationships().values().stream()
        .forEach(
            // TODO: If the source relationship mapping table = one of the entity tables, then just
            // populate a new column on that entity table, instead of always writing a new table.
            relationship -> jobSet.addJob(new WriteRelationshipIdPairs(relationship)));

    if (EntityGroup.Type.CRITERIA_OCCURRENCE.equals(entityGroup.getType())) {
      CriteriaOccurrence criteriaOccurrence = (CriteriaOccurrence) entityGroup;
      // Compute the criteria rollup counts for both the criteria-primary and criteria-occurrence
      // relationships.
      jobSet.addJob(
          new ComputeRollupCounts(
              criteriaOccurrence.getCriteriaEntity(),
              criteriaOccurrence.getCriteriaPrimaryRelationship(),
              null));
      // jobSet.addJob(
      //     new ComputeRollupCounts(
      //         criteriaOccurrence.getCriteriaEntity(),
      //         criteriaOccurrence.getOccurrenceCriteriaRelationship(),
      //         null));

      // If the criteria entity has a hierarchy, then also compute the counts for each
      // hierarchy.
      if (criteriaOccurrence.getCriteriaEntity().hasHierarchies()) {
        criteriaOccurrence.getCriteriaEntity().getHierarchies().stream()
            .forEach(
                hierarchy -> {
                  jobSet.addJob(
                      new ComputeRollupCounts(
                          criteriaOccurrence.getCriteriaEntity(),
                          criteriaOccurrence.getCriteriaPrimaryRelationship(),
                          hierarchy));
                  // TODO: Compute rollups for the occurrence-criteria relationship also. These
                  // workflows frequently fail in Dataflow, so they're commented out below. Either
                  // debug these or replace them with BigQuery-based jobs.
                  // jobSet.addJob(
                  //     new ComputeRollupCounts(
                  //         criteriaOccurrence.getCriteriaEntity(),
                  //         criteriaOccurrence.getOccurrenceCriteriaRelationship(),
                  //         hierarchy));
                });
      }

      // Compute display hints for the occurrence entity.
      if (!criteriaOccurrence.getModifierAttributes().isEmpty()) {
        jobSet.addJob(
            new ComputeDisplayHints(
                criteriaOccurrence, criteriaOccurrence.getModifierAttributes()));
      }
    } else if (EntityGroup.Type.GROUP_ITEMS.equals(entityGroup.getType())) {
      GroupItems groupItems = (GroupItems) entityGroup;
      // Compute the criteria rollup counts for the group-items relationship.
      jobSet.addJob(
          new ComputeRollupCounts(
              groupItems.getGroupEntity(), groupItems.getGroupItemsRelationship(), null));

      // If the group entity has a hierarchy, then also compute the counts for each hierarchy.
      if (groupItems.getGroupEntity().hasHierarchies()) {
        groupItems.getGroupEntity().getHierarchies().stream()
            .forEach(
                hierarchy ->
                    jobSet.addJob(
                        new ComputeRollupCounts(
                            groupItems.getGroupEntity(),
                            groupItems.getGroupItemsRelationship(),
                            hierarchy)));
      }
    }

    return jobSet;
  }

  public Underlay getUnderlay() {
    return underlay;
  }
}

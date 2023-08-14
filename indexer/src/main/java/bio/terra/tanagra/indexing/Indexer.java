package bio.terra.tanagra.indexing;

import bio.terra.tanagra.indexing.job.*;
import bio.terra.tanagra.indexing.jobexecutor.JobRunner;
import bio.terra.tanagra.indexing.jobexecutor.ParallelRunner;
import bio.terra.tanagra.indexing.jobexecutor.SequencedJobSet;
import bio.terra.tanagra.indexing.jobexecutor.SerialRunner;
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
    ValidationUtils.validateRelationships(underlay);
    ValidationUtils.validateAttributes(underlay);
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

    if (EntityGroup.Type.CRITERIA_OCCURRENCE.equals(entityGroup.getType())) {
      CriteriaOccurrence criteriaOccurrence = (CriteriaOccurrence) entityGroup;

      // Write the relationship id-pairs for the occurrence-criteria and occurrence-primary
      // relationships.
      // e.g. To allow joins between person-conditionOccurrence, conditionOccurrence-condition.
      jobSet.addJob(
          new WriteRelationshipIdPairs(criteriaOccurrence.getOccurrenceCriteriaRelationship()));
      jobSet.addJob(
          new WriteRelationshipIdPairs(criteriaOccurrence.getOccurrencePrimaryRelationship()));

      // Compute the criteria rollup counts for the criteria-primary relationship.
      // e.g. To show item counts for each condition.
      jobSet.addJob(
          new ComputeRollupCounts(
              criteriaOccurrence.getCriteriaEntity(),
              criteriaOccurrence.getCriteriaPrimaryRelationship(),
              null));

      // If the criteria entity has hierarchies, then also compute the criteria rollup counts for
      // each hierarchy.
      // e.g. To show rollup counts for each condition.
      if (criteriaOccurrence.getCriteriaEntity().hasHierarchies()) {
        criteriaOccurrence.getCriteriaEntity().getHierarchies().stream()
            .forEach(
                hierarchy -> {
                  jobSet.addJob(
                      new ComputeRollupCounts(
                          criteriaOccurrence.getCriteriaEntity(),
                          criteriaOccurrence.getCriteriaPrimaryRelationship(),
                          hierarchy));
                });
      }

      // Compute display hints for the occurrence entity attributes that are flagged as modifiers.
      // e.g. To show display hints for a specific measurement entity instance, such as blood
      // pressure.
      if (!criteriaOccurrence.getModifierAttributes().isEmpty()) {
        jobSet.addJob(new ComputeModifierDisplayHints(criteriaOccurrence));
      }
    } else if (EntityGroup.Type.GROUP_ITEMS.equals(entityGroup.getType())) {
      GroupItems groupItems = (GroupItems) entityGroup;

      // Write the relationship id-pairs for the group-items relationship.
      // e.g. To allow joins between brand-ingredient.
      jobSet.addJob(new WriteRelationshipIdPairs(groupItems.getGroupItemsRelationship()));

      // Compute the criteria rollup counts for the group-items relationship.
      // e.g. To show how many ingredients each brand contains.
      jobSet.addJob(
          new ComputeRollupCounts(
              groupItems.getGroupEntity(), groupItems.getGroupItemsRelationship(), null));
    }

    return jobSet;
  }

  public Underlay getUnderlay() {
    return underlay;
  }
}

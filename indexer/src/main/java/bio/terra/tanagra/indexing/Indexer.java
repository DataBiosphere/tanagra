package bio.terra.tanagra.indexing;

import bio.terra.tanagra.indexing.job.IndexingJob;
import bio.terra.tanagra.indexing.jobexecutor.JobRunner;
import bio.terra.tanagra.indexing.jobexecutor.ParallelRunner;
import bio.terra.tanagra.indexing.jobexecutor.SequencedJobSet;
import bio.terra.tanagra.indexing.jobexecutor.SerialRunner;
import bio.terra.tanagra.underlay2.Underlay;
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

  public Indexer(Underlay underlay) {
    this.underlay = underlay;
  }

  public void validateConfig() {
    ValidationUtils.validateRelationships(underlay);
    ValidationUtils.validateAttributes(underlay);
  }

  public JobRunner runJobsForAllEntities(
      JobExecutor jobExecutor, boolean isDryRun, IndexingJob.RunType runType) {
    LOGGER.info("INDEXING all entities");
    List<SequencedJobSet> jobSets =
        underlay.getEntities().stream()
            .map(JobSequencer::getJobSetForEntity)
            .collect(Collectors.toList());
    return runJobs(jobExecutor, isDryRun, runType, jobSets);
  }

  public JobRunner runJobsForSingleEntity(
      JobExecutor jobExecutor, boolean isDryRun, IndexingJob.RunType runType, String name) {
    LOGGER.info("INDEXING entity: {}", name);
    List<SequencedJobSet> jobSets =
        List.of(JobSequencer.getJobSetForEntity(underlay.getEntity(name)));
    return runJobs(jobExecutor, isDryRun, runType, jobSets);
  }

  public JobRunner runJobsForAllEntityGroups(
      JobExecutor jobExecutor, boolean isDryRun, IndexingJob.RunType runType) {
    LOGGER.info("INDEXING all entity groups");
    List<SequencedJobSet> jobSets =
        underlay.getEntityGroups().stream()
            .map(JobSequencer::getJobSetForEntityGroup)
            .collect(Collectors.toList());
    return runJobs(jobExecutor, isDryRun, runType, jobSets);
  }

  public JobRunner runJobsForSingleEntityGroup(
      JobExecutor jobExecutor, boolean isDryRun, IndexingJob.RunType runType, String name) {
    LOGGER.info("INDEXING entity group: {}", name);
    List<SequencedJobSet> jobSets =
        List.of(JobSequencer.getJobSetForEntityGroup(underlay.getEntityGroup(name)));
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

  public Underlay getUnderlay() {
    return underlay;
  }
}

package bio.terra.tanagra.indexing;

import bio.terra.tanagra.indexing.job.IndexingJob;
import bio.terra.tanagra.indexing.jobexecutor.JobRunner;
import bio.terra.tanagra.indexing.jobexecutor.SequencedJobSet;
import bio.terra.tanagra.underlay2.ConfigReader;
import bio.terra.tanagra.underlay2.Underlay;
import bio.terra.tanagra.underlay2.serialization.SZIndexer;
import bio.terra.tanagra.underlay2.serialization.SZUnderlay;
import java.util.*;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Indexer {
  private static final Logger LOGGER = LoggerFactory.getLogger(Indexer.class);
  private final SZIndexer szIndexer;
  private final Underlay underlay;

  private Indexer(SZIndexer szIndexer, Underlay underlay) {
    this.szIndexer = szIndexer;
    this.underlay = underlay;
  }

  public static Indexer fromConfig(SZIndexer szIndexer) {
    SZUnderlay szUnderlay = ConfigReader.deserializeUnderlay(szIndexer.underlay);
    Underlay underlay = Underlay.fromConfig(szIndexer.bigQuery, szUnderlay);
    return new Indexer(szIndexer, underlay);
  }

  public Underlay getUnderlay() {
    return underlay;
  }

  public JobRunner runJobsForAllEntities(
      JobSequencer.JobExecutor jobExecutor, boolean isDryRun, IndexingJob.RunType runType) {
    LOGGER.info("INDEXING all entities");
    List<SequencedJobSet> jobSets =
        underlay.getEntities().stream()
            .map(
                entity ->
                    JobSequencer.getJobSetForEntity(
                        szIndexer, underlay, underlay.getEntity(entity.getName())))
            .collect(Collectors.toList());
    return runJobs(jobExecutor, isDryRun, runType, jobSets);
  }

  public JobRunner runJobsForSingleEntity(
      JobSequencer.JobExecutor jobExecutor,
      boolean isDryRun,
      IndexingJob.RunType runType,
      String name) {
    LOGGER.info("INDEXING entity: {}", name);
    List<SequencedJobSet> jobSets =
        List.of(JobSequencer.getJobSetForEntity(szIndexer, underlay, underlay.getEntity(name)));
    return runJobs(jobExecutor, isDryRun, runType, jobSets);
  }

  public JobRunner runJobsForAllEntityGroups(
      JobSequencer.JobExecutor jobExecutor, boolean isDryRun, IndexingJob.RunType runType) {
    LOGGER.info("INDEXING all entity groups");
    List<SequencedJobSet> jobSets =
        underlay.getEntityGroups().stream()
            .map(
                entityGroup ->
                    JobSequencer.getJobSetForEntityGroup(szIndexer, underlay, entityGroup))
            .collect(Collectors.toList());
    return runJobs(jobExecutor, isDryRun, runType, jobSets);
  }

  public JobRunner runJobsForSingleEntityGroup(
      JobSequencer.JobExecutor jobExecutor,
      boolean isDryRun,
      IndexingJob.RunType runType,
      String name) {
    LOGGER.info("INDEXING entity group: {}", name);
    List<SequencedJobSet> jobSets =
        List.of(
            JobSequencer.getJobSetForEntityGroup(
                szIndexer, underlay, underlay.getEntityGroup(name)));
    return runJobs(jobExecutor, isDryRun, runType, jobSets);
  }

  private JobRunner runJobs(
      JobSequencer.JobExecutor jobExecutor,
      boolean isDryRun,
      IndexingJob.RunType runType,
      List<SequencedJobSet> jobSets) {
    JobRunner jobRunner = jobExecutor.getRunner(jobSets, isDryRun, runType);
    jobRunner.runJobSets();
    return jobRunner;
  }
}

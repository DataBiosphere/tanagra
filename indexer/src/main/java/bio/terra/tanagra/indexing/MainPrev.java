package bio.terra.tanagra.indexing;

import static bio.terra.tanagra.indexing.MainPrev.Command.INDEX_ALL;
import static bio.terra.tanagra.indexing.MainPrev.Command.INDEX_ENTITY;
import static bio.terra.tanagra.indexing.MainPrev.Command.INDEX_ENTITY_GROUP;

import bio.terra.tanagra.exception.SystemException;
import bio.terra.tanagra.indexing.job.IndexingJob;
import bio.terra.tanagra.indexing.jobexecutor.JobRunner;
import bio.terra.tanagra.underlay.ConfigReader;
import bio.terra.tanagra.underlay.serialization.SZIndexer;

public final class MainPrev {
  private MainPrev() {}

  enum Command {
    INDEX_ENTITY,
    INDEX_ENTITY_GROUP,
    INDEX_ALL,
    CLEAN_ENTITY,
    CLEAN_ENTITY_GROUP,
    CLEAN_ALL
  }

  public static void main(String... args) throws Exception {
    // TODO: Use a library for command parsing and package this as a proper CLI.
    Command cmd = Command.valueOf(args[0]);
    String indexerConfigName = args[1];

    // Read in the config files, convert to the internal underlay object.
    SZIndexer szIndexer = ConfigReader.deserializeIndexer(indexerConfigName);
    Indexer indexer = Indexer.fromConfig(szIndexer);

    switch (cmd) {
      case INDEX_ENTITY:
      case CLEAN_ENTITY:
        IndexingJob.RunType runTypeEntity =
            INDEX_ENTITY.equals(cmd) ? IndexingJob.RunType.RUN : IndexingJob.RunType.CLEAN;
        String nameEntity = args[2];
        boolean isAllEntities = "*".equals(nameEntity);
        boolean isDryRunEntity = isDryRun(3, args);
        JobSequencer.JobExecutor jobExecEntity = getJobExec(4, args);

        // Index/clean all the entities (*) or just one (entityName).
        JobRunner entityJobRunner;
        if (isAllEntities) {
          entityJobRunner =
              indexer.runJobsForAllEntities(jobExecEntity, isDryRunEntity, runTypeEntity);
        } else {
          entityJobRunner =
              indexer.runJobsForSingleEntity(
                  jobExecEntity, isDryRunEntity, runTypeEntity, nameEntity);
        }
        entityJobRunner.printJobResultSummary();
        entityJobRunner.throwIfAnyFailures();
        break;
      case INDEX_ENTITY_GROUP:
      case CLEAN_ENTITY_GROUP:
        IndexingJob.RunType runTypeEntityGroup =
            INDEX_ENTITY_GROUP.equals(cmd) ? IndexingJob.RunType.RUN : IndexingJob.RunType.CLEAN;
        String nameEntityGroup = args[2];
        boolean isAllEntityGroups = "*".equals(nameEntityGroup);
        boolean isDryRunEntityGroup = isDryRun(3, args);
        JobSequencer.JobExecutor jobExecEntityGroup = getJobExec(4, args);

        // Index/clean all the entity groups (*) or just one (entityGroupName).
        JobRunner entityGroupJobRunner;
        if (isAllEntityGroups) {
          entityGroupJobRunner =
              indexer.runJobsForAllEntityGroups(
                  jobExecEntityGroup, isDryRunEntityGroup, runTypeEntityGroup);
        } else {
          entityGroupJobRunner =
              indexer.runJobsForSingleEntityGroup(
                  jobExecEntityGroup, isDryRunEntityGroup, runTypeEntityGroup, nameEntityGroup);
        }
        entityGroupJobRunner.printJobResultSummary();
        entityGroupJobRunner.throwIfAnyFailures();
        break;
      case INDEX_ALL:
      case CLEAN_ALL:
        IndexingJob.RunType runTypeAll =
            INDEX_ALL.equals(cmd) ? IndexingJob.RunType.RUN : IndexingJob.RunType.CLEAN;
        boolean isDryRunAll = isDryRun(2, args);
        JobSequencer.JobExecutor jobExecAll = getJobExec(3, args);

        // Index/clean all the entities and entity groups.
        JobRunner entityJobRunnerAll =
            indexer.runJobsForAllEntities(jobExecAll, isDryRunAll, runTypeAll);
        JobRunner entityGroupJobRunnerAll =
            indexer.getUnderlay().getEntityGroups().isEmpty()
                ? null
                : indexer.runJobsForAllEntityGroups(jobExecAll, isDryRunAll, runTypeAll);
        entityJobRunnerAll.printJobResultSummary();
        if (entityGroupJobRunnerAll != null) {
          entityGroupJobRunnerAll.printJobResultSummary();
        }
        entityJobRunnerAll.throwIfAnyFailures();
        if (entityGroupJobRunnerAll != null) {
          entityGroupJobRunnerAll.throwIfAnyFailures();
        }
        break;
      default:
        throw new SystemException("Unknown command: " + cmd);
    }
  }

  private static boolean isDryRun(int index, String... args) {
    return args.length > index && "DRY_RUN".equals(args[index]);
  }

  private static JobSequencer.JobExecutor getJobExec(int index, String... args) {
    return args.length > index && "SERIAL".equals(args[index])
        ? JobSequencer.JobExecutor.SERIAL
        : JobSequencer.JobExecutor.PARALLEL;
  }
}

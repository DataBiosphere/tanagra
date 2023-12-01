package bio.terra.tanagra.indexing.cli.shared.command;

import bio.terra.tanagra.cli.command.BaseCommand;
import bio.terra.tanagra.cli.command.Format;
import bio.terra.tanagra.indexing.Indexer;
import bio.terra.tanagra.indexing.cli.shared.options.IndexerConfig;
import bio.terra.tanagra.indexing.cli.shared.options.JobExecutorAndDryRun;
import bio.terra.tanagra.indexing.job.IndexingJob;
import bio.terra.tanagra.indexing.jobexecutor.JobRunner;
import bio.terra.tanagra.underlay.ConfigReader;
import bio.terra.tanagra.underlay.serialization.SZIndexer;
import picocli.CommandLine;

public abstract class Underlay extends BaseCommand {
  private @CommandLine.Mixin Format formatOption;
  private @CommandLine.Mixin IndexerConfig indexerConfig;
  private @CommandLine.Mixin JobExecutorAndDryRun jobExecutorAndDryRun;

  protected abstract IndexingJob.RunType getRunType();

  /** Index/clean all entities and entity groups. */
  @Override
  protected void execute() {
    SZIndexer szIndexer = ConfigReader.deserializeIndexer(indexerConfig.name);
    Indexer indexer = Indexer.fromConfig(szIndexer);

    JobRunner entityJobRunnerAll =
        indexer.runJobsForAllEntities(
            jobExecutorAndDryRun.jobExecutor, jobExecutorAndDryRun.dryRun, getRunType());
    JobRunner entityGroupJobRunnerAll =
        indexer.getUnderlay().getEntityGroups().isEmpty()
            ? null
            : indexer.runJobsForAllEntityGroups(
                jobExecutorAndDryRun.jobExecutor, jobExecutorAndDryRun.dryRun, getRunType());

    entityJobRunnerAll.printJobResultSummary();
    if (entityGroupJobRunnerAll != null) {
      entityGroupJobRunnerAll.printJobResultSummary();
    }
    entityJobRunnerAll.throwIfAnyFailures();
    if (entityGroupJobRunnerAll != null) {
      entityGroupJobRunnerAll.throwIfAnyFailures();
    }
    formatOption.printReturnValue("done", this::printText);
  }

  /** Print this command's output in text format. */
  private void printText(String returnValue) {
    BaseCommand.OUT.println("All jobs status: " + returnValue);
  }
}

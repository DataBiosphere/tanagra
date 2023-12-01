package bio.terra.tanagra.indexing.cli.shared.command;

import bio.terra.tanagra.cli.command.BaseCommand;
import bio.terra.tanagra.cli.command.Format;
import bio.terra.tanagra.indexing.Indexer;
import bio.terra.tanagra.indexing.cli.shared.options.IndexerConfig;
import bio.terra.tanagra.indexing.cli.shared.options.JobExecutorAndDryRun;
import bio.terra.tanagra.indexing.cli.shared.options.OneOrAllEntityGroups;
import bio.terra.tanagra.indexing.job.IndexingJob;
import bio.terra.tanagra.indexing.jobexecutor.JobRunner;
import bio.terra.tanagra.underlay.ConfigReader;
import bio.terra.tanagra.underlay.serialization.SZIndexer;
import picocli.CommandLine;

public abstract class EntityGroup extends BaseCommand {
  private @CommandLine.Mixin Format formatOption;
  private @CommandLine.Mixin IndexerConfig indexerConfig;
  private @CommandLine.Mixin JobExecutorAndDryRun jobExecutorAndDryRun;
  private @CommandLine.Mixin OneOrAllEntityGroups oneOrAllEntityGroups;

  protected abstract IndexingJob.RunType getRunType();

  /** Index/clean one or all entity groups. */
  @Override
  protected void execute() {
    oneOrAllEntityGroups.validate();
    SZIndexer szIndexer = ConfigReader.deserializeIndexer(indexerConfig.name);
    Indexer indexer = Indexer.fromConfig(szIndexer);

    JobRunner entityJobRunner;
    if (oneOrAllEntityGroups.allEntityGroups) {
      entityJobRunner =
          indexer.runJobsForAllEntityGroups(
              jobExecutorAndDryRun.jobExecutor, jobExecutorAndDryRun.dryRun, getRunType());
    } else {
      entityJobRunner =
          indexer.runJobsForSingleEntityGroup(
              jobExecutorAndDryRun.jobExecutor,
              jobExecutorAndDryRun.dryRun,
              getRunType(),
              oneOrAllEntityGroups.entityGroup);
    }

    entityJobRunner.printJobResultSummary();
    entityJobRunner.throwIfAnyFailures();
    formatOption.printReturnValue("done", this::printText);
  }

  /** Print this command's output in text format. */
  private void printText(String returnValue) {
    BaseCommand.OUT.println("Entity jobs status: " + returnValue);
  }
}

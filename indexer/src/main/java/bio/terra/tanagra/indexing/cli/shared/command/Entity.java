package bio.terra.tanagra.indexing.cli.shared.command;

import bio.terra.tanagra.cli.command.BaseCommand;
import bio.terra.tanagra.cli.command.Format;
import bio.terra.tanagra.indexing.Indexer;
import bio.terra.tanagra.indexing.cli.shared.options.IndexerConfig;
import bio.terra.tanagra.indexing.cli.shared.options.JobExecutorAndDryRun;
import bio.terra.tanagra.indexing.cli.shared.options.OneOrAllEntities;
import bio.terra.tanagra.indexing.job.IndexingJob;
import bio.terra.tanagra.indexing.jobexecutor.JobRunner;
import bio.terra.tanagra.indexing.jobresultwriter.SysOutWriter;
import bio.terra.tanagra.underlay.ConfigReader;
import bio.terra.tanagra.underlay.serialization.SZIndexer;
import picocli.CommandLine;

public abstract class Entity extends BaseCommand {
  private @CommandLine.Mixin Format formatOption;
  private @CommandLine.Mixin IndexerConfig indexerConfig;
  private @CommandLine.Mixin JobExecutorAndDryRun jobExecutorAndDryRun;
  private @CommandLine.Mixin OneOrAllEntities oneOrAllEntities;

  protected abstract IndexingJob.RunType getRunType();

  /** Index/clean one or all entities. */
  @Override
  protected void execute() {
    oneOrAllEntities.validate();
    SZIndexer szIndexer = ConfigReader.deserializeIndexer(indexerConfig.name);
    Indexer indexer = Indexer.fromConfig(szIndexer);

    JobRunner jobRunner;
    if (oneOrAllEntities.allEntities) {
      jobRunner =
          indexer.runJobsForAllEntities(
              jobExecutorAndDryRun.jobExecutor, jobExecutorAndDryRun.dryRun, getRunType());
    } else {
      jobRunner =
          indexer.runJobsForSingleEntity(
              jobExecutorAndDryRun.jobExecutor,
              jobExecutorAndDryRun.dryRun,
              getRunType(),
              oneOrAllEntities.entity);
    }

    new SysOutWriter(jobRunner).run();
    jobRunner.throwIfAnyFailures();
    formatOption.printReturnValue("done", this::printText);
  }

  /** Print this command's output in text format. */
  private void printText(String returnValue) {
    BaseCommand.OUT.println("Entity jobs status: " + returnValue);
  }
}

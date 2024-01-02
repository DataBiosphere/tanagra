package bio.terra.tanagra.indexing.cli.shared.command;

import bio.terra.tanagra.cli.BaseMain;
import bio.terra.tanagra.cli.command.BaseCommand;
import bio.terra.tanagra.indexing.JobSequencer;
import bio.terra.tanagra.indexing.cli.shared.options.EntityNames;
import bio.terra.tanagra.indexing.cli.shared.options.IndexerConfig;
import bio.terra.tanagra.indexing.cli.shared.options.JobExecutorAndDryRun;
import bio.terra.tanagra.indexing.job.IndexingJob;
import bio.terra.tanagra.indexing.jobexecutor.JobRunner;
import bio.terra.tanagra.indexing.jobexecutor.SequencedJobSet;
import bio.terra.tanagra.indexing.jobresultwriter.HtmlWriter;
import bio.terra.tanagra.underlay.ConfigReader;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.serialization.SZIndexer;
import bio.terra.tanagra.underlay.serialization.SZUnderlay;
import java.util.List;
import java.util.stream.Collectors;
import picocli.CommandLine;

public abstract class Entity extends BaseCommand {
  private @CommandLine.Mixin IndexerConfig indexerConfig;
  private @CommandLine.Mixin JobExecutorAndDryRun jobExecutorAndDryRun;
  private @CommandLine.Mixin EntityNames entityNames;

  protected abstract IndexingJob.RunType getRunType();

  /** Index/clean one or all entities. */
  @Override
  protected void execute() {
    entityNames.validate();
    ConfigReader configReader = ConfigReader.fromDiskFile(indexerConfig.getGitHubDirWithDefault());
    SZIndexer szIndexer = configReader.readIndexer(indexerConfig.name);
    SZUnderlay szUnderlay = configReader.readUnderlay(szIndexer.underlay);
    Underlay underlay = Underlay.fromConfig(szIndexer.bigQuery, szUnderlay, configReader);

    List<bio.terra.tanagra.underlay.entitymodel.Entity> entities =
        entityNames.allEntities
            ? underlay.getEntities()
            : entityNames.names.stream().map(underlay::getEntity).collect(Collectors.toList());
    List<SequencedJobSet> jobSets =
        entities.stream()
            .map(entity -> JobSequencer.getJobSetForEntity(szIndexer, underlay, entity))
            .collect(Collectors.toList());
    JobRunner jobRunner =
        jobExecutorAndDryRun.jobExecutor.getRunner(
            jobSets, jobExecutorAndDryRun.dryRun, getRunType());
    jobRunner.runJobSets();

    new HtmlWriter(
            BaseMain.getArgList(),
            jobRunner.getJobResults(),
            jobExecutorAndDryRun.jobExecutor.name(),
            OUT,
            ERR,
            jobExecutorAndDryRun.getOutputDirWithDefault())
        .run();
  }
}

package bio.terra.tanagra.indexing.cli.shared.command;

import bio.terra.tanagra.cli.BaseMain;
import bio.terra.tanagra.cli.command.BaseCommand;
import bio.terra.tanagra.indexing.JobSequencer;
import bio.terra.tanagra.indexing.cli.shared.options.IndexerConfig;
import bio.terra.tanagra.indexing.cli.shared.options.JobExecutorAndDryRun;
import bio.terra.tanagra.indexing.cli.shared.options.OneOrAllEntityGroups;
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

public abstract class EntityGroup extends BaseCommand {
  private @CommandLine.Mixin IndexerConfig indexerConfig;
  private @CommandLine.Mixin JobExecutorAndDryRun jobExecutorAndDryRun;
  private @CommandLine.Mixin OneOrAllEntityGroups oneOrAllEntityGroups;

  protected abstract IndexingJob.RunType getRunType();

  /** Index/clean one or all entity groups. */
  @Override
  protected void execute() {
    oneOrAllEntityGroups.validate();
    ConfigReader configReader = ConfigReader.fromDiskFile(indexerConfig.getGitHubDirWithDefault());
    SZIndexer szIndexer = configReader.readIndexer(indexerConfig.name);
    SZUnderlay szUnderlay = configReader.readUnderlay(szIndexer.underlay);
    Underlay underlay = Underlay.fromConfig(szIndexer.bigQuery, szUnderlay, configReader);

    List<SequencedJobSet> jobSets =
        oneOrAllEntityGroups.allEntityGroups
            ? underlay.getEntityGroups().stream()
                .map(
                    entityGroup ->
                        JobSequencer.getJobSetForEntityGroup(
                            szIndexer, underlay, underlay.getEntityGroup(entityGroup.getName())))
                .collect(Collectors.toList())
            : List.of(
                JobSequencer.getJobSetForEntityGroup(
                    szIndexer,
                    underlay,
                    underlay.getEntityGroup(oneOrAllEntityGroups.entityGroup)));
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

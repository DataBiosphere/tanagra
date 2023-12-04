package bio.terra.tanagra.indexing.cli.shared.command;

import bio.terra.tanagra.cli.command.BaseCommand;
import bio.terra.tanagra.indexing.JobSequencer;
import bio.terra.tanagra.indexing.cli.shared.options.IndexerConfig;
import bio.terra.tanagra.indexing.cli.shared.options.JobExecutorAndDryRun;
import bio.terra.tanagra.indexing.cli.shared.options.OneOrAllEntities;
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
  private @CommandLine.Mixin OneOrAllEntities oneOrAllEntities;

  protected abstract IndexingJob.RunType getRunType();

  /** Index/clean one or all entities. */
  @Override
  protected void execute() {
    oneOrAllEntities.validate();
    SZIndexer szIndexer = ConfigReader.deserializeIndexer(indexerConfig.name);
    SZUnderlay szUnderlay = ConfigReader.deserializeUnderlay(szIndexer.underlay);
    Underlay underlay = Underlay.fromConfig(szIndexer.bigQuery, szUnderlay);

    List<SequencedJobSet> jobSets =
        oneOrAllEntities.allEntities
            ? underlay.getEntities().stream()
                .map(
                    entity ->
                        JobSequencer.getJobSetForEntity(
                            szIndexer, underlay, underlay.getEntity(entity.getName())))
                .collect(Collectors.toList())
            : List.of(
                JobSequencer.getJobSetForEntity(
                    szIndexer, underlay, underlay.getEntity(oneOrAllEntities.entity)));
    JobRunner jobRunner =
        jobExecutorAndDryRun.jobExecutor.getRunner(
            jobSets, jobExecutorAndDryRun.dryRun, getRunType());
    jobRunner.runJobSets();

    new HtmlWriter(
            jobRunner.getJobResults(),
            jobExecutorAndDryRun.jobExecutor.name(),
            OUT,
            ERR,
            jobExecutorAndDryRun.outputDir)
        .run();
  }
}

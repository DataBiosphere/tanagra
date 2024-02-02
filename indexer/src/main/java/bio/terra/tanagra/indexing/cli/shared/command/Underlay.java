package bio.terra.tanagra.indexing.cli.shared.command;

import bio.terra.tanagra.cli.BaseMain;
import bio.terra.tanagra.cli.command.BaseCommand;
import bio.terra.tanagra.indexing.JobSequencer;
import bio.terra.tanagra.indexing.cli.shared.options.IndexerConfig;
import bio.terra.tanagra.indexing.cli.shared.options.JobExecutorAndDryRun;
import bio.terra.tanagra.indexing.cli.shared.options.JobFilter;
import bio.terra.tanagra.indexing.job.IndexingJob;
import bio.terra.tanagra.indexing.jobexecutor.JobResult;
import bio.terra.tanagra.indexing.jobexecutor.JobRunner;
import bio.terra.tanagra.indexing.jobexecutor.SequencedJobSet;
import bio.terra.tanagra.indexing.jobresultwriter.HtmlWriter;
import bio.terra.tanagra.underlay.ConfigReader;
import bio.terra.tanagra.underlay.serialization.SZIndexer;
import bio.terra.tanagra.underlay.serialization.SZUnderlay;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import picocli.CommandLine;

public abstract class Underlay extends BaseCommand {
  private @CommandLine.Mixin IndexerConfig indexerConfig;
  private @CommandLine.Mixin JobExecutorAndDryRun jobExecutorAndDryRun;
  private @CommandLine.Mixin JobFilter jobFilter;

  protected abstract IndexingJob.RunType getRunType();

  /** Index/clean all entities and entity groups. */
  @Override
  protected void execute() {
    jobFilter.validate();
    ConfigReader configReader = ConfigReader.fromDiskFile(indexerConfig.getGitHubDirWithDefault());
    SZIndexer szIndexer = configReader.readIndexer(indexerConfig.name);
    SZUnderlay szUnderlay = configReader.readUnderlay(szIndexer.underlay);
    bio.terra.tanagra.underlay.Underlay underlay =
        bio.terra.tanagra.underlay.Underlay.fromConfig(
            szIndexer.bigQuery, szUnderlay, configReader);

    List<SequencedJobSet> entityJobSets =
        underlay.getEntities().stream()
            .map(
                entity ->
                    JobSequencer.getJobSetForEntity(
                            szIndexer, underlay, underlay.getEntity(entity.getName()))
                        .filterJobs(jobFilter.getClassNamesWithPackage()))
            .collect(Collectors.toList());
    JobRunner entityJobRunner =
        jobExecutorAndDryRun.jobExecutor.getRunner(
            entityJobSets, jobExecutorAndDryRun.dryRun, getRunType());
    entityJobRunner.runJobSets();

    List<SequencedJobSet> entityGroupJobSets =
        underlay.getEntityGroups().stream()
            .map(
                entityGroup ->
                    JobSequencer.getJobSetForEntityGroup(
                            szIndexer, underlay, underlay.getEntityGroup(entityGroup.getName()))
                        .filterJobs(jobFilter.getClassNamesWithPackage()))
            .collect(Collectors.toList());
    JobRunner entityGroupJobRunner =
        jobExecutorAndDryRun.jobExecutor.getRunner(
            entityGroupJobSets, jobExecutorAndDryRun.dryRun, getRunType());
    entityGroupJobRunner.runJobSets();

    List<JobResult> allResults = new ArrayList<>();
    allResults.addAll(entityJobRunner.getJobResults());
    allResults.addAll(entityGroupJobRunner.getJobResults());

    new HtmlWriter(
            BaseMain.getArgList(),
            allResults,
            entityJobRunner.getElapsedTimeNS() + entityGroupJobRunner.getElapsedTimeNS(),
            jobExecutorAndDryRun.jobExecutor.name(),
            OUT,
            ERR,
            jobExecutorAndDryRun.getOutputDirWithDefault())
        .run();
  }
}

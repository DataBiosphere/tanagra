package bio.terra.tanagra.indexing.cli.shared.options;

import bio.terra.tanagra.indexing.JobSequencer;
import picocli.CommandLine;

/**
 * Command helper class that defines the job executor and dry run options.
 *
 * <p>This class is meant to be used as a @CommandLine.Mixin.
 */
public class JobExecutorAndDryRun {
  @CommandLine.Option(
      names = "--job-executor",
      description =
          "Executor to use when running jobs. Recommend serial for debugging, parallel otherwise.")
  public JobSequencer.JobExecutor jobExecutor = JobSequencer.JobExecutor.PARALLEL;

  @CommandLine.Option(
      names = "--dry-run",
      description =
          "Do a dry run. No indexing tables will be written and no Dataflow jobs will be kicked off.")
  public boolean dryRun;
}

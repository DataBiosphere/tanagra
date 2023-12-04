package bio.terra.tanagra.indexing.jobresultwriter;

import bio.terra.tanagra.indexing.jobexecutor.JobRunner;

public abstract class JobResultWriter {
  protected final JobRunner jobRunner;

  protected JobResultWriter(JobRunner jobRunner) {
    this.jobRunner = jobRunner;
  }

  public abstract void run();
}

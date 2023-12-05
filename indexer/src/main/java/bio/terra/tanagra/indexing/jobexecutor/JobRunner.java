package bio.terra.tanagra.indexing.jobexecutor;

import bio.terra.tanagra.indexing.job.IndexingJob;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public abstract class JobRunner {
  protected static final long MAX_TIME_PER_JOB_MIN = 60;
  protected static final long MAX_TIME_PER_JOB_DRY_RUN_MIN = 5;

  protected final List<SequencedJobSet> jobSets;
  protected final boolean isDryRun;
  protected final IndexingJob.RunType runType;
  protected final List<JobResult> jobResults;

  public JobRunner(List<SequencedJobSet> jobSets, boolean isDryRun, IndexingJob.RunType runType) {
    this.jobSets = jobSets;
    this.isDryRun = isDryRun;
    this.runType = runType;
    this.jobResults = new ArrayList<>();
  }

  /** Name for display only. */
  public abstract String getName();

  /** Run all job sets. */
  public abstract void runJobSets();

  /** Run a single job set. */
  protected abstract void runSingleJobSet(SequencedJobSet sequencedJobSet)
      throws InterruptedException, ExecutionException;

  public List<JobResult> getJobResults() {
    return jobResults;
  }
}

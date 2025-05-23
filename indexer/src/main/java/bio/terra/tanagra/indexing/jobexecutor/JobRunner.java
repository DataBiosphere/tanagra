package bio.terra.tanagra.indexing.jobexecutor;

import bio.terra.tanagra.indexing.job.IndexingJob;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public abstract class JobRunner {
  protected static final long MAX_TIME_PER_JOB_MIN = 180;
  protected static final long MAX_TIME_PER_JOB_DRY_RUN_MIN = 10;
  protected final List<SequencedJobSet> jobSets;
  protected final boolean isDryRun;
  protected final IndexingJob.RunType runType;
  protected final List<JobResult> jobResults;
  private long elapsedTimeNS;

  public JobRunner(List<SequencedJobSet> jobSets, boolean isDryRun, IndexingJob.RunType runType) {
    this.jobSets = jobSets;
    this.isDryRun = isDryRun;
    this.runType = runType;
    this.jobResults = new ArrayList<>();
  }

  /** Name for display only. */
  public abstract String getName();

  /** Run all job sets. */
  protected abstract void runJobSetsWithoutTimer();

  /** Run a single job set. */
  protected abstract void runSingleJobSet(SequencedJobSet sequencedJobSet)
      throws InterruptedException, ExecutionException;

  /** Run all job sets. */
  public void runJobSets() {
    Instant startTime = Instant.now();
    runJobSetsWithoutTimer();
    elapsedTimeNS = startTime.until(Instant.now(), ChronoUnit.NANOS);
  }

  public List<JobResult> getJobResults() {
    return jobResults;
  }

  public long getElapsedTimeNS() {
    return elapsedTimeNS;
  }
}

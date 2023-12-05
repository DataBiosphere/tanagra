package bio.terra.tanagra.indexing.jobexecutor;

import bio.terra.tanagra.indexing.job.IndexingJob;
import java.util.concurrent.Callable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Thread that runs a single indexing job and outputs an instance of the result class. */
public class JobThread implements Callable<JobResult> {
  private static final Logger LOGGER = LoggerFactory.getLogger(JobThread.class);
  private final IndexingJob indexingJob;
  private final boolean isDryRun;
  private final IndexingJob.RunType runType;
  private final String jobName;

  public JobThread(
      IndexingJob indexingJob, boolean isDryRun, IndexingJob.RunType runType, String jobName) {
    this.indexingJob = indexingJob;
    this.isDryRun = isDryRun;
    this.runType = runType;
    this.jobName = jobName;
  }

  @Override
  public JobResult call() {
    JobResult result =
        new JobResult(
            jobName,
            Thread.currentThread().getName(),
            indexingJob.getEntity(),
            indexingJob.getEntityGroup());

    long startTime = System.nanoTime();
    try {
      IndexingJob.JobStatus status = execute();
      result.setJobStatus(status);
      result.setJobStatusAsExpected(
          indexingJob.checkStatusAfterRunMatchesExpected(runType, isDryRun, status));
      result.setExceptionWasThrown(false);
    } catch (Throwable ex) {
      result.saveExceptionThrown(ex);
    }
    result.setElapsedTimeNS(System.nanoTime() - startTime);

    return result;
  }

  private IndexingJob.JobStatus execute() {
    LOGGER.info("Executing indexing job: {}, {}", runType, indexingJob.getName());
    IndexingJob.JobStatus status = indexingJob.checkStatus();
    LOGGER.info("Job status: {}", status);

    switch (runType) {
      case RUN:
        if (!IndexingJob.JobStatus.NOT_STARTED.equals(status)) {
          LOGGER.info("Skipping because job is either in progress or complete");
          return status;
        }
        indexingJob.run(isDryRun);
        return indexingJob.checkStatus();
      case CLEAN:
        if (IndexingJob.JobStatus.IN_PROGRESS.equals(status)) {
          LOGGER.info("Skipping because job is in progress");
          return status;
        }
        indexingJob.clean(isDryRun);
        return indexingJob.checkStatus();
      case STATUS:
        return status;
      default:
        throw new IllegalArgumentException("Unknown execution type: " + runType);
    }
  }
}

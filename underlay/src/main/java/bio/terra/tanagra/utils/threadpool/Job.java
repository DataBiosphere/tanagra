package bio.terra.tanagra.utils.threadpool;

import java.util.concurrent.Callable;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Job<T> implements Callable<JobResult<T>> {
  private static final Logger LOGGER = LoggerFactory.getLogger(Job.class);
  private final String jobId;
  private final Supplier<T> jobFn;

  public Job(String jobId, Supplier<T> jobFn) {
    this.jobId = jobId;
    this.jobFn = jobFn;
  }

  public String getJobId() {
    return jobId;
  }

  @Override
  public JobResult call() {
    JobResult jobResult = new JobResult(jobId, Thread.currentThread().getName());

    long startTime = System.nanoTime();
    try {
      T jobOutput = jobFn.get();
      jobResult.setJobStatus(JobResult.Status.COMPLETED);
      jobResult.setJobOutput(jobOutput);
    } catch (Throwable ex) {
      jobResult.setJobStatus(JobResult.Status.FAILED);
      jobResult.saveExceptionThrown(ex);
      LOGGER.error("Job thread threw error", ex); // Print the stack trace to stdout/logs.
    }
    jobResult.setElapsedTimeNS(System.nanoTime() - startTime);

    return jobResult;
  }
}

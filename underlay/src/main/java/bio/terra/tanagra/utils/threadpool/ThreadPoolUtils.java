package bio.terra.tanagra.utils.threadpool;

import bio.terra.tanagra.exception.SystemException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ThreadPoolUtils {
  private static final Logger LOGGER = LoggerFactory.getLogger(ThreadPoolUtils.class);
  private static final long MAX_TIME_PER_JOB_MIN = 60;
  private static final long MAX_TIME_FOR_SHUTDOWN_SEC = 60;

  private ThreadPoolUtils() {}

  public static <T> Set<JobResult<T>> runInParallel(int numThreads, Set<Job<T>> jobs) {
    // Create a thread pool with a fixed number of threads.
    ThreadPoolExecutor threadPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(numThreads);
    LOGGER.info("Created pool with {} threads", numThreads);

    // Kick off each job in a separate thread.
    List<Future<JobResult<T>>> jobFutures = new ArrayList<>();
    for (Job<T> job : jobs) {
      LOGGER.info("Kicking off thread for job: {}", job.getJobId());
      Future<JobResult<T>> jobFuture = threadPool.submit(job);
      jobFutures.add(jobFuture);
    }

    try {
      LOGGER.info("Waiting for thread pool to shutdown.");
      shutdownThreadPool(threadPool, MAX_TIME_PER_JOB_MIN, TimeUnit.MINUTES);

      // Compile the results.
      Set<JobResult<T>> jobResults = new HashSet<>();
      for (Future<JobResult<T>> jobFuture : jobFutures) {
        JobResult<T> jobResult = jobFuture.get();
        jobResults.add(jobResult);
      }
      return jobResults;
    } catch (InterruptedException | ExecutionException intEx) {
      LOGGER.error("Error running jobs in parallel.");
      throw new SystemException("Error running jobs in parallel", intEx);
    }
  }

  /**
   * Tell a thread pool to stop accepting new jobs, wait for the existing jobs to finish. If the
   * jobs time out, then interrupt the threads and force them to terminate.
   */
  private static void shutdownThreadPool(
      ThreadPoolExecutor threadPool, long timeout, TimeUnit timeUnit) throws InterruptedException {
    // Wait for all threads to finish.
    threadPool.shutdown();
    boolean terminatedByItself = threadPool.awaitTermination(timeout, timeUnit);

    // If the threads didn't finish in the expected time, then send them interrupts.
    if (!terminatedByItself) {
      threadPool.shutdownNow();
    }
    if (!threadPool.awaitTermination(MAX_TIME_FOR_SHUTDOWN_SEC, TimeUnit.SECONDS)) {
      LOGGER.error("Thread pool failed to shutdown");
    }
  }
}

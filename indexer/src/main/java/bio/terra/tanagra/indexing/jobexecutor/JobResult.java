package bio.terra.tanagra.indexing.jobexecutor;

import bio.terra.tanagra.indexing.job.IndexingJob;
import jakarta.annotation.Nullable;
import java.io.PrintWriter;
import java.io.StringWriter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Output of a thread that runs a single indexing job. */
public class JobResult {
  private static final Logger LOGGER = LoggerFactory.getLogger(JobResult.class);

  private final String jobName;
  private final String threadName;
  @Nullable private final String entity;
  @Nullable private final String entityGroup;

  private IndexingJob.JobStatus jobStatus;
  private boolean threadTerminatedOnTime;
  private boolean jobStatusAsExpected;
  private long elapsedTimeNS;

  private boolean exceptionWasThrown;
  private String exceptionStackTrace;
  private String exceptionMessage;

  public JobResult(
      String jobName, String threadName, @Nullable String entity, @Nullable String entityGroup) {
    this.jobName = jobName;
    this.threadName = threadName;
    this.entity = entity;
    this.entityGroup = entityGroup;

    this.threadTerminatedOnTime = false;
    this.jobStatusAsExpected = false;
    this.exceptionWasThrown = false;
    this.exceptionStackTrace = null;
    this.exceptionMessage = null;
  }

  /**
   * Store the exception message and stack trace for the job results. Don't store the full {@link
   * Throwable} object, because that may not always be serializable. This class may be serialized to
   * disk as part of writing out the job results, so it needs to be a POJO.
   */
  public void saveExceptionThrown(Throwable exceptionThrown) {
    exceptionWasThrown = true;
    exceptionMessage =
        StringUtils.isBlank(exceptionMessage)
            ? exceptionThrown.getMessage()
            : String.format("%s%n%s", exceptionMessage, exceptionThrown.getMessage());

    StringWriter stackTraceStr = new StringWriter();
    exceptionThrown.printStackTrace(new PrintWriter(stackTraceStr));
    exceptionStackTrace = stackTraceStr.toString();

    LOGGER.error("Job thread threw error", exceptionThrown); // print the stack trace to the console
  }

  public boolean isFailure() {
    return exceptionWasThrown || !threadTerminatedOnTime || !jobStatusAsExpected;
  }

  public String getJobName() {
    return jobName;
  }

  public String getThreadName() {
    return threadName;
  }

  @Nullable
  public String getEntity() {
    return entity;
  }

  @Nullable
  public String getEntityGroup() {
    return entityGroup;
  }

  public IndexingJob.JobStatus getJobStatus() {
    return jobStatus;
  }

  public void setJobStatus(IndexingJob.JobStatus jobStatus) {
    this.jobStatus = jobStatus;
  }

  public boolean isThreadTerminatedOnTime() {
    return threadTerminatedOnTime;
  }

  public void setThreadTerminatedOnTime(boolean threadTerminatedOnTime) {
    this.threadTerminatedOnTime = threadTerminatedOnTime;
  }

  public boolean isJobStatusAsExpected() {
    return jobStatusAsExpected;
  }

  public void setJobStatusAsExpected(boolean jobStatusAsExpected) {
    this.jobStatusAsExpected = jobStatusAsExpected;
  }

  public long getElapsedTimeNS() {
    return elapsedTimeNS;
  }

  public void setElapsedTimeNS(long elapsedTimeNS) {
    this.elapsedTimeNS = elapsedTimeNS;
  }

  public boolean isExceptionWasThrown() {
    return exceptionWasThrown;
  }

  public void setExceptionWasThrown(boolean exceptionWasThrown) {
    this.exceptionWasThrown = exceptionWasThrown;
  }

  public String getExceptionStackTrace() {
    return exceptionStackTrace;
  }

  public String getExceptionMessage() {
    return exceptionMessage;
  }
}

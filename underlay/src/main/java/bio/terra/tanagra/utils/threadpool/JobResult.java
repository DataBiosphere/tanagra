package bio.terra.tanagra.utils.threadpool;

import java.io.PrintWriter;
import java.io.StringWriter;
import org.apache.commons.lang3.StringUtils;

public class JobResult<T> {
  public enum Status {
    COMPLETED,
    FAILED
  }

  private final String jobId;
  private final String threadName;
  private T jobOutput;
  private Status jobStatus;
  private boolean jobForceTerminated;
  private long elapsedTimeNS;

  private boolean exceptionWasThrown;
  private String exceptionMessage;
  private String exceptionStackTrace;

  public JobResult(String jobId, String threadName) {
    this.jobId = jobId;
    this.threadName = threadName;
  }

  public String getJobId() {
    return jobId;
  }

  public String getThreadName() {
    return threadName;
  }

  public T getJobOutput() {
    return jobOutput;
  }

  public JobResult<T> setJobOutput(T jobOutput) {
    this.jobOutput = jobOutput;
    return this;
  }

  public Status getJobStatus() {
    return jobStatus;
  }

  public JobResult setJobStatus(Status jobStatus) {
    this.jobStatus = jobStatus;
    return this;
  }

  public boolean isJobForceTerminated() {
    return jobForceTerminated;
  }

  public JobResult setJobForceTerminated(boolean jobForceTerminated) {
    this.jobForceTerminated = jobForceTerminated;
    return this;
  }

  public long getElapsedTimeNS() {
    return elapsedTimeNS;
  }

  public JobResult setElapsedTimeNS(long elapsedTimeNS) {
    this.elapsedTimeNS = elapsedTimeNS;
    return this;
  }

  public boolean isExceptionWasThrown() {
    return exceptionWasThrown;
  }

  public String getExceptionMessage() {
    return exceptionMessage;
  }

  public String getExceptionStackTrace() {
    return exceptionStackTrace;
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
  }
}

package bio.terra.tanagra.indexing.jobresultwriter;

import bio.terra.tanagra.indexing.jobexecutor.JobResult;
import bio.terra.tanagra.indexing.jobexecutor.JobRunner;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("PMD.SystemPrintln")
public class SysOutWriter extends JobResultWriter {
  private static final String TERMINAL_ESCAPE_RESET = "\u001B[0m";
  private static final String TERMINAL_ANSI_PURPLE = "\u001B[35m";
  private static final String TERMINAL_ANSI_GREEN = "\u001b[32m";
  private static final String TERMINAL_ANSI_RED = "\u001b[31m";

  public SysOutWriter(JobRunner jobRunner) {
    super(jobRunner);
  }

  @Override
  public void run() {
    System.out.println(System.lineSeparator());
    System.out.println(
        TERMINAL_ANSI_PURPLE
            + "Indexing job summary ("
            + jobRunner.getName()
            + ")"
            + TERMINAL_ESCAPE_RESET);
    jobRunner.getJobResults().stream()
        .sorted(Comparator.comparing(JobResult::getJobName, String.CASE_INSENSITIVE_ORDER))
        .forEach(this::printSingleResult);
  }

  private void printSingleResult(JobResult jobResult) {
    System.out.println(
        String.format(
            "%s %s",
            jobResult.getJobName(),
            jobResult.isFailure()
                ? (TERMINAL_ANSI_RED + "FAILED" + TERMINAL_ESCAPE_RESET)
                : (TERMINAL_ANSI_GREEN + "SUCCESS" + TERMINAL_ESCAPE_RESET)));
    System.out.println(String.format("   thread: %s", jobResult.getThreadName()));
    System.out.println(String.format("   job status: %s", jobResult.getJobStatus()));
    System.out.println(
        String.format("   job status as expected: %s", jobResult.isJobStatusAsExpected()));
    System.out.println(
        String.format(
            "   elapsed time (sec): %d",
            TimeUnit.MINUTES.convert(jobResult.getElapsedTimeNS(), TimeUnit.NANOSECONDS)));
    System.out.println(
        String.format("   thread terminated on time: %s", jobResult.isThreadTerminatedOnTime()));
    System.out.println(String.format("   exception msg: %s", jobResult.getExceptionMessage()));
    System.out.println(
        String.format("   exception stack trace: %s", jobResult.getExceptionStackTrace()));
  }
}

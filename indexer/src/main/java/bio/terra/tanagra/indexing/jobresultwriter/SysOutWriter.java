package bio.terra.tanagra.indexing.jobresultwriter;

import bio.terra.tanagra.indexing.jobexecutor.JobResult;
import java.io.PrintStream;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("PMD.SystemPrintln")
public class SysOutWriter extends JobResultWriter {
  private static final String TERMINAL_ESCAPE_RESET = "\u001B[0m";
  private static final String TERMINAL_ANSI_PURPLE = "\u001B[35m";
  private static final String TERMINAL_ANSI_GREEN = "\u001b[32m";
  private static final String TERMINAL_ANSI_RED = "\u001b[31m";

  public SysOutWriter(
      List<String> commandArgs,
      List<JobResult> jobResults,
      long elapsedTimeNS,
      String jobRunnerName,
      PrintStream outStream,
      PrintStream errStream) {
    super(commandArgs, jobResults, elapsedTimeNS, jobRunnerName, outStream, errStream);
  }

  @Override
  public void run() {
    outStream.println(System.lineSeparator());
    outStream.println(
        TERMINAL_ANSI_PURPLE
            + "Indexing job summary ("
            + jobRunnerName
            + ")"
            + TERMINAL_ESCAPE_RESET);
    jobResults.stream()
        .sorted(Comparator.comparing(JobResult::getJobName, String.CASE_INSENSITIVE_ORDER))
        .forEach(this::printSingleResult);
  }

  private void printSingleResult(JobResult jobResult) {
    outStream.printf(
        "%s %s%n",
        jobResult.getJobName(),
        jobResult.isFailure()
            ? (TERMINAL_ANSI_RED + "FAILED" + TERMINAL_ESCAPE_RESET)
            : (TERMINAL_ANSI_GREEN + "SUCCESS" + TERMINAL_ESCAPE_RESET));
    outStream.printf("   thread: %s%n", jobResult.getThreadName());
    outStream.printf("   job status: %s%n", jobResult.getJobStatus());
    outStream.printf("   job status as expected: %s%n", jobResult.isJobStatusAsExpected());
    outStream.printf(
        "   elapsed time (sec): %d%n",
        TimeUnit.MINUTES.convert(jobResult.getElapsedTimeNS(), TimeUnit.NANOSECONDS));
    outStream.printf("   thread terminated on time: %s%n", jobResult.isThreadTerminatedOnTime());
    outStream.printf("   exception msg: %s%n", jobResult.getExceptionMessage());
    outStream.printf("   exception stack trace: %s%n", jobResult.getExceptionStackTrace());
  }
}

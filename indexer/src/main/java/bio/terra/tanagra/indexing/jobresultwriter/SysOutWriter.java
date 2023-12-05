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
      String jobRunnerName,
      PrintStream outStream,
      PrintStream errStream) {
    super(commandArgs, jobResults, jobRunnerName, outStream, errStream);
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
    outStream.println(
        String.format(
            "%s %s",
            jobResult.getJobName(),
            jobResult.isFailure()
                ? (TERMINAL_ANSI_RED + "FAILED" + TERMINAL_ESCAPE_RESET)
                : (TERMINAL_ANSI_GREEN + "SUCCESS" + TERMINAL_ESCAPE_RESET)));
    outStream.println(String.format("   thread: %s", jobResult.getThreadName()));
    outStream.println(String.format("   job status: %s", jobResult.getJobStatus()));
    outStream.println(
        String.format("   job status as expected: %s", jobResult.isJobStatusAsExpected()));
    outStream.println(
        String.format(
            "   elapsed time (sec): %d",
            TimeUnit.MINUTES.convert(jobResult.getElapsedTimeNS(), TimeUnit.NANOSECONDS)));
    outStream.println(
        String.format("   thread terminated on time: %s", jobResult.isThreadTerminatedOnTime()));
    outStream.println(String.format("   exception msg: %s", jobResult.getExceptionMessage()));
    outStream.println(
        String.format("   exception stack trace: %s", jobResult.getExceptionStackTrace()));
  }
}

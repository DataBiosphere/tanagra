package bio.terra.tanagra.indexing.jobresultwriter;

import bio.terra.tanagra.cli.exception.InternalErrorException;
import bio.terra.tanagra.cli.utils.Context;
import bio.terra.tanagra.indexing.jobexecutor.JobResult;
import bio.terra.tanagra.utils.FileUtils;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.apache.commons.text.StringSubstitutor;

@SuppressWarnings("PMD.InsufficientStringBufferDeclaration")
public class HtmlWriter extends JobResultWriter {
  private static final String FILE_NAME = "tanagra-indexing.html";
  private static final String FILE_TEMPLATE =
      "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">\n"
          + "<html xmlns=\"http://www.w3.org/1999/xhtml\">\n"
          + "    <head>\n"
          + "        <title>Indexing Jobs Report</title>\n"
          + "        <style type=\"text/css\">\n"
          + "\t\t* { font-family: Verdana, sans-serif }\n"
          + "\t\t.tablerow0 { background: #EEEEEE; }\n\n"
          + "\t\t.tablerow1 { background: white; }\n\n"
          + "\t\t.detailrow0 { background: #EEEEEE; }\n\n"
          + "\t\t.detailrow1 { background: white; }\n\n"
          + "\t\t.tableheader { background: #C1E1C1; font-size: larger; }\n"
          + "\t\t</style>\n"
          + "    </head>\n"
          + "    <body>\n"
          + "        <h1>Indexing Jobs Report</h1>\n"
          + "        <p><b>Code Version</b></p>\n"
          + "        <p>Git Tag ${version.gitTag}</p>\n"
          + "        <p>Git Hash <a href=\"${version.gitUrl}\">${version.gitHash}</a></p>\n"
          + "        <p>Build ${version.build}</p>\n"
          + "        <br/>\n"
          + "        <p><b>Command:</b> ${jobWriter.command}</p>\n"
          + "        <p><b>Job Runner:</b> ${jobRunner.name}</p>\n"
          + "        <p><b>Working Directory:</b> ${jobRunner.workingDirectory}</p>\n"
          + "        <br/>\n"
          + "        <p><b>Total # jobs failed: ${jobWriter.numFailures}</b></p>\n"
          + "        <p><b>Total # jobs run: ${jobWriter.numJobs}</b></p>\n"
          + "        <p><b>Elapsed Time (min.sec): ${jobWriter.elapsedTime}</b></p>\n"
          + "        <br/>\n"
          + "        <h2>Summary</h2>\n"
          + "        <table width=\"75%\" cellpadding=\"5\" cellspacing=\"2\">\n"
          + "            <tr class=\"tableheader\">\n"
          + "                <th align=\"left\">Entity/Group</th>\n"
          + "                <th align=\"left\"># Jobs Failed</th>\n"
          + "                <th align=\"left\"># Jobs Run</th>\n"
          + "            </tr>\n"
          + "${jobWriter.summaryTableRows}"
          + "        </table>\n"
          + "        <p><br/><br/></p>\n"
          + "        <h2>Job Details</h2>\n"
          + "        <table width=\"75%\" cellpadding=\"5\" cellspacing=\"2\">\n"
          + "            <tr class=\"tableheader\">\n"
          + "                <th align=\"left\">Entity/Group</th>\n"
          + "                <th align=\"left\">Job Name</th>\n"
          + "                <th align=\"left\">Elapsed time (min.sec)</th>\n"
          + "                <th align=\"left\">Thread</th>\n"
          + "                <th align=\"left\">Status</th>\n"
          + "            </tr>\n"
          + "${jobWriter.detailTableRows}"
          + "        </table>\n"
          + "        <p><br/><br/></p>\n"
          + "        <h2>Stack Traces</h2>\n"
          + "${jobWriter.stackTraces}"
          + "    </body>\n"
          + "</html>\n";
  private final Path outputDir;

  public HtmlWriter(
      List<String> commandArgs,
      List<JobResult> jobResults,
      long elapsedTimeNS,
      String jobRunnerName,
      PrintStream outStream,
      PrintStream errStream,
      Path outputDir) {
    super(commandArgs, jobResults, elapsedTimeNS, jobRunnerName, outStream, errStream);
    this.outputDir = outputDir;
  }

  @Override
  public void run() {
    VersionInformation versionInformation = VersionInformation.fromResourceFile();
    Map<String, String> substitutionParams = new HashMap<>();
    substitutionParams.put("version.gitTag", versionInformation.gitTag());
    substitutionParams.put("version.gitHash", versionInformation.gitHash());
    substitutionParams.put("version.gitUrl", versionInformation.getGithubUrl());
    substitutionParams.put("version.build", versionInformation.build());
    substitutionParams.put("jobWriter.command", getCommand());
    substitutionParams.put("jobRunner.name", jobRunnerName);
    substitutionParams.put("jobRunner.workingDirectory", Path.of("").toAbsolutePath().toString());
    substitutionParams.put("jobWriter.numJobs", String.valueOf(getNumJobs()));
    substitutionParams.put("jobWriter.numFailures", String.valueOf(getNumFailures()));
    substitutionParams.put("jobWriter.elapsedTime", elapsedTimeMinSec(getElapsedTimeNS()));
    substitutionParams.put("jobWriter.summaryTableRows", summaryTableRows());
    substitutionParams.put("jobWriter.detailTableRows", detailTableRows());
    substitutionParams.put("jobWriter.stackTraces", stackTraces());
    String fileContents = StringSubstitutor.replace(FILE_TEMPLATE, substitutionParams);

    try {
      FileUtils.writeStringToFile(getOutputFile(), fileContents);
    } catch (IOException ioEx) {
      throw new InternalErrorException("Error writing output file: " + getOutputFile(), ioEx);
    }

    outStream.println(
        "Indexing completed with "
            + getNumFailures()
            + " failures out of "
            + getNumJobs()
            + " jobs.");
    outStream.println("Log statements written to: " + Context.getLogFile());
    outStream.println("Results report written to: " + getOutputFile());
  }

  public Path getOutputFile() {
    return outputDir.resolve(FILE_NAME).toAbsolutePath();
  }

  private String summaryTableRows() {
    StringBuilder summaryRows = new StringBuilder();
    getEntitySummaries().values().stream()
        .sorted(Comparator.comparing(Summary::getEntity))
        .forEach(
            summary ->
                summaryRows.append(
                    "            <tr class=\"tablerow0\">\n"
                        + "                <td><b><a href=\"#"
                        + summary.getEntity()
                        + "\">"
                        + summary.getEntity()
                        + "</a></b></td>\n"
                        + "                <td>"
                        + summary.getNumJobsFailed()
                        + "</td>\n"
                        + "                <td>"
                        + summary.getNumJobsRun()
                        + "</td>\n"
                        + "            </tr>\n"));
    getEntityGroupSummaries().values().stream()
        .sorted(Comparator.comparing(Summary::getEntityGroup))
        .forEach(
            summary ->
                summaryRows.append(
                    "            <tr class=\"tablerow0\">\n"
                        + "                <td><b><a href=\"#"
                        + summary.getEntityGroup()
                        + "\">"
                        + summary.getEntityGroup()
                        + "</b></td>\n"
                        + "                <td>"
                        + summary.getNumJobsFailed()
                        + "</td>\n"
                        + "                <td>"
                        + summary.getNumJobsRun()
                        + "</td>\n"
                        + "            </tr>\n"));
    return summaryRows.toString();
  }

  private String detailTableRows() {
    StringBuilder detailRows = new StringBuilder();
    Stream.concat(
            getEntitySummaries().values().stream().sorted(Comparator.comparing(Summary::getEntity)),
            getEntityGroupSummaries().values().stream()
                .sorted(Comparator.comparing(Summary::getEntityGroup)))
        .forEach(
            summary -> {
              List<JobResult> jobResultsSorted =
                  summary.getJobResults().stream()
                      .sorted(Comparator.comparing(JobResult::getJobName))
                      .toList();
              for (int i = 0; i < jobResultsSorted.size(); i++) {
                JobResult jobResult = jobResultsSorted.get(i);
                String bookmarkName =
                    jobResult.getEntity() != null
                        ? jobResult.getEntity()
                        : jobResult.getEntityGroup();
                detailRows.append(
                    "            <tr class=\"tablerow0\">\n"
                        + "                <td>"
                        + bookmarkName
                        + "</td>\n"
                        + "                <td>"
                        + (i == 0 ? "<a name=\"" + bookmarkName + "\"/>" : "")
                        + jobResult.getJobName()
                        + "</td>\n"
                        + "                <td>"
                        + elapsedTimeMinSec(jobResult.getElapsedTimeNS())
                        + "</td>\n"
                        + "                <td>"
                        + jobResult.getThreadName()
                        + "</td>\n"
                        + "                <td><b>"
                        + (jobResult.isFailure()
                            ? "<a href=\"#" + jobResult.getJobName() + "\">FAILED</a>"
                            : "SUCCEEDED")
                        + "</b></td>\n"
                        + "            </tr>\n");
              }
            });
    return detailRows.toString();
  }

  private String elapsedTimeMinSec(long elapsedTimeNS) {
    long min = TimeUnit.MINUTES.convert(elapsedTimeNS, TimeUnit.NANOSECONDS);
    long sec = TimeUnit.SECONDS.convert(elapsedTimeNS, TimeUnit.NANOSECONDS) - (min * 60);
    return String.valueOf(min) + '.' + (sec < 10 ? '0' + String.valueOf(sec) : String.valueOf(sec));
  }

  private String stackTraces() {
    StringBuilder stackTraces = new StringBuilder();
    Stream.concat(
            getEntitySummaries().values().stream().sorted(Comparator.comparing(Summary::getEntity)),
            getEntityGroupSummaries().values().stream()
                .sorted(Comparator.comparing(Summary::getEntityGroup)))
        .forEach(
            summary ->
                summary.getJobResults().stream()
                    .sorted(Comparator.comparing(JobResult::getJobName))
                    .filter(JobResult::isFailure)
                    .forEach(
                        jobResult ->
                            stackTraces.append(
                                "        <h3><a name=\""
                                    + jobResult.getJobName()
                                    + "\"/>"
                                    + jobResult.getJobName()
                                    + "</h3>\n"
                                    + "            <pre>"
                                    + jobResult.getExceptionStackTrace()
                                    + "</pre>\n")));
    return stackTraces.toString();
  }
}

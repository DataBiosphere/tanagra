package bio.terra.tanagra.indexing.jobresultwriter;

import bio.terra.tanagra.cli.BaseMain;
import bio.terra.tanagra.indexing.jobexecutor.JobResult;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

public abstract class JobResultWriter {
  protected final List<String> commandArgs;
  protected final List<JobResult> jobResults;
  protected final long elapsedTimeNS;
  protected final String jobRunnerName;
  protected final PrintStream outStream;
  protected final PrintStream errStream;
  private final long numJobs;
  private final long numFailures;
  private final Map<String, Summary> entitySummaries = new HashMap<>();
  private final Map<String, Summary> entityGroupSummaries = new HashMap<>();

  protected JobResultWriter(
      List<String> commandArgs,
      List<JobResult> jobResults,
      long elapsedTimeNS,
      String jobRunnerName,
      PrintStream outStream,
      PrintStream errStream) {
    this.commandArgs = commandArgs;
    this.jobResults = jobResults;
    this.elapsedTimeNS = elapsedTimeNS;
    this.jobRunnerName = jobRunnerName;
    this.outStream = outStream;
    this.errStream = errStream;
    this.numJobs = jobResults.size();
    this.numFailures = jobResults.stream().filter(JobResult::isFailure).count();
    jobResults.stream()
        .forEach(
            jobResult -> {
              if (jobResult.getEntity() != null) {
                Summary summary =
                    entitySummaries.containsKey(jobResult.getEntity())
                        ? entitySummaries.get(jobResult.getEntity())
                        : new Summary(jobResult.getEntity(), null);
                summary.addJobResult(jobResult);
                entitySummaries.put(jobResult.getEntity(), summary);
              } else {
                Summary summary =
                    entityGroupSummaries.containsKey(jobResult.getEntityGroup())
                        ? entityGroupSummaries.get(jobResult.getEntityGroup())
                        : new Summary(null, jobResult.getEntityGroup());
                summary.addJobResult(jobResult);
                entityGroupSummaries.put(jobResult.getEntityGroup(), summary);
              }
            });
  }

  public long getNumJobs() {
    return numJobs;
  }

  public long getNumFailures() {
    return numFailures;
  }

  public long getElapsedTimeNS() {
    return elapsedTimeNS;
  }

  protected ImmutableMap<String, Summary> getEntitySummaries() {
    return ImmutableMap.copyOf(entitySummaries);
  }

  protected ImmutableMap<String, Summary> getEntityGroupSummaries() {
    return ImmutableMap.copyOf(entityGroupSummaries);
  }

  protected String getCommand() {
    return "tanagra " + String.join(" ", BaseMain.getArgList());
  }

  public abstract void run();

  public static class Summary {
    private final @Nullable String entity;
    private final @Nullable String entityGroup;

    private final List<JobResult> jobResults = new ArrayList<>();

    public Summary(@Nullable String entity, @Nullable String entityGroup) {
      this.entity = entity;
      this.entityGroup = entityGroup;
    }

    public void addJobResult(JobResult jobResult) {
      jobResults.add(jobResult);
    }

    @Nullable
    public String getEntity() {
      return entity;
    }

    @Nullable
    public String getEntityGroup() {
      return entityGroup;
    }

    public ImmutableList<JobResult> getJobResults() {
      return ImmutableList.copyOf(jobResults);
    }

    public int getNumJobsRun() {
      return jobResults.size();
    }

    public long getNumJobsFailed() {
      return jobResults.stream().filter(JobResult::isFailure).count();
    }
  }
}

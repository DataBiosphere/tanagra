package bio.terra.tanagra.indexing.jobexecutor;

import bio.terra.tanagra.indexing.job.IndexingJob;
import java.util.Iterator;
import java.util.List;

/** Utility class that runs multiple job sets in serial. */
public final class SerialRunner extends JobRunner {
  public SerialRunner(
      List<SequencedJobSet> jobSets, boolean isDryRun, IndexingJob.RunType runType) {
    super(jobSets, isDryRun, runType);
  }

  @Override
  public String getName() {
    return "SERIAL";
  }

  /** Run all job sets serially. */
  @Override
  public void runJobSetsWithoutTimer() {
    jobSets.forEach(this::runSingleJobSet);
  }

  /** Run a single job set. Run the stages serially, and the jobs within each stage serially. */
  @Override
  protected void runSingleJobSet(SequencedJobSet sequencedJobSet) {
    // Iterate through the job stages, running all jobs in each stage.
    Iterator<List<IndexingJob>> jobStagesIterator = sequencedJobSet.iterator();
    while (jobStagesIterator.hasNext()) {
      List<IndexingJob> jobsInStage = jobStagesIterator.next();
      for (IndexingJob job : jobsInStage) {
        JobResult jobResult = new JobThread(job, isDryRun, runType, job.getName()).call();
        jobResult.setThreadTerminatedOnTime(true);
        jobResults.add(jobResult);
      }
    }
  }
}

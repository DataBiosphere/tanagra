package bio.terra.tanagra.indexing.job;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface IndexingJob {
  Logger LOGGER = LoggerFactory.getLogger(IndexingJob.class);

  enum JobStatus {
    NOT_STARTED,
    IN_PROGRESS,
    COMPLETE
  }

  enum RunType {
    STATUS,
    CLEAN,
    RUN
  }

  String getName();

  void run(boolean isDryRun);

  void clean(boolean isDryRun);

  JobStatus checkStatus();

  default JobStatus execute(RunType runType, boolean isDryRun) {
    LOGGER.info("Executing indexing job: {}, {}", runType, getName());
    JobStatus status = checkStatus();
    LOGGER.info("Job status: {}", status);

    switch (runType) {
      case RUN:
        if (!JobStatus.NOT_STARTED.equals(status)) {
          LOGGER.info("Skipping because job is either in progress or complete");
          return status;
        }
        run(isDryRun);
        return checkStatus();
      case CLEAN:
        if (JobStatus.IN_PROGRESS.equals(status)) {
          LOGGER.info("Skipping because job is in progress");
          return status;
        }
        clean(isDryRun);
        return checkStatus();
      case STATUS:
        return status;
      default:
        throw new IllegalArgumentException("Unknown execution type: " + runType);
    }
  }

  /** Check if the job completed what it was supposed to. */
  static boolean checkStatusAfterRunMatchesExpected(
      RunType runType, boolean isDryRun, JobStatus status) {
    return isDryRun
        || RunType.STATUS.equals(runType)
        || (RunType.RUN.equals(runType) && JobStatus.COMPLETE.equals(status))
        || (RunType.CLEAN.equals(runType) && JobStatus.NOT_STARTED.equals(status));
  }
}

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

  default String getEntity() {
    return null;
  }

  default String getEntityGroup() {
    return null;
  }

  String getName();

  JobStatus checkStatus();

  void run(boolean isDryRun);

  void clean(boolean isDryRun);

  /** Check if the job completed what it was supposed to. */
  default boolean checkStatusAfterRunMatchesExpected(
      RunType runType, boolean isDryRun, JobStatus status) {
    return isDryRun
        || RunType.STATUS.equals(runType)
        || (RunType.RUN.equals(runType) && JobStatus.COMPLETE.equals(status))
        || (RunType.CLEAN.equals(runType) && JobStatus.NOT_STARTED.equals(status));
  }
}

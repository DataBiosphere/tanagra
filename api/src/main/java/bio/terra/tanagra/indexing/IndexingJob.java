package bio.terra.tanagra.indexing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface IndexingJob {
  Logger LOGGER = LoggerFactory.getLogger(IndexingJob.class);

  enum JobStatus {
    NOT_STARTED,
    IN_PROGRESS,
    COMPLETE
  }

  void dryRun();

  void run();

  JobStatus checkStatus();

  boolean prerequisitesComplete();

  default void runIfPending() {
    if (!prerequisitesComplete()) {
      LOGGER.info("Skipping because prerequisites are not complete");
      return;
    }
    JobStatus status = checkStatus();
    LOGGER.info("Job status: {}", status);
    if (JobStatus.NOT_STARTED.equals(status)) {
      run();
    }
  }
}

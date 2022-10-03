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

  String getName();

  void run(boolean isDryRun);

  void clean(boolean isDryRun);

  JobStatus checkStatus();

  boolean prerequisitesComplete();

  default void checkStatusAndRun(boolean isDryRun) {
    LOGGER.info("RUN Indexing job: {}", getName());
    JobStatus status = checkStatus();
    LOGGER.info("Job status: {}", status);

    if (!prerequisitesComplete()) {
      LOGGER.info("Skipping because prerequisites are not complete");
      return;
    } else if (!JobStatus.NOT_STARTED.equals(status)) {
      LOGGER.info("Skipping because job is either in progress or complete");
      return;
    }
    run(isDryRun);
  }

  default void checkStatusAndClean(boolean isDryRun) {
    LOGGER.info("CLEAN Indexing job: {}", getName());
    JobStatus status = checkStatus();
    LOGGER.info("Job status: {}", status);

    if (JobStatus.IN_PROGRESS.equals(status)) {
      LOGGER.info("Skipping because job is in progress");
      return;
    }
    clean(isDryRun);
  }
}

package bio.terra.tanagra.db.exception;

import bio.terra.common.exception.BadRequestException;

/** A cohort with this user_facing_cohort_id already exists. */
public class DuplicateCohortException extends BadRequestException {
  public DuplicateCohortException(String message) {
    super(message);
  }

  public DuplicateCohortException(String message, Throwable cause) {
    super(message, cause);
  }
}

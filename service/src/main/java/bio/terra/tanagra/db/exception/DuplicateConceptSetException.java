package bio.terra.tanagra.db.exception;

import bio.terra.common.exception.BadRequestException;

/** A concept set with this id already exists. */
public class DuplicateConceptSetException extends BadRequestException {
  public DuplicateConceptSetException(String message) {
    super(message);
  }

  public DuplicateConceptSetException(String message, Throwable cause) {
    super(message, cause);
  }
}

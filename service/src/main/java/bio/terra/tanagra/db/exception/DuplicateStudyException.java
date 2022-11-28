package bio.terra.tanagra.db.exception;

import bio.terra.common.exception.BadRequestException;

/** A study with this study_id already exists. */
public class DuplicateStudyException extends BadRequestException {
  public DuplicateStudyException(String message) {
    super(message);
  }

  public DuplicateStudyException(String message, Throwable cause) {
    super(message, cause);
  }
}

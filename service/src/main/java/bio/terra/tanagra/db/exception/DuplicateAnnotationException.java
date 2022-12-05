package bio.terra.tanagra.db.exception;

import bio.terra.common.exception.BadRequestException;

/** An annotation with this id already exists. */
public class DuplicateAnnotationException extends BadRequestException {
  public DuplicateAnnotationException(String message) {
    super(message);
  }

  public DuplicateAnnotationException(String message, Throwable cause) {
    super(message, cause);
  }
}

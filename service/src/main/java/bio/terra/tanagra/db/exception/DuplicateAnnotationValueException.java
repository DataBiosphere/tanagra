package bio.terra.tanagra.db.exception;

import bio.terra.common.exception.BadRequestException;

/** An annotation value with this id already exists. */
public class DuplicateAnnotationValueException extends BadRequestException {
  public DuplicateAnnotationValueException(String message) {
    super(message);
  }

  public DuplicateAnnotationValueException(String message, Throwable cause) {
    super(message, cause);
  }
}

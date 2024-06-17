package bio.terra.tanagra.exception;

/** Custom exception class for not found exceptions. */
public class NotFoundException extends RuntimeException {
  /**
   * Constructs an exception with the given message. The cause is set to null.
   *
   * @param message description of error that may help with debugging
   */
  public NotFoundException(String message) {
    super(message);
  }

  /**
   * Constructs an exception with the given message and cause.
   *
   * @param message description of error that may help with debugging
   * @param cause underlying exception that can be logged for debugging purposes
   */
  public NotFoundException(String message, Throwable cause) {
    super(message, cause);
  }
}

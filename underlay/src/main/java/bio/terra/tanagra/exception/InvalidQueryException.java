package bio.terra.tanagra.exception;

/**
 * Custom exception class for invalid query exceptions. These represent errors in the specification
 * of a query request, that the user needs to fix (e.g. "entity has no hierarchy").
 */
public class InvalidQueryException extends SystemException {
  /**
   * Constructs an exception with the given message. The cause is set to null.
   *
   * @param message description of error that may help with debugging
   */
  public InvalidQueryException(String message) {
    super(message);
  }

  /**
   * Constructs an exception with the given message and cause.
   *
   * @param message description of error that may help with debugging
   * @param cause underlying exception that can be logged for debugging purposes
   */
  public InvalidQueryException(String message, Throwable cause) {
    super(message, cause);
  }
}

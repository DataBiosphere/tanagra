package bio.terra.tanagra.cli.exception;

import java.util.Objects;

/**
 * Custom exception class for internal error exceptions. These represent errors that the user cannot
 * fix.
 *
 * <p>In contrast to UserActionableExceptions, throwing an InternalErrorException will
 *
 * <p>-print information about the cause or point users to the log file for more information
 *
 * <p>-use a more scary font when printing to the terminal (e.g. bold red text).
 */
public class InternalErrorException extends RuntimeException {
  private static final String DEFAULT_ERROR_MESSAGE = "No error message available.";

  /**
   * Constructs an exception with the given message. The cause is set to null.
   *
   * @param message string to display to the user
   */
  public InternalErrorException(String message) {
    super(Objects.requireNonNullElse(message, DEFAULT_ERROR_MESSAGE));
  }

  /**
   * Constructs an exception with the given message and cause.
   *
   * @param message string to display to the user
   * @param cause underlying exception that can be logged for debugging purposes
   */
  public InternalErrorException(String message, Throwable cause) {
    super(Objects.requireNonNullElse(message, DEFAULT_ERROR_MESSAGE), cause);
  }
}

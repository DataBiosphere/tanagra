package bio.terra.tanagra.service.auth;

import bio.terra.common.exception.UnauthorizedException;

public class InvalidTokenException extends UnauthorizedException {
  public InvalidTokenException(String message) {
    super(message);
  }

  public InvalidTokenException(String message, Throwable cause) {
    super(message, cause);
  }
}

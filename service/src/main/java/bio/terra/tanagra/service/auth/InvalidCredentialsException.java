package bio.terra.tanagra.service.auth;

import bio.terra.common.exception.UnauthorizedException;

public class InvalidCredentialsException extends UnauthorizedException {
  public InvalidCredentialsException(String message) {
    super(message);
  }

  public InvalidCredentialsException(String message, Throwable cause) {
    super(message, cause);
  }
}

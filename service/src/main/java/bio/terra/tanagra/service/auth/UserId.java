package bio.terra.tanagra.service.auth;

import bio.terra.common.exception.UnauthorizedException;
import java.io.Serializable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Wrapper around a user id. This is the output of decoding a credential, so any information Tanagra
 * or its plugins need can be stored here.
 */
public final class UserId implements Serializable {
  private static final String DISABLED_AUTHENTICATION_USER_ID = "authentication-disabled";

  private final String subject;
  private final String email;

  private UserId(String subject, String email) {
    this.subject = subject;
    this.email = email;
  }

  /** Build a default user object for when authentication is disabled. */
  public static UserId forDisabledAuthentication() {
    return new UserId(DISABLED_AUTHENTICATION_USER_ID, DISABLED_AUTHENTICATION_USER_ID);
  }

  /** Build a user object with information from an authentication token. */
  public static UserId fromToken(String subject, String email) {
    return new UserId(subject, email);
  }

  /** Get the current user object from the security context. */
  public static UserId currentUser() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (!(authentication instanceof UserAuthentication)) {
      throw new UnauthorizedException("Error loading user authentication object");
    }
    return ((UserAuthentication) authentication).getPrincipal();
  }

  public String getSubject() {
    return subject;
  }

  public String getEmail() {
    return email;
  }
}

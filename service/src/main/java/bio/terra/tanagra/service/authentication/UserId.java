package bio.terra.tanagra.service.authentication;

import java.io.Serializable;

public final class UserId implements Serializable {
  private static final String DISABLED_AUTHENTICATION_USER_ID = "authentication-disabled";

  private final String subject;
  private final String email;
  private final String token;

  private UserId(String subject, String email, String token) {
    this.subject = subject;
    this.email = email;
    this.token = token;
  }

  /** Build a default user object for when authentication is disabled. */
  public static UserId forDisabledAuthentication() {
    return new UserId(DISABLED_AUTHENTICATION_USER_ID, DISABLED_AUTHENTICATION_USER_ID, "");
  }

  /** Build a user object with information from an authentication token. */
  public static UserId fromToken(String subject, String email, String token) {
    return new UserId(subject, email, token);
  }

  public String getSubject() {
    return subject;
  }

  public String getEmail() {
    return email;
  }

  public String getToken() {
    return token;
  }
}

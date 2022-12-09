package bio.terra.tanagra.service.auth;

import java.io.Serializable;

/**
 * Wrapper around a user id. This is the output of decoding a credential, so any information Tanagra
 * or its plugins need can be stored here.
 */
public class UserId implements Serializable {
  private final String subject;
  private final String email;

  public UserId(String subject, String email) {
    this.subject = subject;
    this.email = email;
  }

  public String getSubject() {
    return subject;
  }

  public String getEmail() {
    return email;
  }
}

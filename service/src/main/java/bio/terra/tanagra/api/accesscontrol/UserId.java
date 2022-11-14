package bio.terra.tanagra.api.accesscontrol;

/**
 * Wrapper around a user id. This is the output of decoding a credential, so any information Tanagra
 * or its plugins need can be stored here.
 */
public class UserId {
  private final String id;

  public UserId(String id) {
    this.id = id;
  }

  public String getId() {
    return id;
  }
}

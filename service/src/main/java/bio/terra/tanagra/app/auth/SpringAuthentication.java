package bio.terra.tanagra.app.auth;

import bio.terra.common.exception.UnauthorizedException;
import bio.terra.tanagra.service.auth.UserId;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

public class SpringAuthentication implements Authentication {
  private final UserId userId;

  public SpringAuthentication(UserId userId) {
    this.userId = userId;
  }

  @Override
  public String getName() {
    return userId.getEmail();
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return List.of();
  }

  @Override
  public String getCredentials() {
    return userId.getToken();
  }

  @Override
  public Object getDetails() {
    return null;
  }

  @Override
  public UserId getPrincipal() {
    return userId;
  }

  @Override
  public boolean isAuthenticated() {
    return true;
  }

  @Override
  @SuppressWarnings("PMD.UncommentedEmptyMethodBody")
  public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {}

  /** Get the current user object from the Spring security context. */
  public static UserId getCurrentUser() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (!(authentication instanceof SpringAuthentication)) {
      throw new UnauthorizedException("Error loading user authentication object");
    }
    return ((SpringAuthentication) authentication).getPrincipal();
  }

  /** Set the given user in the Spring security context. */
  public static void setCurrentUser(UserId userId) {
    SecurityContextHolder.getContext().setAuthentication(new SpringAuthentication(userId));
  }

  /**
   * Clear the Spring security context, just to make sure nothing subsequently uses the credentials
   * set up there.
   */
  public static void clearCurrentUser() {
    SecurityContextHolder.clearContext();
  }
}

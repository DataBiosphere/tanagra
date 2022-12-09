package bio.terra.tanagra.service.auth;

import java.util.Collection;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

public class UserAuthentication implements Authentication {
  public enum TokenType {
    JWT,
    BEARER_TOKEN
  }

  private final UserId userId;
  private final String token;
  private final TokenType tokenType;

  public UserAuthentication(UserId userId, String token, TokenType tokenType) {
    this.userId = userId;
    this.token = token;
    this.tokenType = tokenType;
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
    return token;
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

  public TokenType getTokenType() {
    return tokenType;
  }
}

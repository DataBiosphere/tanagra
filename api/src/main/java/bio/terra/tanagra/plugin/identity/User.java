package bio.terra.tanagra.plugin.identity;

import bio.terra.tanagra.generated.model.ApiUser;
import java.util.ArrayList;
import java.util.List;

public class User {
  private final String identifier;
  private final List<String> roles;

  private String givenName;
  private String surname;

  public User(String identifier) {
    this(identifier, null, null, new ArrayList<>());
  }

  public User(String identifier, String givenName, String surname, List<String> roles) {
    this.identifier = identifier;
    this.givenName = givenName;
    this.surname = surname;
    this.roles = roles;
  }

  public String getIdentifier() {
    return identifier;
  }

  public void setGivenName(String givenName) {
    this.givenName = givenName;
  }

  public String getGivenName() {
    return this.givenName;
  }

  public void setSurname(String surname) {
    this.surname = surname;
  }

  public String getSurname() {
    return this.surname;
  }

  public void addRole(String role) {
    this.roles.add(role);
  }

  public void removeRole(String role) {
    this.roles.remove(role);
  }

  public boolean hasRole(String role) {
    return roles.contains(role);
  }

  public ApiUser toApiObject() {
    ApiUser apiUser = new ApiUser();
    apiUser.setIdentifier(this.identifier);
    apiUser.setGivenName(this.givenName);
    apiUser.setSurname(this.surname);

    return apiUser;
  }
}

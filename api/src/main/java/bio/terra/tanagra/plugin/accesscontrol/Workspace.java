package bio.terra.tanagra.plugin.accesscontrol;

import bio.terra.tanagra.generated.model.ApiWorkspace;
import bio.terra.tanagra.plugin.identity.User;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Workspace implements IAccessControlledEntity {
  private final String identifier;
  private String name;
  private List<User> members;

  public Workspace(String identifier) {
    this(identifier, new ArrayList<>());
  }

  public Workspace(String identifier, List<User> members) {
    this.identifier = identifier;
    this.members = members;
  }

  @Override
  public String getAccessControlType() {
    return this.getClass().getSimpleName();
  }

  @Override
  public String getIdentifier() {
    return identifier;
  }

  @Override
  public void setMembers(List<User> members) {
    this.members = members;
  }

  @Override
  public void addMember(User member) {
    this.members.add(member);
  }

  @Override
  public List<User> getMembers() {
    return this.members;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getName() {
    return this.name;
  }

  public ApiWorkspace toApiObject() {
    ApiWorkspace apiWorkspace = new ApiWorkspace();
    apiWorkspace.setIdentifier(this.getIdentifier());
    apiWorkspace.setName(this.getName());
    apiWorkspace.setMembers(
        this.members.stream().map(User::toApiObject).collect(Collectors.toList()));

    return apiWorkspace;
  }
}

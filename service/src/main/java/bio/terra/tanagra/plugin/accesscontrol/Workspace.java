package bio.terra.tanagra.plugin.accesscontrol;

import bio.terra.tanagra.plugin.identity.User;
import java.util.ArrayList;
import java.util.List;

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

  /*
  public ServiceWorkspace toServiceObject() {
    ServiceWorkspace serviceWorkspace = new ServiceWorkspace();
    serviceWorkspace.setIdentifier(this.getIdentifier());
    serviceWorkspace.setName(this.getName());
    serviceWorkspace.setMembers(
        this.members.stream().map(User::toServiceObject).collect(Collectors.toList()));

    return serviceWorkspace;
  }
  */
}

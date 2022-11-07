package bio.terra.tanagra.plugin.accesscontrol;

import bio.terra.tanagra.plugin.identity.User;
import java.util.ArrayList;
import java.util.List;

public class Cohort implements IAccessControlledEntity {
  private final String identifier;
  private List<User> members;

  public Cohort(String identifier) {
    this(identifier, new ArrayList<>());
  }

  public Cohort(String identifier, List<User> members) {
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
}

package bio.terra.tanagra.plugin.accesscontrol;

import bio.terra.tanagra.plugin.identity.User;
import java.util.List;

public interface IAccessControlledEntity {
  String getAccessControlType();

  String getIdentifier();

  void setMembers(List<User> members);

  void addMember(User member);

  List<User> getMembers();
}

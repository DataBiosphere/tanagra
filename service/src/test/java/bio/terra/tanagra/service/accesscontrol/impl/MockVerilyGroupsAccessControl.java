package bio.terra.tanagra.service.accesscontrol.impl;

import bio.terra.tanagra.service.accesscontrol2.impl.VerilyGroupsAccessControl;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MockVerilyGroupsAccessControl extends VerilyGroupsAccessControl {
  private final Map<String, VerilyGroup> groups = new HashMap<>(); // group id -> group object
  private final Map<String, List<String>> members = new HashMap<>(); // group id -> list of members

  /**
   * Call the VerilyGroups API to list all the groups the application default credentials have
   * access to.
   */
  @Override
  protected List<VerilyGroup> apiListGroups() {
    return groups.values().stream()
        .sorted(Comparator.comparing(VerilyGroup::getId))
        .collect(Collectors.toList());
  }

  /**
   * Call the VerilyGroups API to list all the members in a group. Administrator access to a group
   * is required to list its members.
   *
   * @return List of member emails.
   */
  @Override
  protected List<String> apiListMembers(String groupId) {
    return members.get(groupId);
  }

  public void addGroup(VerilyGroup newGroup, List<String> newMembers) {
    groups.put(newGroup.getId(), newGroup);
    members.put(newGroup.getId(), newMembers);
  }
}

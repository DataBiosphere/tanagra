package bio.terra.tanagra.service.accesscontrol.impl;

import bio.terra.tanagra.service.accesscontrol.model.impl.SamGroupsAccessControl;
import bio.terra.tanagra.service.authentication.UserId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.broadinstitute.dsde.workbench.client.sam.model.ManagedGroupMembershipEntry;

public class MockSamGroupsAccessControl extends SamGroupsAccessControl {

  private final Map<String, String> underlayToGroups = new HashMap<>(); // underlay -> group name

  private final Map<String, List<ManagedGroupMembershipEntry>> samGroupMemberships =
      new HashMap<>(); // user -> list of managed group memberships

  @Override
  protected List<ManagedGroupMembershipEntry> apiListGroupMemberships(UserId user) {
    return samGroupMemberships.get(user.getEmail());
  }

  public void addMembership(String userEmail, String underlay, String groupName) {
    underlayToGroups.put(underlay, groupName);

    List<ManagedGroupMembershipEntry> userGroups =
        samGroupMemberships.getOrDefault(userEmail, new ArrayList<>());
    userGroups.add(new ManagedGroupMembershipEntry().groupName(groupName));
    samGroupMemberships.put(userEmail, userGroups);
  }
}

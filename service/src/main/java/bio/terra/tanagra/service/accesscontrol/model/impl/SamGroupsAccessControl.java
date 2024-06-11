package bio.terra.tanagra.service.accesscontrol.model.impl;

import bio.terra.common.sam.SamRetry;
import bio.terra.tanagra.service.accesscontrol.AccessControlHelper;
import bio.terra.tanagra.service.accesscontrol.Permissions;
import bio.terra.tanagra.service.accesscontrol.ResourceCollection;
import bio.terra.tanagra.service.accesscontrol.ResourceId;
import bio.terra.tanagra.service.accesscontrol.ResourceType;
import bio.terra.tanagra.service.accesscontrol.model.UnderlayAccessControl;
import bio.terra.tanagra.service.authentication.UserId;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import okhttp3.OkHttpClient;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.dsde.workbench.client.sam.ApiClient;
import org.broadinstitute.dsde.workbench.client.sam.ApiException;
import org.broadinstitute.dsde.workbench.client.sam.api.GroupApi;
import org.broadinstitute.dsde.workbench.client.sam.model.ManagedGroupMembershipEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

public class SamGroupsAccessControl implements UnderlayAccessControl {
  private static final Logger LOGGER = LoggerFactory.getLogger(SamGroupsAccessControl.class);

  private String basePath;

  private OkHttpClient commonHttpClient;
  private final Map<String, String> underlayToGroup = new HashMap<>();

  @Override
  public String getDescription() {
    return "SamGroups underlay-based access control";
  }

  @Override
  public void initialize(
      List<String> params,
      String basePath,
      String oauthClientId,
      AccessControlHelper accessControlHelper) {
    this.basePath = basePath;
    Assert.notNull(basePath, "Base URL is required for Sam Service API calls");

    Assert.isTrue(
        params.size() % 2 == 0,
        "Require even number of parameters to SamGroups access control implementation: underlay1,groupName1,underlay2,groupName2,...");
    for (int i = 0; i < params.size(); i += 2) {
      underlayToGroup.put(params.get(i), params.get(i + 1));
    }

    this.commonHttpClient = new ApiClient().getHttpClient();
  }

  public GroupApi samGroupApi(String accessToken) {
    // OkHttpClient objects manage their own thread pools, so it's much more performant to share one
    // across requests.
    ApiClient apiClient = new ApiClient().setHttpClient(commonHttpClient).setBasePath(basePath);
    apiClient.setAccessToken(accessToken);
    return new GroupApi(apiClient);
  }

  /*
  Call Sam Service to get a list of the user's group memberships (direct and inherited)
   */
  protected List<ManagedGroupMembershipEntry> apiListGroupMemberships(UserId user) {
    try {
      GroupApi groupApi = samGroupApi(user.getToken());
      return SamRetry.retry(groupApi::listGroupMemberships);
    } catch (InterruptedException | ApiException ex) {
      LOGGER.error("Error listing SamGroupMemberships for user " + user.getEmail(), ex);
    }
    return Collections.emptyList();
  }

  private List<String> listSamGroupMemberships(UserId user) {
    // sam group roles: member / admin. filter out memberships without roles
    return apiListGroupMemberships(user).stream()
        .filter(entry -> StringUtils.isNotEmpty(entry.getRole()))
        .map(ManagedGroupMembershipEntry::getGroupName)
        .collect(Collectors.toList());
  }

  @Override
  public ResourceCollection listUnderlays(UserId user, int offset, int limit) {
    List<String> groupMemberships = listSamGroupMemberships(user);

    Set<ResourceId> underlaysWithAccess =
        underlayToGroup.entrySet().stream()
            .filter(entry -> groupMemberships.contains(entry.getValue()))
            .map(entry -> ResourceId.forUnderlay(entry.getKey()))
            .collect(Collectors.toSet());

    return underlaysWithAccess.isEmpty()
        ? ResourceCollection.empty(ResourceType.UNDERLAY, null)
        : ResourceCollection.resourcesSamePermissions(
                Permissions.allActions(ResourceType.UNDERLAY), underlaysWithAccess)
            .slice(offset, limit);
  }

  @Override
  public Permissions getUnderlay(UserId user, ResourceId underlay) {
    // There are no partial permissions, users either have all or none.
    String groupName = underlayToGroup.getOrDefault(underlay.getUnderlay(), null);
    return groupName != null && listSamGroupMemberships(user).contains(groupName)
        ? Permissions.allActions(ResourceType.UNDERLAY)
        : Permissions.empty(ResourceType.UNDERLAY);
  }
}

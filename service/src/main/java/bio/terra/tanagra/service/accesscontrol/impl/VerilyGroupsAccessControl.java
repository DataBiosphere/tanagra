package bio.terra.tanagra.service.accesscontrol.impl;

import bio.terra.tanagra.exception.SystemException;
import bio.terra.tanagra.service.accesscontrol.AccessControl;
import bio.terra.tanagra.service.accesscontrol.Permissions;
import bio.terra.tanagra.service.accesscontrol.ResourceCollection;
import bio.terra.tanagra.service.accesscontrol.ResourceId;
import bio.terra.tanagra.service.accesscontrol.ResourceType;
import bio.terra.tanagra.service.authentication.AppDefaultUtils;
import bio.terra.tanagra.service.authentication.InvalidCredentialsException;
import bio.terra.tanagra.service.authentication.UserId;
import com.google.auth.oauth2.IdToken;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * VerilyGroups access control plugin implementation that checks membership in a VerilyGroup for
 * each underlay.
 */
public class VerilyGroupsAccessControl implements AccessControl {
  private static final Logger LOGGER = LoggerFactory.getLogger(VerilyGroupsAccessControl.class);

  public static final String ALL_ACCESS = "ALL_ACCESS";

  private String basePath;
  private String oauthClientId;

  private VerilyGroup allAccessGroup;
  private final Map<String, VerilyGroup> underlayToGroup = new HashMap<>();

  @Override
  public String getDescription() {
    return "Check VerilyGroup membership for underlay access";
  }

  @Override
  public void initialize(List<String> params, String basePath, String oauthClientId) {
    // Store the basePath and oauthClientId first, so we can use it when looking up group IDs.
    if (basePath == null || oauthClientId == null) {
      throw new IllegalArgumentException(
          "Base URL and OAuth client id are required for VerilyGroup API calls");
    }
    this.basePath = basePath;
    this.oauthClientId = oauthClientId;

    if (params.size() % 2 != 0) {
      throw new IllegalArgumentException(
          "Require even number of parameters to VerilyGroups access control implementation: underlay1,groupName1,underlay2,groupName2,...");
    }

    try {
      // Lookup the group ID for each group name. The ID is required to list the members.
      Map<String, VerilyGroup> nameToGroup =
          apiListGroups().stream()
              .collect(Collectors.toMap(VerilyGroup::getName, Function.identity()));

      // Build a map of underlay name to VerilyGroup object.
      for (int i = 0; i < params.size(); i += 2) {
        String underlay = params.get(i);
        String groupName = params.get(i + 1);
        if (ALL_ACCESS.equals(underlay)) {
          allAccessGroup = nameToGroup.get(groupName);
        } else {
          underlayToGroup.put(underlay, nameToGroup.get(groupName));
        }
      }
    } catch (SystemException | InvalidCredentialsException ex) {
      LOGGER.error("Error initializing VerilyGroups access control implementation", ex);
    }
  }

  @Override
  public boolean isAuthorized(UserId user, Permissions permissions, @Nullable ResourceId resource) {
    if (ResourceType.UNDERLAY.equals(permissions.getType())) {
      // Check membership in ALL_ACCESS and/or underlay-specific group.
      return hasAllUnderlayAccess(user.getEmail())
          || hasSpecificUnderlayAccess(resource.getUnderlay(), user.getEmail());
    } else {
      // For resources other than underlays, all actions are allowed to everyone.
      return true;
    }
  }

  @Override
  public Permissions getPermissions(UserId user, ResourceId resource) {
    // There are no partial permissions, users either have all or none.
    return isAuthorized(user, Permissions.allActions(resource.getType()), resource)
        ? Permissions.allActions(resource.getType())
        : Permissions.empty(resource.getType());
  }

  @Override
  public ResourceCollection listAllPermissions(
      UserId user, ResourceType type, @Nullable ResourceId parentResource, int offset, int limit) {
    if (ResourceType.UNDERLAY.equals(type)) {
      if (hasAllUnderlayAccess(user.getEmail())) {
        // If user is a member in ALL_ACCESS group, then return all underlays.
        return ResourceCollection.allResourcesAllPermissions(ResourceType.UNDERLAY, null);
      } else {
        // Otherwise, check membership in each of the underlay-specific groups.
        Map<ResourceId, Permissions> underlayPermissionsMap = new HashMap<>();
        underlayToGroup.keySet().stream()
            .forEach(
                underlay ->
                    underlayPermissionsMap.put(
                        ResourceId.forUnderlay(underlay),
                        hasSpecificUnderlayAccess(underlay, user.getEmail())
                            ? Permissions.allActions(ResourceType.UNDERLAY)
                            : Permissions.empty(ResourceType.UNDERLAY)));
        return ResourceCollection.resourcesDifferentPermissions(underlayPermissionsMap)
            .slice(offset, limit);
      }
    } else {
      // For resources other than underlays, everyone has all permissions on everything.
      return ResourceCollection.allResourcesAllPermissions(type, parentResource);
    }
  }

  /** Return true if the user email is included the underlay-specific group membership list. */
  private boolean hasSpecificUnderlayAccess(String underlay, String userEmail) {
    // Null group or unmapped underlay means the underlay is inaccessible.
    VerilyGroup group = underlayToGroup.get(underlay);
    return group != null && apiListMembers(group.getId()).contains(userEmail);
  }

  /** Return true if the user email is included in the ALL_ACCESS group membership list. */
  private boolean hasAllUnderlayAccess(String userEmail) {
    if (allAccessGroup == null) {
      return false;
    } else {
      return apiListMembers(allAccessGroup.getId()).contains(userEmail);
    }
  }

  /**
   * Call the VerilyGroups API to list all the groups the application default credentials have
   * access to.
   */
  protected List<VerilyGroup> apiListGroups() {
    JSONObject response =
        makeGetRequestWithADC(basePath + "/v1/groups?group_type=MANAGED_GROUPS", oauthClientId);

    List<VerilyGroup> groups = new ArrayList<>();
    JSONArray groupsArr = response.getJSONArray("groups");
    for (int i = 0; i < groupsArr.length(); i++) {
      JSONObject group = groupsArr.getJSONObject(i);
      VerilyGroup verilyGroup =
          new VerilyGroup(
              group.getString("id"), group.getString("displayName"), group.getString("email"));
      LOGGER.debug(
          "Found Verily Group: {}, {}, {}",
          verilyGroup.getEmail(),
          verilyGroup.getName(),
          verilyGroup.getId());
      groups.add(verilyGroup);
    }
    return groups;
  }

  /**
   * Call the VerilyGroups API to list all the members in a group. Administrator access to a group
   * is required to list its members.
   *
   * @return List of member emails.
   */
  protected List<String> apiListMembers(String groupId) {
    JSONObject response =
        makeGetRequestWithADC(basePath + "/v1/groups/" + groupId + "/members", oauthClientId);

    List<String> members = new ArrayList<>();
    JSONArray membersArr = response.getJSONArray("members");
    for (int i = 0; i < membersArr.length(); i++) {
      JSONObject member = membersArr.getJSONObject(i);
      String email = member.getString("email");
      LOGGER.debug("Found Verily Group member: {}", email);
      members.add(email);
    }
    return members;
  }

  /** Make a GET request to the VerilyGroups API. */
  private static JSONObject makeGetRequestWithADC(String url, String oauthClientId)
      throws SystemException {
    try {
      IdToken idToken = AppDefaultUtils.getIdTokenFromAdc(Collections.emptyList(), oauthClientId);
      URL obj = new URL(url);

      HttpURLConnection con = (HttpURLConnection) obj.openConnection();
      con.setRequestMethod("GET");
      con.setRequestProperty("Authorization", "Bearer " + idToken.getTokenValue());
      con.setRequestProperty("x-xsrf-protected", "1");
      con.setRequestProperty("Accept", "application/json");
      con.setRequestProperty("Content-Type", "application/json");

      int responseCode = con.getResponseCode();
      LOGGER.debug("HTTP GET response code: {}", responseCode);
      if (responseCode != HttpURLConnection.HTTP_OK) {
        throw new SystemException("Error calling VerilyGroups API: " + responseCode);
      }

      StringBuffer response = new StringBuffer();
      try (BufferedReader in =
          new BufferedReader(
              new InputStreamReader(con.getInputStream(), Charset.forName("UTF-8")))) {
        String inputLine;
        while ((inputLine = in.readLine()) != null) {
          response.append(inputLine);
        }
      }
      LOGGER.debug("HTTP GET response body: {}", response);
      return new JSONObject(response.toString());
    } catch (IOException ioEx) {
      throw new SystemException("Error calling VerilyGroups API", ioEx);
    }
  }

  public static class VerilyGroup {
    private final String id;
    private final String name;
    private final String email;

    public VerilyGroup(String id, String name, String email) {
      this.id = id;
      this.name = name;
      this.email = email;
    }

    public String getId() {
      return id;
    }

    public String getName() {
      return name;
    }

    public String getEmail() {
      return email;
    }
  }
}

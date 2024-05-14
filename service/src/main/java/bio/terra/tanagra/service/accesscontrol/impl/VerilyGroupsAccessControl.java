package bio.terra.tanagra.service.accesscontrol.impl;

import bio.terra.tanagra.exception.SystemException;
import bio.terra.tanagra.service.accesscontrol.Permissions;
import bio.terra.tanagra.service.accesscontrol.ResourceCollection;
import bio.terra.tanagra.service.accesscontrol.ResourceId;
import bio.terra.tanagra.service.accesscontrol.ResourceType;
import bio.terra.tanagra.service.accesscontrol.UnderlayAccessControl;
import bio.terra.tanagra.service.authentication.AppDefaultUtils;
import bio.terra.tanagra.service.authentication.InvalidCredentialsException;
import bio.terra.tanagra.service.authentication.UserId;
import com.google.auth.oauth2.IdToken;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VerilyGroupsAccessControl implements UnderlayAccessControl {
  private static final Logger LOGGER = LoggerFactory.getLogger(VerilyGroupsAccessControl.class);
  public static final String ALL_ACCESS = "ALL_ACCESS";

  private String basePath;
  private String oauthClientId;

  private VerilyGroup allAccessGroup;
  private final Map<String, VerilyGroup> underlayToGroup = new HashMap<>();

  @Override
  public String getDescription() {
    return "VerilyGroups underlay-based access control";
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
  public ResourceCollection listUnderlays(UserId user, int offset, int limit) {
    if (hasAllUnderlayAccess(user.getEmail())) {
      // If user is a member in ALL_ACCESS group, then return all underlays.
      return ResourceCollection.allResourcesAllPermissions(ResourceType.UNDERLAY, null);
    } else {
      // Otherwise, check membership in each of the underlay-specific groups.
      Set<ResourceId> underlaysWithAccess =
          underlayToGroup.keySet().stream()
              .filter(underlay -> hasSpecificUnderlayAccess(underlay, user.getEmail()))
              .map(ResourceId::forUnderlay)
              .collect(Collectors.toSet());
      return underlaysWithAccess.isEmpty()
          ? ResourceCollection.empty(ResourceType.UNDERLAY, null)
          : ResourceCollection.resourcesSamePermissions(
                  Permissions.allActions(ResourceType.UNDERLAY), underlaysWithAccess)
              .slice(offset, limit);
    }
  }

  @Override
  public Permissions getUnderlay(UserId user, ResourceId underlay) {
    // There are no partial permissions, users either have all or none.
    return hasAllUnderlayAccess(user.getEmail())
            || hasSpecificUnderlayAccess(underlay.getUnderlay(), user.getEmail())
        ? Permissions.allActions(ResourceType.UNDERLAY)
        : Permissions.empty(ResourceType.UNDERLAY);
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
          new BufferedReader(new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8))) {
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

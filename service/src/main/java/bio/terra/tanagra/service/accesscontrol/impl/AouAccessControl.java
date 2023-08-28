package bio.terra.tanagra.service.accesscontrol.impl;

import bio.terra.tanagra.exception.SystemException;
import bio.terra.tanagra.service.accesscontrol.AccessControl;
import bio.terra.tanagra.service.accesscontrol.Action;
import bio.terra.tanagra.service.accesscontrol.Permissions;
import bio.terra.tanagra.service.accesscontrol.ResourceCollection;
import bio.terra.tanagra.service.accesscontrol.ResourceId;
import bio.terra.tanagra.service.accesscontrol.ResourceType;
import bio.terra.tanagra.service.auth.UserId;
import com.google.common.collect.ImmutableList;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AouAccessControl implements AccessControl {

  private static final Logger LOGGER = LoggerFactory.getLogger(AouAccessControl.class);

  private String basePath;

  @Override
  public void initialize(List<String> params, String basePath, String oauthClientId) {
    if (basePath == null || oauthClientId == null) {
      throw new IllegalArgumentException(
          "Base URL and OAuth client id are required for VUMC admin service API calls");
    }
    this.basePath = basePath;
  }

  @Override
  public String getDescription() {
    return "Check WorkspaceAccessLevel of the user for the workspace in All of Us Research Workbench";
  }

  @Override
  public boolean isAuthorized(UserId user, Permissions permissions, @Nullable ResourceId resource) {
    // AoU has access levels for studyId which is same as AoU workspaceNamespace
    if (permissions.getType().equals(ResourceType.STUDY)
        && permissions.getActions().contains(Action.CREATE)) {
      // AoU is calling Tanagra to create study - do not check authorization
      return true;
    } else {
      try {
        String accessLevel = getWorkspaceAccess(user, resource);
        LOGGER.info(
            "User access level for workspace [{}] is [{}]", resource.getStudy(), accessLevel);
        switch (accessLevel) {
          case "OWNER":
            return true;
          case "WRITER":
            if (permissions.getType().equals(ResourceType.STUDY)
                && (permissions.getActions().contains(Action.UPDATE)
                    || permissions.getActions().contains(Action.DELETE))) {
              return false;
            }
            return true;
          case "READER":
            if (permissions.getActions().contains(Action.QUERY_COUNTS)
                || permissions.getActions().contains(Action.QUERY_INSTANCES)
                || permissions.getActions().contains(Action.READ)) {
              return true;
            }
          default:
            return false;
        }
      } catch (Exception ex) {
        LOGGER.error(
            "Workspace [{}] does not exist or user [{}] does not have permission to use it.",
            resource.getStudy(),
            user.getEmail(),
            ex);
        return false;
      }
    }
  }

  @Override
  public ResourceCollection listAllPermissions(
      UserId user, ResourceType type, @Nullable ResourceId parentResource, int offset, int limit) {
    try {
      String accessLevel = getWorkspaceAccess(user, parentResource);
      switch (accessLevel) {
        case "OWNER":
          return ResourceCollection.allResourcesAllPermissions(type, parentResource);
        case "WRITER":
          if (type.equals(ResourceType.STUDY)) {
            return ResourceCollection.allResourcesSamePermissions(
                Permissions.forActions(
                    type, Action.READ, Action.CREATE_COHORT, Action.CREATE_CONCEPT_SET),
                parentResource);
          }
          return ResourceCollection.allResourcesAllPermissions(type, parentResource);
        case "READER":
          if (ImmutableList.of(ResourceType.UNDERLAY, ResourceType.REVIEW).contains(type)) {
            return ResourceCollection.allResourcesSamePermissions(
                Permissions.forActions(
                    type, Action.READ, Action.QUERY_INSTANCES, Action.QUERY_COUNTS),
                parentResource);
          } else {
            return ResourceCollection.allResourcesSamePermissions(
                Permissions.forActions(type, Action.READ), parentResource);
          }
        default:
          return ResourceCollection.empty(type, parentResource);
      }
    } catch (Exception ex) {
      LOGGER.error(
          "Workspace [{}] does not exist or user [{}] does not have permission to use it.",
          parentResource.getStudy(),
          user.getEmail(),
          ex);
      return ResourceCollection.empty(type, parentResource);
    }
  }

  private String getWorkspaceAccess(UserId user, ResourceId resource) {
    //   "/v1/workspaces/access/{workspaceNamespace}":
    LOGGER.debug("ResourceId getStudy() -> {}", resource.getStudy());
    try {
      URL obj = new URL(basePath + "/" + resource.getStudy());

      HttpURLConnection con = (HttpURLConnection) obj.openConnection();
      con.setRequestMethod("GET");
      con.setRequestProperty("Authorization", "Bearer " + user.getToken());
      con.setRequestProperty("x-xsrf-protected", "1");
      con.setRequestProperty("Accept", "application/json");
      con.setRequestProperty("Content-Type", "application/json");

      LOGGER.debug("HTTP GET response code: {}", con.getResponseCode());
      if (con.getResponseCode() != HttpURLConnection.HTTP_OK) {
        LOGGER.error(
            "Resource {} not found or user {} is unauthorized for resource",
            resource.getType(),
            user.getEmail());
        return "NO ACCESS";
      } else {
        BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()));
        StringBuilder body = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
          body.append(line);
        }
        return body.toString();
      }
    } catch (Exception ex) {
      LOGGER.error(
          "Exception getting authorization for Resource {} for user {}",
          resource.getType(),
          user.getEmail(),
          ex);
      throw new SystemException("Exception getting authorization", ex);
    }
  }
}

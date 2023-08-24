package bio.terra.tanagra.service.accesscontrol.impl;

import bio.terra.tanagra.exception.SystemException;
import bio.terra.tanagra.service.accesscontrol.AccessControl;
import bio.terra.tanagra.service.accesscontrol.Action;
import bio.terra.tanagra.service.accesscontrol.Permissions;
import bio.terra.tanagra.service.accesscontrol.ResourceCollection;
import bio.terra.tanagra.service.accesscontrol.ResourceId;
import bio.terra.tanagra.service.accesscontrol.ResourceType;
import bio.terra.tanagra.service.auth.UserId;
import bio.terra.tanagra.utils.HttpUtils;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.List;
import javax.annotation.Nullable;
import org.apache.http.HttpStatus;
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
      // AoU is calling Tanagra to create study and
      // the call to this method in StudiesV2ApiController.createStudy()
      // does set ResourceId object for authorization call
      return true;
    } else {
      LOGGER.info(
          "user email: "
              + user.getEmail()
              + " subject: "
              + user.getSubject()
              + " token: "
              + user.getToken());
      try {
        String accessLevel = getWorkspaceAccessWithRetry(user, resource);
        LOGGER.info(
            "User access level for workspace [{}] is [{}]", resource.getStudy(), accessLevel);
        switch (accessLevel) {
          case "OWNER":
          case "WRITER":
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
      } catch (Exception e) {
        LOGGER.error(
            "Workspace [{}] does not exist or user [{}] does not have permission to use it.",
            resource.getStudy(),
            user.getEmail());
        return false;
      }
    }
  }

  @Override
  public ResourceCollection listAllPermissions(
      UserId user, ResourceType type, @Nullable ResourceId parentResource, int offset, int limit) {
    // supported for
    // type: ResourceType.COHORT, ResourceType.REVIEW, ResourceType.CONCEPT_SET,
    // ResourceType.ANNOTATION_KEY
    // studyId = parentResource.getStudy
    // TODO
    String em = user.getEmail();
    String accessLevel = getWorkspaceAccessWithRetry(user, parentResource);
    return null;
  }

  private String getWorkspaceAccess(UserId user, ResourceId resource) throws IOException {
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
        throw new AouApiException(con.getResponseCode(), con.getResponseMessage());
      } else {
        BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()));
        StringBuilder body = new StringBuilder();
        String line = null;
        while ((line = br.readLine()) != null) body.append(line);
        return body.toString();
      }
    } catch (Exception ex) {
      throw new IOException(ex);
    }
  }

  private String getWorkspaceAccessWithRetry(UserId user, ResourceId resource) {
    return callWithRetries(
        () -> getWorkspaceAccess(user, resource), "Retrying getting WorkspaceAccess");
  }

  private <T> T callWithRetries(
      HttpUtils.SupplierWithCheckedException<T, IOException> makeRequest, String errorMsg) {
    return handleClientExceptions(
        () ->
            HttpUtils.callWithRetries(
                makeRequest,
                AouAccessControl::isRetryable,
                HttpUtils.DEFAULT_MAXIMUM_RETRIES,
                HttpUtils.DEFAULT_DURATION_SLEEP_FOR_RETRY),
        errorMsg);
  }

  private <T> T handleClientExceptions(
      HttpUtils.SupplierWithCheckedException<T, IOException> makeRequest, String errorMsg) {
    try {
      return makeRequest.makeRequest();
    } catch (IOException | InterruptedException ex) {
      // wrap the exception and re-throw it
      throw new SystemException(errorMsg, ex);
    }
  }

  static boolean isRetryable(Exception ex) {
    if (ex instanceof SocketTimeoutException) {
      return true;
    }
    if (!(ex instanceof AouApiException)) {
      return false;
    }
    LOGGER.error("Caught a AouApiException.", ex);
    int statusCode = ((AouApiException) ex).getStatusCode();

    return statusCode == HttpStatus.SC_NOT_FOUND
        || statusCode == HttpStatus.SC_INTERNAL_SERVER_ERROR;
  }

  private class AouApiException extends Exception {
    private int statusCode;

    public AouApiException(int statusCode, String message) {
      super(message);
      this.statusCode = statusCode;
    }

    public int getStatusCode() {
      return statusCode;
    }
  }
}

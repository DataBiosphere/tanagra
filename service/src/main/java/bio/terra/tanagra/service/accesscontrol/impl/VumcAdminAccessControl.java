package bio.terra.tanagra.service.accesscontrol.impl;

import static bio.terra.tanagra.service.accesscontrol.Action.*;

import bio.terra.common.logging.RequestIdFilter;
import bio.terra.tanagra.app.auth.SpringAuthentication;
import bio.terra.tanagra.exception.SystemException;
import bio.terra.tanagra.service.accesscontrol.AccessControl;
import bio.terra.tanagra.service.accesscontrol.Action;
import bio.terra.tanagra.service.accesscontrol.ResourceId;
import bio.terra.tanagra.service.accesscontrol.ResourceIdCollection;
import bio.terra.tanagra.service.accesscontrol.ResourceType;
import bio.terra.tanagra.service.auth.AppDefaultUtils;
import bio.terra.tanagra.service.auth.UserId;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.ws.rs.client.Client;
import org.apache.http.HttpStatus;
import org.slf4j.MDC;
import org.vumc.vda.tanagra.admin.api.AuthorizationApi;
import org.vumc.vda.tanagra.admin.api.TestApi;
import org.vumc.vda.tanagra.admin.api.UnauthenticatedApi;
import org.vumc.vda.tanagra.admin.client.ApiClient;
import org.vumc.vda.tanagra.admin.client.ApiException;
import org.vumc.vda.tanagra.admin.model.*;

public class VumcAdminAccessControl implements AccessControl {
  private static final String USE_ADC = "USE_ADC";

  private String basePath;
  private String oauthClientId;
  private boolean useAdc;
  private Client commonHttpClient;

  @Override
  public String getDescription() {
    return "Check VUMC admin service for study access";
  }

  @Override
  public void initialize(List<String> params, String basePath, String oauthClientId) {
    if (basePath == null || oauthClientId == null) {
      throw new IllegalArgumentException(
          "Base URL and OAuth client id are required for VUMC admin service API calls");
    }
    this.basePath = basePath;
    this.oauthClientId = oauthClientId;

    // Default is to use application default credentials.
    this.useAdc = params.isEmpty() || USE_ADC.equalsIgnoreCase(params.get(0));
    this.commonHttpClient = new ApiClient().getHttpClient();
  }

  @Override
  public boolean isAuthorized(
      UserId userId, Action action, ResourceType resourceType, @Nullable ResourceId resourceId) {
    if (resourceType == ResourceType.UNDERLAY) {
      // For underlays, check authorization with the underlay id.
      String underlayId = resourceId == null ? null : resourceId.getUnderlay();
      return apiIsAuthorized(
          userId.getEmail(),
          ResourceAction.valueOf(action.toString()),
          org.vumc.vda.tanagra.admin.model.ResourceType.UNDERLAY,
          underlayId);
    } else if (resourceType == ResourceType.STUDY) {
      // For studies, check authorization with the study id.
      String studyId = resourceId == null ? null : resourceId.getStudy();
      return apiIsAuthorized(
          userId.getEmail(),
          ResourceAction.valueOf(action.toString()),
          org.vumc.vda.tanagra.admin.model.ResourceType.STUDY,
          studyId);
    } else {
      // For artifacts that are children of a study (e.g. cohort):
      //   - Map the action type to the action on the study (e.g. create cohort -> update study).
      //   - Check authorization with the parent study id.
      String studyId = resourceId == null ? null : resourceId.getStudy();
      Action actionOnStudy =
          List.of(READ, QUERY_INSTANCES, QUERY_COUNTS).contains(action) ? READ : UPDATE;
      return apiIsAuthorized(
          userId.getEmail(),
          ResourceAction.valueOf(actionOnStudy.toString()),
          org.vumc.vda.tanagra.admin.model.ResourceType.STUDY,
          studyId);
    }
  }

  @Override
  public ResourceIdCollection listResourceIds(
      UserId userId,
      ResourceType resourceType,
      ResourceId parentResourceId,
      int offset,
      int limit) {
    if (resourceType == ResourceType.UNDERLAY) {
      // For underlays, list authorized underlay ids.
      ResourceList resourceList =
          apiListAuthorizedResources(
              userId.getEmail(), org.vumc.vda.tanagra.admin.model.ResourceType.UNDERLAY);
      return ResourceIdCollection.forCollection(
          resourceList.stream()
              .map(resource -> ResourceId.forUnderlay(resource.getId()))
              .collect(Collectors.toList()));
    } else if (resourceType == ResourceType.STUDY) {
      // For studies, list authorized study ids.
      ResourceList resourceList =
          apiListAuthorizedResources(
              userId.getEmail(), org.vumc.vda.tanagra.admin.model.ResourceType.STUDY);
      return ResourceIdCollection.forCollection(
          resourceList.stream()
              .map(resource -> ResourceId.forStudy(resource.getId()))
              .collect(Collectors.toList()));
    } else {
      // For artifacts that are children of a study (e.g. cohort), check if the user is authorized
      // to read the parent study.
      //   - If yes, they can see all children artifacts in it (e.g. cohorts).
      //   - If no, they can see no children artifacts in it.
      if (isAuthorized(userId, READ, ResourceType.STUDY, parentResourceId)) {
        return ResourceIdCollection.allResourceIds();
      } else {
        return ResourceIdCollection.empty();
      }
    }
  }

  @SuppressWarnings("PMD.UseObjectForClearerAPI")
  private boolean apiIsAuthorized(
      String userEmail,
      ResourceAction resourceAction,
      org.vumc.vda.tanagra.admin.model.ResourceType resourceType,
      String resourceId) {
    AuthorizationApi authorizationApi = new AuthorizationApi(getApiClientAuthenticated());
    try {
      authorizationApi.isAuthorized(userEmail, resourceAction, resourceType, resourceId);
      return true;
    } catch (ApiException apiEx) {
      if (apiEx.getCode() == HttpStatus.SC_UNAUTHORIZED) {
        return false;
      }
      throw new SystemException("Error calling VUMC admin service isAuthorized endpoint", apiEx);
    }
  }

  private ResourceList apiListAuthorizedResources(
      String userEmail, org.vumc.vda.tanagra.admin.model.ResourceType resourceType) {
    AuthorizationApi authorizationApi = new AuthorizationApi(getApiClientAuthenticated());
    try {
      return authorizationApi.listAuthorizedResources(userEmail, resourceType);
    } catch (ApiException apiEx) {
      throw new SystemException(
          "Error calling VUMC admin service listAuthorizedResources endpoint", apiEx);
    }
  }

  public SystemVersion apiVersion() throws ApiException {
    // Use an authenticated client here, even though the version endpoint is part of the
    // UnauthenticatedApi in case all endpoints are behind IAP.
    return new UnauthenticatedApi(getApiClientAuthenticated()).serviceVersion();
  }

  public CoreServiceTest apiRoundTripTest() throws ApiException {
    TestApi testApi = new TestApi(getApiClientAuthenticated());
    return testApi.coreServiceTest();
  }

  /**
   * Return an ApiClient with a token from the currently authenticated user or the application
   * default credentials, depending on the configuration flag.
   */
  private ApiClient getApiClientAuthenticated() {
    UserId userId =
        useAdc
            ? AppDefaultUtils.getUserIdFromAdc(oauthClientId)
            : SpringAuthentication.getCurrentUser();
    ApiClient client =
        new ApiClient()
            .setBasePath(basePath)
            .setHttpClient(commonHttpClient)
            .addDefaultHeader(
                RequestIdFilter.REQUEST_ID_HEADER, MDC.get(RequestIdFilter.REQUEST_ID_MDC_KEY));
    client.setAccessToken(userId.getToken());
    return client;
  }
}

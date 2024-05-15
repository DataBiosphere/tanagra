package bio.terra.tanagra.service.accesscontrol.model.impl;

import bio.terra.common.logging.RequestIdFilter;
import bio.terra.tanagra.app.authentication.SpringAuthentication;
import bio.terra.tanagra.exception.SystemException;
import bio.terra.tanagra.service.accesscontrol.AccessControlHelper;
import bio.terra.tanagra.service.accesscontrol.Action;
import bio.terra.tanagra.service.accesscontrol.Permissions;
import bio.terra.tanagra.service.accesscontrol.ResourceCollection;
import bio.terra.tanagra.service.accesscontrol.ResourceId;
import bio.terra.tanagra.service.accesscontrol.ResourceType;
import bio.terra.tanagra.service.accesscontrol.model.StudyAccessControl;
import bio.terra.tanagra.service.authentication.AppDefaultUtils;
import bio.terra.tanagra.service.authentication.UserId;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.ws.rs.client.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.vumc.vda.tanagra.admin.api.AuthorizationApi;
import org.vumc.vda.tanagra.admin.api.TestApi;
import org.vumc.vda.tanagra.admin.api.UnauthenticatedApi;
import org.vumc.vda.tanagra.admin.client.ApiClient;
import org.vumc.vda.tanagra.admin.client.ApiException;
import org.vumc.vda.tanagra.admin.model.CoreServiceTest;
import org.vumc.vda.tanagra.admin.model.ResourceAction;
import org.vumc.vda.tanagra.admin.model.ResourceList;
import org.vumc.vda.tanagra.admin.model.SystemVersion;

public class VumcAdminAccessControl implements StudyAccessControl {
  private static final Logger LOGGER = LoggerFactory.getLogger(VumcAdminAccessControl.class);
  private static final String USE_ADC = "USE_ADC";

  private String basePath;
  private String oauthClientId;
  private boolean useAdc;
  private Client commonHttpClient;

  @Override
  public String getDescription() {
    return "VUMC admin service study-based access control";
  }

  @Override
  public void initialize(
      List<String> params,
      String basePath,
      String oauthClientId,
      AccessControlHelper accessControlHelper) {
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
  public ResourceCollection listUnderlays(UserId user, int offset, int limit) {
    LOGGER.error(
        "VUMC will not call Tanagra to list underlays - this is managed by the admin UI & service.");
    return ResourceCollection.empty(ResourceType.UNDERLAY, null);
  }

  @Override
  public Permissions getUnderlay(UserId user, ResourceId underlay) {
    Map<ResourceId, Permissions> resourcePermissionsMap =
        listAllPermissions(user, ResourceType.UNDERLAY);
    return resourcePermissionsMap.getOrDefault(underlay, Permissions.empty(ResourceType.UNDERLAY));
  }

  @Override
  public Permissions createStudy(UserId user) {
    return apiIsAuthorizedUser(user.getEmail())
        ? Permissions.forActions(ResourceType.STUDY, Set.of(Action.CREATE))
        : Permissions.empty(ResourceType.STUDY);
  }

  @Override
  public ResourceCollection listStudies(UserId user, int offset, int limit) {
    // Get permissions for all studies, then splice the list to accommodate the offset+limit.
    Map<ResourceId, Permissions> resourcePermissionsMap =
        listAllPermissions(user, ResourceType.STUDY);
    return resourcePermissionsMap.isEmpty()
        ? ResourceCollection.empty(ResourceType.STUDY, null)
        : ResourceCollection.resourcesDifferentPermissions(resourcePermissionsMap)
            .slice(offset, limit);
  }

  @Override
  public Permissions getStudy(UserId user, ResourceId study) {
    // Get permissions for all studies, then pull out the one requested here.
    Map<ResourceId, Permissions> resourcePermissionsMap =
        listAllPermissions(user, ResourceType.STUDY);
    return resourcePermissionsMap.getOrDefault(study, Permissions.empty(ResourceType.STUDY));
  }

  @Override
  public Permissions getActivityLog(UserId user) {
    return apiIsAuthorizedUser(user.getEmail())
        ? Permissions.allActions(ResourceType.ACTIVITY_LOG)
        : Permissions.empty(ResourceType.ACTIVITY_LOG);
  }

  private Map<ResourceId, Permissions> listAllPermissions(UserId user, ResourceType type) {
    ResourceList apiResourceList =
        apiListAuthorizedResources(
            user.getEmail(), org.vumc.vda.tanagra.admin.model.ResourceType.valueOf(type.name()));

    Map<ResourceId, Set<ResourceAction>> resourceApiActionsMap = new HashMap<>();
    apiResourceList.forEach(
        apiResource -> {
          // Ignore any API resources returned that don't match the expected resource type.
          if (!org.vumc.vda.tanagra.admin.model.ResourceType.ALL.equals(apiResource.getType())
              && !apiResource.getType().name().equals(type.name())) {
            LOGGER.warn(
                "API resource list includes unexpected resource type: requested {}, returned {}",
                type,
                apiResource.getType());
            return;
          }

          // Convert from the API resource id.
          ResourceId resource;
          if (apiResource.getId() == null) {
            resource = ResourceId.builder().type(type).isNull(true).build();
          } else if (ResourceType.UNDERLAY.equals(type)) {
            resource = ResourceId.forUnderlay(apiResource.getId());
          } else {
            resource = ResourceId.forStudy(apiResource.getId());
          }

          // Add to the list of permitted API actions for this resource id.
          Set<ResourceAction> apiActions =
              resourceApiActionsMap.containsKey(resource)
                  ? resourceApiActionsMap.get(resource)
                  : new HashSet<>();
          apiActions.add(apiResource.getAction());

          resourceApiActionsMap.put(resource, apiActions);
        });

    // For each resource id, convert the list of permitted API actions to a permissions object.
    return resourceApiActionsMap.entrySet().stream()
        .collect(
            Collectors.toMap(
                Map.Entry::getKey,
                entry -> {
                  Set<Action> actions = new HashSet<>();
                  entry
                      .getValue()
                      .forEach(
                          apiAction ->
                              actions.addAll(
                                  ResourceType.UNDERLAY.equals(type)
                                      ? fromUnderlayApiAction(apiAction)
                                      : fromStudyApiAction(apiAction)));
                  return Permissions.forActions(type, actions);
                }));
  }

  /** Admin service underlay permission -> Core service underlay permission. */
  private static Set<Action> fromUnderlayApiAction(ResourceAction apiAction) {
    switch (apiAction) {
      case ALL:
      case READ:
        return ResourceType.UNDERLAY.getActions();
      case UPDATE:
      case CREATE:
      case DELETE:
      default:
        LOGGER.warn("Unknown mapping for underlay API resource action {}", apiAction);
        return Set.of();
    }
  }
  /** Admin service study permission -> Core service study permission. */
  private static Set<Action> fromStudyApiAction(ResourceAction apiAction) {
    switch (apiAction) {
      case ALL:
        return ResourceType.STUDY.getActions();
      case READ:
        return Set.of(Action.READ);
      case UPDATE:
        return Set.of(Action.UPDATE, Action.CREATE_COHORT, Action.CREATE_CONCEPT_SET);
      case CREATE:
        return Set.of(Action.CREATE);
      case DELETE:
        return Set.of(Action.DELETE);
      default:
        LOGGER.warn("Unknown mapping for study API resource action {}", apiAction);
        return Set.of();
    }
  }

  protected ResourceList apiListAuthorizedResources(
      String userEmail, org.vumc.vda.tanagra.admin.model.ResourceType resourceType) {
    AuthorizationApi authorizationApi = new AuthorizationApi(getApiClientAuthenticated());
    try {
      return authorizationApi.listAuthorizedResources(userEmail, resourceType);
    } catch (ApiException apiEx) {
      throw new SystemException(
          "Error calling VUMC admin service listAuthorizedResources endpoint", apiEx);
    }
  }

  protected boolean apiIsAuthorizedUser(String userEmail) {
    AuthorizationApi authorizationApi = new AuthorizationApi(getApiClientAuthenticated());
    try {
      authorizationApi.isAuthorizedUser(userEmail);
      return true;
    } catch (ApiException apiEx) {
      if (apiEx.getCode() == org.apache.http.HttpStatus.SC_UNAUTHORIZED) {
        return false;
      }
      throw new SystemException(
          "Error calling VUMC admin service isAuthorizedUser endpoint", apiEx);
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

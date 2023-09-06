package bio.terra.tanagra.service.accesscontrol.impl;

import static bio.terra.tanagra.service.accesscontrol.Action.*;

import bio.terra.common.logging.RequestIdFilter;
import bio.terra.tanagra.app.auth.SpringAuthentication;
import bio.terra.tanagra.exception.SystemException;
import bio.terra.tanagra.service.accesscontrol.*;
import bio.terra.tanagra.service.accesscontrol.ResourceType;
import bio.terra.tanagra.service.auth.AppDefaultUtils;
import bio.terra.tanagra.service.auth.UserId;
import java.util.*;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.ws.rs.client.Client;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.vumc.vda.tanagra.admin.api.AuthorizationApi;
import org.vumc.vda.tanagra.admin.api.TestApi;
import org.vumc.vda.tanagra.admin.api.UnauthenticatedApi;
import org.vumc.vda.tanagra.admin.client.ApiClient;
import org.vumc.vda.tanagra.admin.client.ApiException;
import org.vumc.vda.tanagra.admin.model.*;

public class VumcAdminAccessControl implements AccessControl {
  private static final Logger LOGGER = LoggerFactory.getLogger(VumcAdminAccessControl.class);
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
  public boolean isAuthorized(UserId user, Permissions permissions, @Nullable ResourceId resource) {
    if (ResourceType.ACTIVITY_LOG.equals(permissions.getType())) {
      // TODO: Call VUMC admin service.
      return true; // Temporarily, all users can see the activity log.
    } else if (ResourceType.UNDERLAY.equals(permissions.getType())) {
      // For underlays, check authorization with the underlay id.
      return isAuthorized(
          user,
          permissions.getActions(),
          ResourceType.UNDERLAY,
          resource == null ? null : resource.getUnderlay());
    } else if (ResourceType.STUDY.equals(permissions.getType())) {
      // For studies, check authorization with the study id.
      return isAuthorized(
          user,
          permissions.getActions(),
          ResourceType.STUDY,
          resource == null ? null : resource.getStudy());
    } else {
      // For artifacts that are children of a study (e.g. cohort):
      //   - Map the action type to the action on the study (e.g. create cohort -> update study).
      //   - Check authorization with the ancestor study id.
      HashSet<Action> actionsOnStudy = new HashSet<>();
      permissions.getActions().stream()
          .forEach(
              action ->
                  actionsOnStudy.add(
                      List.of(READ, QUERY_INSTANCES, QUERY_COUNTS).contains(action)
                          ? READ
                          : UPDATE));
      return isAuthorized(
          user, actionsOnStudy, ResourceType.STUDY, resource == null ? null : resource.getStudy());
    }
  }

  private boolean isAuthorized(UserId user, Set<Action> actions, ResourceType type, String id) {
    // Convert the resource type to its API equivalent.
    org.vumc.vda.tanagra.admin.model.ResourceType apiType =
        org.vumc.vda.tanagra.admin.model.ResourceType.valueOf(type.name());

    // Convert the list of permitted actions to their API equivalents.
    Set<ResourceAction> apiActions = new HashSet<>();
    if (ResourceType.UNDERLAY.equals(type)) {
      actions.stream().forEach(a -> apiActions.addAll(toUnderlayApiAction(a)));
    } else if (ResourceType.STUDY.equals(type)) {
      actions.stream().forEach(a -> apiActions.addAll(toStudyApiAction(a)));
    } else {
      throw new SystemException(
          "VUMC admin service only stores permissions for underlays and studies");
    }

    // Check authorization for each permission separately.
    for (ResourceAction apiAction : apiActions) {
      if (!apiIsAuthorized(user.getEmail(), apiAction, apiType, id)) {
        return false;
      }
    }
    return true;
  }

  private Set<ResourceAction> toUnderlayApiAction(Action action) {
    if (!ResourceType.UNDERLAY.getActions().contains(action)) {
      throw new SystemException("Invalid underlay action: " + action);
    }
    switch (action) {
      case READ:
      case QUERY_INSTANCES:
      case QUERY_COUNTS:
        return Set.of(ResourceAction.READ);
      default:
        LOGGER.debug("Unknown mapping for underlay action {}", action);
        return Set.of();
    }
  }

  private Set<ResourceAction> toStudyApiAction(Action action) {
    if (!ResourceType.STUDY.getActions().contains(action)) {
      throw new SystemException("Invalid study action: " + action);
    }
    switch (action) {
      case READ:
        return Set.of(ResourceAction.READ);
      case CREATE:
        return Set.of(ResourceAction.CREATE);
      case DELETE:
        return Set.of(ResourceAction.DELETE);
      case UPDATE:
      case CREATE_COHORT:
      case CREATE_CONCEPT_SET:
        return Set.of(ResourceAction.UPDATE);
      default:
        LOGGER.debug("Unknown mapping for study action {}", action);
        return Set.of();
    }
  }

  @Override
  public ResourceCollection listAllPermissions(
      UserId user, ResourceType type, @Nullable ResourceId parentResource, int offset, int limit) {
    if (ResourceType.ACTIVITY_LOG.equals(type)) {
      // Users either have all permissions on activity logs, or none.
      return isAuthorized(user, Permissions.forActions(ResourceType.ACTIVITY_LOG, READ), null)
          ? ResourceCollection.allResourcesAllPermissions(ResourceType.ACTIVITY_LOG, null)
          : ResourceCollection.empty(ResourceType.ACTIVITY_LOG, null);
    } else if (ResourceType.UNDERLAY.equals(type) || ResourceType.STUDY.equals(type)) {
      // Admin service stores permissions for underlays and studies.
      Map<ResourceId, Set<ResourceAction>> resourceApiActionsMap = listAllPermissions(user, type);

      // For each resource id, convert the list of permitted API actions to a permissions object.
      Map<ResourceId, Permissions> resourcePermissionsMap =
          resourceApiActionsMap.entrySet().stream()
              .collect(
                  Collectors.toMap(
                      Map.Entry::getKey,
                      entry -> {
                        Set<Action> actions = new HashSet<>();
                        entry.getValue().stream()
                            .forEach(
                                apiAction ->
                                    actions.addAll(
                                        ResourceType.UNDERLAY.equals(type)
                                            ? fromUnderlayApiAction(apiAction)
                                            : fromStudyApiAction(apiAction)));
                        return Permissions.forActions(type, actions);
                      }));

      return resourcePermissionsMap.isEmpty()
          ? ResourceCollection.empty(type, parentResource)
          : ResourceCollection.resourcesDifferentPermissions(resourcePermissionsMap)
              .slice(offset, limit);
    } else {
      // Admin service does not store permissions for any other resource types.
      // All other resource types are descendants of a study (e.g. cohort), so list permissions for
      // studies.
      Map<ResourceId, Set<ResourceAction>> studyApiActionsMap =
          listAllPermissions(user, ResourceType.STUDY);

      // Check that the user has access to the parent study.
      ResourceId studyAncestorResource = ResourceId.forStudy(parentResource.getStudy());
      if (!studyApiActionsMap.containsKey(studyAncestorResource)) {
        return ResourceCollection.empty(type, parentResource);
      }

      // For the parent study only, convert the list of permitted API actions to a permissions
      // object for the descendant resource type.
      Set<ResourceAction> parentStudyApiActions = studyApiActionsMap.get(studyAncestorResource);
      Set<Action> descendantActions = new HashSet<>();
      parentStudyApiActions.stream()
          .forEach(
              apiAction ->
                  descendantActions.addAll(fromStudyApiActionForDescendant(apiAction, type)));
      Permissions descendantPermissions = Permissions.forActions(type, descendantActions);

      return ResourceCollection.allResourcesSamePermissions(descendantPermissions, parentResource)
          .slice(offset, limit);
    }
  }

  private Map<ResourceId, Set<ResourceAction>> listAllPermissions(UserId user, ResourceType type) {
    ResourceList apiResourceList =
        apiListAuthorizedResources(
            user.getEmail(), org.vumc.vda.tanagra.admin.model.ResourceType.valueOf(type.name()));

    Map<ResourceId, Set<ResourceAction>> resourceApiActionsMap = new HashMap<>();
    apiResourceList.stream()
        .forEach(
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
    return resourceApiActionsMap;
  }

  /** Admin service underlay permission -> Core service underlay permission. */
  private Set<Action> fromUnderlayApiAction(ResourceAction apiAction) {
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
  private Set<Action> fromStudyApiAction(ResourceAction apiAction) {
    switch (apiAction) {
      case ALL:
        return ResourceType.STUDY.getActions();
      case READ:
        return Set.of(READ);
      case UPDATE:
        return Set.of(UPDATE, CREATE_COHORT, CREATE_CONCEPT_SET);
      case CREATE:
        return Set.of(CREATE);
      case DELETE:
        return Set.of(DELETE);
      default:
        LOGGER.warn("Unknown mapping for study API resource action {}", apiAction);
        return Set.of();
    }
  }

  /** Admin service study permission -> Core service study descendant (e.g. cohort) permission. */
  private Set<Action> fromStudyApiActionForDescendant(
      ResourceAction apiAction, ResourceType descendantType) {
    switch (apiAction) {
      case ALL:
        return descendantType.getActions();
      case READ:
        return ResourceType.REVIEW.equals(descendantType)
            ? Set.of(READ, QUERY_INSTANCES, QUERY_COUNTS)
            : Set.of(READ);
      case UPDATE:
        Set<Action> allActionsExceptRead = new HashSet<>(descendantType.getActions());
        allActionsExceptRead.remove(READ);
        allActionsExceptRead.remove(QUERY_INSTANCES);
        allActionsExceptRead.remove(QUERY_COUNTS);
        return allActionsExceptRead;
      case CREATE:
      case DELETE:
      default:
        LOGGER.warn(
            "Unknown mapping for study API resource action {} for descendant resource type {}",
            apiAction,
            descendantType);
        return Set.of();
    }
  }

  @SuppressWarnings("PMD.UseObjectForClearerAPI")
  protected boolean apiIsAuthorized(
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

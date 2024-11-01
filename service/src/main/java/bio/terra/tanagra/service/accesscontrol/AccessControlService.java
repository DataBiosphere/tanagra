package bio.terra.tanagra.service.accesscontrol;

import static bio.terra.tanagra.service.accesscontrol.Action.READ;
import static bio.terra.tanagra.service.accesscontrol.ResourceType.COHORT;
import static bio.terra.tanagra.service.accesscontrol.ResourceType.FEATURE_SET;
import static bio.terra.tanagra.service.accesscontrol.ResourceType.STUDY;
import static bio.terra.tanagra.service.accesscontrol.ResourceType.UNDERLAY;

import bio.terra.common.exception.UnauthorizedException;
import bio.terra.tanagra.app.authentication.SpringAuthentication;
import bio.terra.tanagra.app.configuration.AccessControlConfiguration;
import bio.terra.tanagra.exception.SystemException;
import bio.terra.tanagra.service.accesscontrol.model.CoreModel;
import bio.terra.tanagra.service.accesscontrol.model.FineGrainedAccessControl;
import bio.terra.tanagra.service.artifact.StudyService;
import bio.terra.tanagra.service.authentication.UserId;
import com.google.common.annotations.VisibleForTesting;
import jakarta.validation.constraints.NotNull;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AccessControlService {
  private static final Logger LOGGER = LoggerFactory.getLogger(AccessControlService.class);
  private final FineGrainedAccessControl accessControlImpl;

  @Autowired
  @SuppressWarnings("PMD.PreserveStackTrace")
  public AccessControlService(
      AccessControlConfiguration accessControlConfiguration, StudyService studyService) {
    // Try to load the access control model using the CoreModel enum.
    // If that doesn't work, then assume the model is a class name that we can load via reflection.
    FineGrainedAccessControl impl;
    String modelPointer = accessControlConfiguration.getModel();
    try {
      CoreModel coreModel = CoreModel.valueOf(modelPointer);
      impl = coreModel.createNewInstance();
    } catch (IllegalArgumentException iaEx) {
      LOGGER.warn(
          "Access control model not recognized as a CoreModel. Trying to load using classname.",
          iaEx);
      try {
        Class<?> implClass = Class.forName(modelPointer);
        impl = (FineGrainedAccessControl) implClass.getDeclaredConstructor().newInstance();
      } catch (ClassNotFoundException
          | NoSuchMethodException
          | InstantiationException
          | IllegalAccessException
          | InvocationTargetException cnfEx) {
        throw new SystemException("Invalid access control class name: " + modelPointer, cnfEx);
      }
    }

    impl.initialize(
        accessControlConfiguration.getParams(),
        accessControlConfiguration.getBasePath(),
        accessControlConfiguration.getOauthClientId(),
        new AccessControlHelper(studyService));
    this.accessControlImpl = impl;
  }

  @VisibleForTesting
  public AccessControlService(
      FineGrainedAccessControl impl,
      AccessControlConfiguration accessControlConfiguration,
      StudyService studyService) {
    impl.initialize(
        accessControlConfiguration.getParams(),
        accessControlConfiguration.getBasePath(),
        accessControlConfiguration.getOauthClientId(),
        new AccessControlHelper(studyService));
    this.accessControlImpl = impl;
  }

  public void throwIfUnauthorized(UserId user, Permissions permissions) {
    if (!isAuthorized(user, permissions)) {
      throw new UnauthorizedException("User is unauthorized to " + permissions.logString());
    }
  }

  public void throwIfUnauthorized(UserId user, Permissions permissions, ResourceId resource) {
    if (!isAuthorized(user, permissions, resource)) {
      throw new UnauthorizedException(
          "User is unauthorized to "
              + permissions.logString()
              + " "
              + resource.getType()
              + ": "
              + resource.getId());
    }
  }

  public boolean isAuthorized(UserId user, Permissions permissions) {
    // Study.CREATE and ActivityLog.* are the only permissions that don't require a corresponding
    // resource id.
    if (Permissions.forActions(ResourceType.STUDY, Action.CREATE).equals(permissions)) {
      return accessControlImpl.createStudy(user).contains(permissions);
    } else if (ResourceType.ACTIVITY_LOG.equals(permissions.getType())) {
      return accessControlImpl.getActivityLog(user).contains(permissions);
    } else {
      throw new SystemException(
          "Permissions check requires resource id: "
              + permissions.getType()
              + ", "
              + permissions.logString());
    }
  }

  @VisibleForTesting
  public boolean isAuthorized(UserId user, Permissions permissions, ResourceId resource) {
    if (resource == null) {
      return isAuthorized(user, permissions);
    }

    if (!permissions.getType().equals(resource.getType())) {
      throw new SystemException(
          "Permissions and resource types do not match: "
              + permissions.getType()
              + ", "
              + resource.getType());
    }
    Permissions resourcePermissions =
        switch (resource.getType()) {
          case UNDERLAY -> accessControlImpl.getUnderlay(user, resource);
          case STUDY -> accessControlImpl.getStudy(user, resource);
          case COHORT -> accessControlImpl.getCohort(user, resource);
          case FEATURE_SET -> accessControlImpl.getDataFeatureSet(user, resource);
          case REVIEW -> accessControlImpl.getReview(user, resource);
          case ANNOTATION_KEY -> accessControlImpl.getAnnotation(user, resource);
          case ACTIVITY_LOG -> accessControlImpl.getActivityLog(user);
          default -> throw new SystemException("Unsupported resource type: " + resource.getType());
        };
    return resourcePermissions.contains(permissions);
  }

  public ResourceCollection listAuthorizedResources(
      UserId user, Permissions permissions, String userAccessGroup, int offset, int limit) {
    ResourceCollection allResources =
        switch (permissions.getType()) {
          case UNDERLAY -> accessControlImpl.listUnderlays(user, offset, limit);
          case STUDY ->
              userAccessGroup != null
                  ? accessControlImpl.listStudies(user, userAccessGroup, offset, limit)
                  : accessControlImpl.listStudies(user, offset, limit);
          default ->
              throw new SystemException(
                  "Listing " + permissions.getType() + " resources requires a parent resource id");
        };
    return allResources.filter(permissions);
  }

  public ResourceCollection listAuthorizedResources(
      UserId user, Permissions permissions, ResourceId parentResource, int offset, int limit) {
    if (parentResource == null) {
      return listAuthorizedResources(user, permissions, (String) null, offset, limit);
    }

    if (!parentResource.getType().equals(permissions.getType().getParentResourceType())) {
      throw new SystemException(
          "Parent resource type "
              + parentResource.getType()
              + " is unexpected for child resource type "
              + permissions.getType());
    }
    ResourceCollection allResources =
        switch (permissions.getType()) {
          case COHORT -> accessControlImpl.listCohorts(user, parentResource, offset, limit);
          case FEATURE_SET ->
              accessControlImpl.listDataFeatureSets(user, parentResource, offset, limit);
          case REVIEW -> accessControlImpl.listReviews(user, parentResource, offset, limit);
          case ANNOTATION_KEY ->
              accessControlImpl.listAnnotations(user, parentResource, offset, limit);
          default ->
              throw new SystemException(
                  "Listing "
                      + permissions.getType()
                      + " resources does not require a parent resource id");
        };
    return allResources.filter(permissions);
  }

  public void checkUnderlayAccess(@NotNull String underlayName) {
    throwIfUnauthorized(
        SpringAuthentication.getCurrentUser(),
        Permissions.forActions(UNDERLAY, READ),
        ResourceId.forUnderlay(underlayName));
  }

  public void checkReadAccess(
      @NotNull String studyName,
      @NotNull List<String> cohortIds,
      @NotNull List<String> featureSetIds) {
    throwIfUnauthorized(
        SpringAuthentication.getCurrentUser(),
        Permissions.forActions(STUDY, READ),
        ResourceId.forStudy(studyName));

    for (String cohortId : cohortIds) {
      throwIfUnauthorized(
          SpringAuthentication.getCurrentUser(),
          Permissions.forActions(COHORT, READ),
          ResourceId.forCohort(studyName, cohortId));
    }

    for (String featureSetId : featureSetIds) {
      throwIfUnauthorized(
          SpringAuthentication.getCurrentUser(),
          Permissions.forActions(FEATURE_SET, READ),
          ResourceId.forFeatureSet(studyName, featureSetId));
    }
  }
}

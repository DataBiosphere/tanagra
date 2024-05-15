package bio.terra.tanagra.service.accesscontrol;

import bio.terra.common.exception.UnauthorizedException;
import bio.terra.tanagra.app.configuration.AccessControlConfiguration;
import bio.terra.tanagra.exception.SystemException;
import bio.terra.tanagra.service.accesscontrol.model.CoreModel;
import bio.terra.tanagra.service.accesscontrol.model.FineGrainedAccessControl;
import bio.terra.tanagra.service.artifact.StudyService;
import bio.terra.tanagra.service.authentication.UserId;
import com.google.common.annotations.VisibleForTesting;
import java.lang.reflect.InvocationTargetException;
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
    Permissions resourcePermissions;
    switch (resource.getType()) {
      case UNDERLAY:
        resourcePermissions = accessControlImpl.getUnderlay(user, resource);
        break;
      case STUDY:
        resourcePermissions = accessControlImpl.getStudy(user, resource);
        break;
      case COHORT:
        resourcePermissions = accessControlImpl.getCohort(user, resource);
        break;
      case CONCEPT_SET:
        resourcePermissions = accessControlImpl.getDataFeatureSet(user, resource);
        break;
      case REVIEW:
        resourcePermissions = accessControlImpl.getReview(user, resource);
        break;
      case ANNOTATION_KEY:
        resourcePermissions = accessControlImpl.getAnnotation(user, resource);
        break;
      case ACTIVITY_LOG:
        resourcePermissions = accessControlImpl.getActivityLog(user);
        break;
      default:
        throw new SystemException("Unsupported resource type: " + resource.getType());
    }
    return resourcePermissions.contains(permissions);
  }

  public ResourceCollection listAuthorizedResources(
      UserId user, Permissions permissions, int offset, int limit) {
    ResourceCollection allResources;
    switch (permissions.getType()) {
      case UNDERLAY:
        allResources = accessControlImpl.listUnderlays(user, offset, limit);
        break;
      case STUDY:
        allResources = accessControlImpl.listStudies(user, offset, limit);
        break;
      default:
        throw new SystemException(
            "Listing " + permissions.getType() + " resources requires a parent resource id");
    }
    return allResources.filter(permissions);
  }

  public ResourceCollection listAuthorizedResources(
      UserId user, Permissions permissions, ResourceId parentResource, int offset, int limit) {
    if (parentResource == null) {
      return listAuthorizedResources(user, permissions, offset, limit);
    }

    if (!parentResource.getType().equals(permissions.getType().getParentResourceType())) {
      throw new SystemException(
          "Parent resource type "
              + parentResource.getType()
              + " is unexpected for child resource type "
              + permissions.getType());
    }
    ResourceCollection allResources;
    switch (permissions.getType()) {
      case COHORT:
        allResources = accessControlImpl.listCohorts(user, parentResource, offset, limit);
        break;
      case CONCEPT_SET:
        allResources = accessControlImpl.listDataFeatureSets(user, parentResource, offset, limit);
        break;
      case REVIEW:
        allResources = accessControlImpl.listReviews(user, parentResource, offset, limit);
        break;
      case ANNOTATION_KEY:
        allResources = accessControlImpl.listAnnotations(user, parentResource, offset, limit);
        break;
      default:
        throw new SystemException(
            "Listing "
                + permissions.getType()
                + " resources does not require a parent resource id");
    }
    return allResources.filter(permissions);
  }
}

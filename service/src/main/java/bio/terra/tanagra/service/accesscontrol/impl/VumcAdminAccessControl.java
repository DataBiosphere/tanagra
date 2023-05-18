package bio.terra.tanagra.service.accesscontrol.impl;

import static bio.terra.tanagra.service.accesscontrol.Action.*;

import bio.terra.tanagra.service.VumcAdminService;
import bio.terra.tanagra.service.accesscontrol.AccessControl;
import bio.terra.tanagra.service.accesscontrol.Action;
import bio.terra.tanagra.service.accesscontrol.ResourceId;
import bio.terra.tanagra.service.accesscontrol.ResourceIdCollection;
import bio.terra.tanagra.service.accesscontrol.ResourceType;
import bio.terra.tanagra.service.auth.UserId;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.vumc.vda.tanagra.admin.model.ResourceAction;
import org.vumc.vda.tanagra.admin.model.ResourceList;
import org.vumc.vda.tanagra.admin.model.ResourceTypeList;

public class VumcAdminAccessControl implements AccessControl {
  private final VumcAdminService vumcAdminService;

  public VumcAdminAccessControl(VumcAdminService vumcAdminService) {
    this.vumcAdminService = vumcAdminService;
  }

  @Override
  public String getDescription() {
    return "Forward the access control checks to the VUMC admin service";
  }

  @Override
  public boolean isAuthorized(
      UserId userId, Action action, ResourceType resourceType, @Nullable ResourceId resourceId) {
    if (resourceType == ResourceType.UNDERLAY) {
      // For underlays, check authorization with the underlay id.
      String underlayId = resourceId == null ? null : resourceId.getUnderlay();
      return vumcAdminService.isAuthorized(
          userId.getEmail(),
          ResourceAction.valueOf(action.toString()),
          org.vumc.vda.tanagra.admin.model.ResourceType.UNDERLAY,
          underlayId);
    } else if (resourceType == ResourceType.STUDY) {
      // For studies, check authorization with the study id.
      String studyId = resourceId == null ? null : resourceId.getStudy();
      return vumcAdminService.isAuthorized(
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
      return vumcAdminService.isAuthorized(
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
    ResourceTypeList resourceTypeList = new ResourceTypeList();
    if (resourceType == ResourceType.UNDERLAY) {
      // For underlays, list authorized underlay ids.
      resourceTypeList.add(org.vumc.vda.tanagra.admin.model.ResourceType.UNDERLAY);
      ResourceList resourceList =
          vumcAdminService.listAuthorizedResources(userId.getEmail(), resourceTypeList);
      return ResourceIdCollection.forCollection(
          resourceList.stream()
              .map(resource -> ResourceId.forUnderlay(resource.getId()))
              .collect(Collectors.toList()));
    } else if (resourceType == ResourceType.STUDY) {
      // For studies, list authorized study ids.
      resourceTypeList.add(org.vumc.vda.tanagra.admin.model.ResourceType.STUDY);
      ResourceList resourceList =
          vumcAdminService.listAuthorizedResources(userId.getEmail(), resourceTypeList);
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
}

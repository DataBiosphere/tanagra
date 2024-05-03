package bio.terra.tanagra.app.controller;

import static bio.terra.tanagra.service.accesscontrol.Action.CREATE;
import static bio.terra.tanagra.service.accesscontrol.Action.DELETE;
import static bio.terra.tanagra.service.accesscontrol.Action.READ;
import static bio.terra.tanagra.service.accesscontrol.Action.UPDATE;
import static bio.terra.tanagra.service.accesscontrol.ResourceType.STUDY;

import bio.terra.common.exception.BadRequestException;
import bio.terra.tanagra.app.authentication.SpringAuthentication;
import bio.terra.tanagra.app.controller.objmapping.ToApiUtils;
import bio.terra.tanagra.generated.controller.StudiesApi;
import bio.terra.tanagra.generated.model.ApiPropertyKeyValue;
import bio.terra.tanagra.generated.model.ApiStudy;
import bio.terra.tanagra.generated.model.ApiStudyCreateInfo;
import bio.terra.tanagra.generated.model.ApiStudyList;
import bio.terra.tanagra.generated.model.ApiStudyUpdateInfo;
import bio.terra.tanagra.service.accesscontrol.AccessControlService;
import bio.terra.tanagra.service.accesscontrol.Permissions;
import bio.terra.tanagra.service.accesscontrol.ResourceCollection;
import bio.terra.tanagra.service.accesscontrol.ResourceId;
import bio.terra.tanagra.service.artifact.StudyService;
import bio.terra.tanagra.service.artifact.model.Study;
import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

@Controller
public class StudiesApiController implements StudiesApi {
  private final StudyService studyService;
  private final AccessControlService accessControlService;

  @Autowired
  public StudiesApiController(
      StudyService studyService, AccessControlService accessControlService) {
    this.studyService = studyService;
    this.accessControlService = accessControlService;
  }

  @Override
  public ResponseEntity<ApiStudy> createStudy(ApiStudyCreateInfo body) {
    accessControlService.throwIfUnauthorized(
        SpringAuthentication.getCurrentUser(), Permissions.forActions(STUDY, CREATE));
    Study.Builder studyToCreate =
        Study.builder()
            .id(body.getId())
            .displayName(body.getDisplayName())
            .description(body.getDescription())
            .properties(fromApiObject(body.getProperties()));
    return ResponseEntity.ok(
        ToApiUtils.toApiObject(
            studyService.createStudy(
                studyToCreate, SpringAuthentication.getCurrentUser().getEmail())));
  }

  @Override
  public ResponseEntity<Void> deleteStudy(String studyId) {
    accessControlService.throwIfUnauthorized(
        SpringAuthentication.getCurrentUser(),
        Permissions.forActions(STUDY, DELETE),
        ResourceId.forStudy(studyId));
    studyService.deleteStudy(studyId, SpringAuthentication.getCurrentUser().getEmail());
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }

  @Override
  public ResponseEntity<ApiStudy> getStudy(String studyId) {
    accessControlService.throwIfUnauthorized(
        SpringAuthentication.getCurrentUser(),
        Permissions.forActions(STUDY, READ),
        ResourceId.forStudy(studyId));
    return ResponseEntity.ok(ToApiUtils.toApiObject(studyService.getStudy(studyId)));
  }

  @Override
  public ResponseEntity<ApiStudyList> listStudies(
      String displayName,
      String description,
      String createdBy,
      Boolean includeDeleted,
      List<String> properties,
      Integer offset,
      Integer limit) {
    ResourceCollection authorizedStudyIds =
        accessControlService.listAuthorizedResources(
            SpringAuthentication.getCurrentUser(),
            Permissions.forActions(STUDY, READ),
            offset,
            limit);
    List<Study> authorizedStudies =
        studyService.listStudies(
            authorizedStudyIds,
            offset,
            limit,
            includeDeleted != null && includeDeleted,
            fromApiObject(displayName, description, createdBy, properties));
    ApiStudyList apiStudies = new ApiStudyList();
    authorizedStudies.forEach(study -> apiStudies.add(ToApiUtils.toApiObject(study)));
    return ResponseEntity.ok(apiStudies);
  }

  @Override
  public ResponseEntity<ApiStudy> updateStudy(String studyId, ApiStudyUpdateInfo body) {
    accessControlService.throwIfUnauthorized(
        SpringAuthentication.getCurrentUser(),
        Permissions.forActions(STUDY, UPDATE),
        ResourceId.forStudy(studyId));
    Study updatedStudy =
        studyService.updateStudy(
            studyId,
            SpringAuthentication.getCurrentUser().getEmail(),
            body.getDisplayName(),
            body.getDescription());
    return ResponseEntity.ok(ToApiUtils.toApiObject(updatedStudy));
  }

  @Override
  public ResponseEntity<Void> updateStudyProperties(
      String studyId, List<ApiPropertyKeyValue> body) {
    accessControlService.throwIfUnauthorized(
        SpringAuthentication.getCurrentUser(),
        Permissions.forActions(STUDY, UPDATE),
        ResourceId.forStudy(studyId));
    studyService.updateStudyProperties(
        studyId, SpringAuthentication.getCurrentUser().getEmail(), fromApiObject(body));
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }

  @Override
  public ResponseEntity<Void> deleteStudyProperties(String studyId, List<String> body) {
    accessControlService.throwIfUnauthorized(
        SpringAuthentication.getCurrentUser(),
        Permissions.forActions(STUDY, UPDATE),
        ResourceId.forStudy(studyId));
    studyService.deleteStudyProperties(
        studyId, SpringAuthentication.getCurrentUser().getEmail(), body);
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }

  private static ImmutableMap<String, String> fromApiObject(
      List<ApiPropertyKeyValue> apiProperties) {
    Map<String, String> propertyMap = new HashMap<>();
    if (apiProperties != null) {
      for (ApiPropertyKeyValue apiProperty : apiProperties) {
        propertyMap.put(apiProperty.getKey(), apiProperty.getValue());
      }
    }
    return ImmutableMap.copyOf(propertyMap);
  }

  private static Study.Builder fromApiObject(
      String displayName, String description, String createdBy, List<String> properties) {
    // Convert the list of properties (key1, value1, key2, value2,...) to a map.
    // The API uses a list parameter here because map query parameters aren't handled by the Java
    // codegen.
    Map<String, String> propertiesMap;
    if (properties == null) {
      propertiesMap = null;
    } else {
      propertiesMap = new HashMap<>();
      for (int i = 0; i < properties.size(); i += 2) {
        if (i == properties.size() - 1) {
          throw new BadRequestException(
              "Study filter properties map must have an even number of elements: key1, value1, key2, value2.");
        }
        propertiesMap.put(properties.get(i), properties.get(i + 1));
      }
    }
    return Study.builder()
        .displayName(displayName)
        .description(description)
        .createdBy(createdBy)
        .properties(propertiesMap);
  }
}

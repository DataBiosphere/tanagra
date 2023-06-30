package bio.terra.tanagra.app.controller;

import static bio.terra.tanagra.service.accesscontrol.Action.CREATE;
import static bio.terra.tanagra.service.accesscontrol.Action.DELETE;
import static bio.terra.tanagra.service.accesscontrol.Action.READ;
import static bio.terra.tanagra.service.accesscontrol.Action.UPDATE;
import static bio.terra.tanagra.service.accesscontrol.ResourceType.STUDY;

import bio.terra.common.exception.BadRequestException;
import bio.terra.tanagra.app.auth.SpringAuthentication;
import bio.terra.tanagra.generated.controller.StudiesV2Api;
import bio.terra.tanagra.generated.model.*;
import bio.terra.tanagra.service.AccessControlService;
import bio.terra.tanagra.service.StudyService;
import bio.terra.tanagra.service.accesscontrol.Permissions;
import bio.terra.tanagra.service.accesscontrol.ResourceCollection;
import bio.terra.tanagra.service.accesscontrol.ResourceId;
import bio.terra.tanagra.service.artifact.Study;
import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

@Controller
public class StudiesV2ApiController implements StudiesV2Api {
  private final StudyService studyService;
  private final AccessControlService accessControlService;

  @Autowired
  public StudiesV2ApiController(
      StudyService studyService, AccessControlService accessControlService) {
    this.studyService = studyService;
    this.accessControlService = accessControlService;
  }

  @Override
  public ResponseEntity<ApiStudyV2> createStudy(ApiStudyCreateInfoV2 body) {
    accessControlService.throwIfUnauthorized(
        SpringAuthentication.getCurrentUser(), Permissions.forActions(STUDY, CREATE));
    Study.Builder studyToCreate =
        Study.builder()
            .displayName(body.getDisplayName())
            .description(body.getDescription())
            .properties(fromApiObject(body.getProperties()));
    return ResponseEntity.ok(
        toApiObject(
            studyService.createStudy(
                studyToCreate, SpringAuthentication.getCurrentUser().getEmail())));
  }

  @Override
  public ResponseEntity<Void> deleteStudy(String studyId) {
    accessControlService.throwIfUnauthorized(
        SpringAuthentication.getCurrentUser(),
        Permissions.forActions(STUDY, DELETE),
        ResourceId.forStudy(studyId));
    studyService.deleteStudy(studyId);
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }

  @Override
  public ResponseEntity<ApiStudyV2> getStudy(String studyId) {
    accessControlService.throwIfUnauthorized(
        SpringAuthentication.getCurrentUser(),
        Permissions.forActions(STUDY, READ),
        ResourceId.forStudy(studyId));
    return ResponseEntity.ok(toApiObject(studyService.getStudy(studyId)));
  }

  @Override
  public ResponseEntity<ApiStudyListV2> listStudies(
      String displayName,
      String description,
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
            authorizedStudyIds, offset, limit, fromApiObject(displayName, description, properties));
    ApiStudyListV2 apiStudies = new ApiStudyListV2();
    authorizedStudies.stream().forEach(study -> apiStudies.add(toApiObject(study)));
    return ResponseEntity.ok(apiStudies);
  }

  @Override
  public ResponseEntity<ApiStudyV2> updateStudy(String studyId, ApiStudyUpdateInfoV2 body) {
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
    return ResponseEntity.ok(toApiObject(updatedStudy));
  }

  @Override
  public ResponseEntity<Void> updateStudyProperties(
      String studyId, List<ApiPropertyKeyValueV2> body) {
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

  private static ApiStudyV2 toApiObject(Study study) {
    ApiPropertiesV2 apiProperties = new ApiPropertiesV2();
    study
        .getProperties()
        .forEach(
            (key, value) -> apiProperties.add(new ApiPropertyKeyValueV2().key(key).value(value)));
    return new ApiStudyV2()
        .id(study.getId())
        .displayName(study.getDisplayName())
        .description(study.getDescription())
        .properties(apiProperties)
        .created(study.getCreated())
        .createdBy(study.getCreatedBy())
        .lastModified(study.getLastModified());
  }

  private static ImmutableMap<String, String> fromApiObject(
      List<ApiPropertyKeyValueV2> apiProperties) {
    Map<String, String> propertyMap = new HashMap<>();
    if (apiProperties != null) {
      for (ApiPropertyKeyValueV2 apiProperty : apiProperties) {
        propertyMap.put(apiProperty.getKey(), apiProperty.getValue());
      }
    }
    return ImmutableMap.copyOf(propertyMap);
  }

  private static Study.Builder fromApiObject(
      String displayName, String description, List<String> properties) {
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
        .properties(propertiesMap);
  }
}

package bio.terra.tanagra.app.controller;

import static bio.terra.tanagra.service.accesscontrol.Action.CREATE;
import static bio.terra.tanagra.service.accesscontrol.Action.DELETE;
import static bio.terra.tanagra.service.accesscontrol.Action.READ;
import static bio.terra.tanagra.service.accesscontrol.Action.UPDATE;
import static bio.terra.tanagra.service.accesscontrol.ResourceType.STUDY;

import bio.terra.tanagra.app.auth.SpringAuthentication;
import bio.terra.tanagra.generated.controller.StudiesV2Api;
import bio.terra.tanagra.generated.model.ApiPropertiesV2;
import bio.terra.tanagra.generated.model.ApiPropertyKeyValueV2;
import bio.terra.tanagra.generated.model.ApiStudyCreateInfoV2;
import bio.terra.tanagra.generated.model.ApiStudyListV2;
import bio.terra.tanagra.generated.model.ApiStudyUpdateInfoV2;
import bio.terra.tanagra.generated.model.ApiStudyV2;
import bio.terra.tanagra.service.AccessControlService;
import bio.terra.tanagra.service.StudyService;
import bio.terra.tanagra.service.accesscontrol.ResourceId;
import bio.terra.tanagra.service.accesscontrol.ResourceIdCollection;
import bio.terra.tanagra.service.model.Study;
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
    accessControlService.throwIfUnauthorized(SpringAuthentication.getCurrentUser(), CREATE, STUDY);
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
        SpringAuthentication.getCurrentUser(), DELETE, STUDY, new ResourceId(studyId));
    studyService.deleteStudy(studyId);
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }

  @Override
  public ResponseEntity<ApiStudyV2> getStudy(String studyId) {
    accessControlService.throwIfUnauthorized(
        SpringAuthentication.getCurrentUser(), READ, STUDY, new ResourceId(studyId));
    return ResponseEntity.ok(toApiObject(studyService.getStudy(studyId)));
  }

  @Override
  public ResponseEntity<ApiStudyListV2> listStudies(Integer offset, Integer limit) {
    ResourceIdCollection authorizedStudyIds =
        accessControlService.listResourceIds(
            SpringAuthentication.getCurrentUser(), STUDY, offset, limit);
    List<Study> authorizedStudies = studyService.listStudies(authorizedStudyIds, offset, limit);
    ApiStudyListV2 apiStudies = new ApiStudyListV2();
    authorizedStudies.stream().forEach(study -> apiStudies.add(toApiObject(study)));
    return ResponseEntity.ok(apiStudies);
  }

  @Override
  public ResponseEntity<ApiStudyV2> updateStudy(String studyId, ApiStudyUpdateInfoV2 body) {
    accessControlService.throwIfUnauthorized(
        SpringAuthentication.getCurrentUser(), UPDATE, STUDY, new ResourceId(studyId));
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
        SpringAuthentication.getCurrentUser(), UPDATE, STUDY, new ResourceId(studyId));
    studyService.updateStudyProperties(
        studyId, SpringAuthentication.getCurrentUser().getEmail(), fromApiObject(body));
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }

  @Override
  public ResponseEntity<Void> deleteStudyProperties(String studyId, List<String> body) {
    accessControlService.throwIfUnauthorized(
        SpringAuthentication.getCurrentUser(), UPDATE, STUDY, new ResourceId(studyId));
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
}

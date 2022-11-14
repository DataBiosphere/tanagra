package bio.terra.tanagra.app.controller;

import static bio.terra.tanagra.api.accesscontrol.Action.CREATE;
import static bio.terra.tanagra.api.accesscontrol.Action.DELETE;
import static bio.terra.tanagra.api.accesscontrol.Action.READ;
import static bio.terra.tanagra.api.accesscontrol.Action.UPDATE;
import static bio.terra.tanagra.api.accesscontrol.ResourceType.STUDY;

import bio.terra.tanagra.api.AccessControlService;
import bio.terra.tanagra.api.accesscontrol.ResourceId;
import bio.terra.tanagra.api.accesscontrol.ResourceIdCollection;
import bio.terra.tanagra.artifact.Study;
import bio.terra.tanagra.artifact.StudyService;
import bio.terra.tanagra.generated.controller.StudiesV2Api;
import bio.terra.tanagra.generated.model.ApiPropertiesV2;
import bio.terra.tanagra.generated.model.ApiPropertiesV2Inner;
import bio.terra.tanagra.generated.model.ApiStudyCreateInfoV2;
import bio.terra.tanagra.generated.model.ApiStudyListV2;
import bio.terra.tanagra.generated.model.ApiStudyUpdateInfoV2;
import bio.terra.tanagra.generated.model.ApiStudyV2;
import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.lang3.RandomStringUtils;
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
    accessControlService.throwIfUnauthorized(null, CREATE, STUDY);

    // Generate a random 10-character alphanumeric string for the new study ID.
    String newStudyId = RandomStringUtils.randomAlphanumeric(10);

    Study studyToCreate =
        Study.builder()
            .studyId(newStudyId)
            .displayName(body.getDisplayName())
            .description(body.getDescription())
            .properties(fromApiObject(body.getProperties()))
            .build();
    studyService.createStudy(studyToCreate);
    return ResponseEntity.ok(toApiObject(studyToCreate));
  }

  @Override
  public ResponseEntity<Void> deleteStudy(String studyId) {
    accessControlService.throwIfUnauthorized(null, DELETE, STUDY, new ResourceId(studyId));
    studyService.deleteStudy(studyId);
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }

  @Override
  public ResponseEntity<ApiStudyV2> getStudy(String studyId) {
    accessControlService.throwIfUnauthorized(null, READ, STUDY, new ResourceId(studyId));
    return ResponseEntity.ok(toApiObject(studyService.getStudy(studyId)));
  }

  @Override
  public ResponseEntity<ApiStudyListV2> listStudies(Integer offset, Integer limit) {
    ResourceIdCollection authorizedStudyNames =
        accessControlService.listResourceIds(STUDY, offset, limit);
    List<Study> authorizedStudies;
    if (authorizedStudyNames.isAllResourceIds()) {
      authorizedStudies = studyService.getAllStudies(offset, limit);
    } else {
      authorizedStudies =
          studyService.getStudies(
              authorizedStudyNames.getResourceIds().stream()
                  .map(ResourceId::getId)
                  .collect(Collectors.toList()),
              offset,
              limit);
    }

    ApiStudyListV2 apiStudies = new ApiStudyListV2();
    authorizedStudies.stream().forEach(study -> apiStudies.add(toApiObject(study)));
    return ResponseEntity.ok(apiStudies);
  }

  @Override
  public ResponseEntity<ApiStudyV2> updateStudy(String studyId, ApiStudyUpdateInfoV2 body) {
    accessControlService.throwIfUnauthorized(null, UPDATE, STUDY, new ResourceId(studyId));
    Study updatedStudy =
        studyService.updateStudy(studyId, body.getDisplayName(), body.getDescription());
    return ResponseEntity.ok(toApiObject(updatedStudy));
  }

  @Override
  public ResponseEntity<Void> updateStudyProperties(
      String studyId, List<ApiPropertiesV2Inner> body) {
    accessControlService.throwIfUnauthorized(null, UPDATE, STUDY, new ResourceId(studyId));
    studyService.updateStudyProperties(studyId, fromApiObject(body));
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }

  @Override
  public ResponseEntity<Void> deleteStudyProperties(String studyId, List<String> body) {
    accessControlService.throwIfUnauthorized(null, UPDATE, STUDY, new ResourceId(studyId));
    studyService.deleteStudyProperties(studyId, body);
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }

  private static ApiStudyV2 toApiObject(Study study) {
    ApiPropertiesV2 apiProperties = new ApiPropertiesV2();
    study
        .getProperties()
        .forEach(
            (key, value) -> apiProperties.add(new ApiPropertiesV2Inner().key(key).value(value)));
    return new ApiStudyV2()
        .id(study.getStudyId())
        .displayName(study.getDisplayName())
        .description(study.getDescription())
        .properties(apiProperties);
  }

  private static ImmutableMap<String, String> fromApiObject(
      List<ApiPropertiesV2Inner> apiProperties) {
    Map<String, String> propertyMap = new HashMap<>();
    if (apiProperties != null) {
      for (ApiPropertiesV2Inner apiProperty : apiProperties) {
        propertyMap.put(apiProperty.getKey(), apiProperty.getValue());
      }
    }
    return ImmutableMap.copyOf(propertyMap);
  }
}

package bio.terra.tanagra.app.controller;

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
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

@Controller
public class StudiesV2ApiController implements StudiesV2Api {
  private final StudyService studyService;

  @Autowired
  public StudiesV2ApiController(StudyService studyService) {
    this.studyService = studyService;
  }

  @Override
  public ResponseEntity<ApiStudyV2> createStudy(ApiStudyCreateInfoV2 body) {
    // Generate a random UUID for the new study ID.
    UUID newStudyUuid = UUID.randomUUID();

    Study studyToCreate =
        Study.builder()
            .studyId(newStudyUuid)
            .displayName(body.getDisplayName())
            .description(body.getDescription())
            .properties(fromApiObject(body.getProperties()))
            .build();
    studyService.createStudy(studyToCreate);
    return ResponseEntity.ok(toApiObject(studyToCreate));
  }

  @Override
  public ResponseEntity<Void> deleteStudy(UUID studyId) {
    studyService.deleteStudy(studyId);
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }

  @Override
  public ResponseEntity<ApiStudyV2> getStudy(UUID studyId) {
    return ResponseEntity.ok(toApiObject(studyService.getStudy(studyId)));
  }

  @Override
  public ResponseEntity<ApiStudyListV2> listStudies(Integer offset, Integer limit) {
    ApiStudyListV2 apiStudies = new ApiStudyListV2();
    studyService.getAllStudies(offset, limit).forEach(study -> apiStudies.add(toApiObject(study)));
    return ResponseEntity.ok(apiStudies);
  }

  @Override
  public ResponseEntity<ApiStudyV2> updateStudy(UUID studyId, ApiStudyUpdateInfoV2 body) {
    Study updatedStudy =
        studyService.updateStudy(studyId, body.getDisplayName(), body.getDescription());
    return ResponseEntity.ok(toApiObject(updatedStudy));
  }

  @Override
  public ResponseEntity<Void> updateStudyProperties(UUID studyId, List<ApiPropertiesV2Inner> body) {
    studyService.updateStudyProperties(studyId, fromApiObject(body));
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }

  @Override
  public ResponseEntity<Void> deleteStudyProperties(UUID studyId, List<String> body) {
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

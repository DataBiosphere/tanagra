package bio.terra.tanagra.service.accesscontrol;

import bio.terra.tanagra.service.artifact.StudyService;
import bio.terra.tanagra.service.artifact.model.Study;
import java.util.List;
import java.util.stream.Collectors;

public class AccessControlHelper {
  private final StudyService studyService;

  public AccessControlHelper(StudyService studyService) {
    this.studyService = studyService;
  }

  public String getStudyUser(String studyId) {
    return studyService.getStudy(studyId).getCreatedBy();
  }

  public List<String> listStudiesForUser(String userEmail) {
    return studyService
        .listStudies(
            ResourceCollection.allResourcesAllPermissions(ResourceType.STUDY, null),
            0,
            Integer.MAX_VALUE,
            false,
            Study.builder().createdBy(userEmail))
        .stream()
        .map(Study::getId)
        .collect(Collectors.toList());
  }
}
